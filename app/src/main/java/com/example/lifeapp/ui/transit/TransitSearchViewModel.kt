package com.example.lifeapp.ui.transit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeapp.data.local.entity.TransitBookmarkEntity
import com.example.lifeapp.data.model.OperatorCompany
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop
import com.example.lifeapp.data.repository.BusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TransitTab { SEARCH, BOOKMARK }

data class TransitUiState(
    val currentTab: TransitTab = TransitTab.SEARCH,
    val searchQuery: String = "",
    val numericChips: List<Char> = emptyList(),
    val letterChips: List<Char> = emptyList(),
    val allRoutes: List<TransitRoute> = emptyList(),
    val filteredRoutes: List<TransitRoute> = emptyList(),
    val bookmarks: List<TransitBookmarkEntity> = emptyList(),
    val isLoadingRoutes: Boolean = false,
    
    val selectedRoute: TransitRoute? = null,
    val routeStops: List<TransitStop> = emptyList(),
    val isLoadingStops: Boolean = false,
    val selectedStopEtaMap: Map<String, List<TransitEta>> = emptyMap(),
    val bookmarkedStopIds: Set<String> = emptySet(),
    val expandedStopIds: Set<String> = emptySet()
)

@HiltViewModel
class TransitSearchViewModel @Inject constructor(
    private val busRepository: BusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransitUiState())
    val uiState: StateFlow<TransitUiState> = _uiState.asStateFlow()

    private var etaAutoRefreshJob: Job? = null

    init {
        loadAllRoutes()
        observeBookmarks()
    }

    fun onResumeRefresh() {
        startEtaAutoRefreshLoop()
    }

    fun onPauseStopRefresh() {
        stopEtaAutoRefreshLoop()
    }

    private fun loadAllRoutes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRoutes = true) }
            try {
                val routes = busRepository.getKmbRoutes()
                _uiState.update { 
                    it.copy(
                        allRoutes = routes,
                        isLoadingRoutes = false
                    )
                }
                updateFilteredRoutes(_uiState.value.searchQuery)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingRoutes = false) }
            }
        }
    }

    private fun observeBookmarks() {
        viewModelScope.launch {
            busRepository.getAllBookmarks().collectLatest { bookmarkList ->
                val bookmarkedIds = bookmarkList.map { it.bookmarkId }.toSet()
                _uiState.update { currentState ->
                    currentState.copy(
                        bookmarks = bookmarkList,
                        bookmarkedStopIds = bookmarkedIds
                    )
                }
            }
        }
    }

    fun selectTab(tab: TransitTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun onChipClicked(char: Char) {
        onSearchQueryChanged(_uiState.value.searchQuery + char)
    }

    fun onBackspaceClicked() {
        val current = _uiState.value.searchQuery
        if (current.isNotEmpty()) {
            onSearchQueryChanged(current.substring(0, current.length - 1))
        }
    }

    fun onSearchQueryChanged(query: String) {
        val upperQuery = query.uppercase()
        updateFilteredRoutes(upperQuery)
    }

    private fun updateFilteredRoutes(query: String) {
        val all = _uiState.value.allRoutes
        
        val filtered = if (query.isEmpty()) {
            all
        } else {
            all.filter { it.routeName.contains(query, ignoreCase = true) }
        }

        val nextChars = all.mapNotNull { route ->
            val name = route.routeName.uppercase()
            val index = name.indexOf(query, ignoreCase = true)
            if (index != -1 && index + query.length < name.length) {
                name[index + query.length]
            } else null
        }.distinct().sorted()

        // 【問題 2 修正】當 query 為空時，動態提取所有現有路線中的英文字母（如 F, K, X 等），不再依賴固定 hardcode
        val defaultLetters = if (query.isEmpty()) {
            all.mapNotNull { route ->
                Regex("[A-Za-z]+$").find(route.routeName)?.value?.uppercase()?.firstOrNull()
            }.distinct().sorted()
        } else {
            nextChars.filter { it.isLetter() }
        }

        val defaultNums = if (query.isEmpty()) {
            listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '0')
        } else {
            nextChars.filter { it.isDigit() }
        }

        _uiState.update { currentState ->
            currentState.copy(
                searchQuery = query,
                filteredRoutes = filtered,
                numericChips = defaultNums,
                letterChips = defaultLetters
            )
        }
    }

    fun selectRoute(route: TransitRoute) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedRoute = route,
                isLoadingStops = true,
                routeStops = emptyList(),
                selectedStopEtaMap = emptyMap(),
                expandedStopIds = emptySet()
            )
        }

        viewModelScope.launch {
            try {
                val stops = busRepository.getKmbRouteStops(
                    route = route.routeName,
                    bound = route.bound ?: "O",
                    serviceType = route.serviceType ?: "1"
                )

                val allStopIds = stops.map { it.stopId }.toSet()

                _uiState.update { currentState ->
                    currentState.copy(
                        routeStops = stops,
                        isLoadingStops = false,
                        expandedStopIds = allStopIds
                    )
                }

                allStopIds.forEach { fetchStopEta(it) }
                startEtaAutoRefreshLoop()
            } catch (e: Exception) {
                _uiState.update { currentState -> currentState.copy(isLoadingStops = false) }
            }
        }
    }

    // 【問題 3 補充】點擊收藏項目時直接跳轉回對應路線詳情頁
    fun selectBookmarkRoute(bookmark: TransitBookmarkEntity) {
        val operatorCompany = try {
            OperatorCompany.valueOf(bookmark.company)
        } catch (_: Exception) {
            OperatorCompany.KMB
        }

        val targetRoute = _uiState.value.allRoutes.find { route ->
            route.routeName == bookmark.routeName &&
                    route.bound == bookmark.bound &&
                    route.serviceType == bookmark.serviceType
        } ?: TransitRoute(
            company = operatorCompany,
            routeName = bookmark.routeName,
            bound = bookmark.bound,
            serviceType = bookmark.serviceType,
            originZh = bookmark.originZh,
            destinationZh = bookmark.destinationZh
        )

        selectRoute(targetRoute)

        // 自動展開該收藏的車站
        _uiState.update { currentState ->
            currentState.copy(expandedStopIds = setOf(bookmark.stopId))
        }
    }

    fun clearSelectedRoute() {
        stopEtaAutoRefreshLoop()
        _uiState.update { currentState ->
            currentState.copy(
                selectedRoute = null,
                routeStops = emptyList(),
                selectedStopEtaMap = emptyMap(),
                expandedStopIds = emptySet()
            )
        }
    }

    fun toggleStopExpand(stopId: String) {
        val currentExpanded = _uiState.value.expandedStopIds.toMutableSet()
        if (currentExpanded.contains(stopId)) {
            currentExpanded.remove(stopId)
        } else {
            currentExpanded.add(stopId)
            fetchStopEta(stopId)
        }
        _uiState.update { currentState -> currentState.copy(expandedStopIds = currentExpanded) }
    }

    fun fetchStopEta(stopId: String) {
        val route = _uiState.value.selectedRoute ?: return
        viewModelScope.launch {
            try {
                val etaList = busRepository.getKmbEta(
                    stopId = stopId,
                    route = route.routeName,
                    serviceType = route.serviceType ?: "1"
                )

                _uiState.update { currentState ->
                    val currentMap = currentState.selectedStopEtaMap.toMutableMap()
                    currentMap[stopId] = etaList
                    currentState.copy(selectedStopEtaMap = currentMap)
                }
            } catch (_: Exception) {}
        }
    }

    private fun startEtaAutoRefreshLoop() {
        stopEtaAutoRefreshLoop()
        etaAutoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                if (_uiState.value.selectedRoute != null) {
                    _uiState.value.expandedStopIds.forEach { stopId ->
                        fetchStopEta(stopId)
                    }
                }
            }
        }
    }

    private fun stopEtaAutoRefreshLoop() {
        etaAutoRefreshJob?.cancel()
        etaAutoRefreshJob = null
    }

    fun toggleBookmark(stop: TransitStop) {
        val route = _uiState.value.selectedRoute ?: return
        val bookmarkId = "KMB_${route.routeName}_${route.bound}_${route.serviceType}_${stop.stopId}"

        viewModelScope.launch {
            if (_uiState.value.bookmarkedStopIds.contains(bookmarkId)) {
                busRepository.removeBookmark(bookmarkId)
            } else {
                val entity = TransitBookmarkEntity(
                    bookmarkId = bookmarkId,
                    routeName = route.routeName,
                    company = OperatorCompany.KMB.name,
                    bound = route.bound ?: "O",
                    serviceType = route.serviceType ?: "1",
                    originZh = route.originZh,
                    destinationZh = route.destinationZh,
                    stopId = stop.stopId,
                    stopNameZh = stop.nameZh,
                    sequence = stop.sequence
                )
                busRepository.addBookmark(entity)
            }
        }
    }

    fun removeBookmark(bookmarkId: String) {
        viewModelScope.launch {
            busRepository.removeBookmark(bookmarkId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopEtaAutoRefreshLoop()
    }
}
