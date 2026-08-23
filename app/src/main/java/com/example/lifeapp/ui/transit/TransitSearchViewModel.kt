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

    private val _currentTimeMs = MutableStateFlow(System.currentTimeMillis())
    val currentTimeMs: StateFlow<Long> = _currentTimeMs.asStateFlow()

    private var etaAutoRefreshJob: Job? = null
    private var tickerJob: Job? = null

    init {
        loadAllRoutes()
        observeBookmarks()
        startTicker()
    }

    fun onResumeRefresh() {
        startEtaAutoRefreshLoop()
    }

    fun onPauseStopRefresh() {
        stopEtaAutoRefreshLoop()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _currentTimeMs.value = System.currentTimeMillis()
            }
        }
    }

    private fun loadAllRoutes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingRoutes = true)
            val routes = busRepository.getKmbRoutes()
            _uiState.value = _uiState.value.copy(
                allRoutes = routes,
                isLoadingRoutes = false
            )
            updateFilteredRoutes(_uiState.value.searchQuery)
        }
    }

    private fun observeBookmarks() {
        viewModelScope.launch {
            busRepository.getAllBookmarks().collectLatest { bookmarkList ->
                val bookmarkedIds = bookmarkList.map { it.bookmarkId }.toSet()
                _uiState.value = _uiState.value.copy(
                    bookmarks = bookmarkList,
                    bookmarkedStopIds = bookmarkedIds
                )
            }
        }
    }

    fun selectTab(tab: TransitTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
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

    fun onClearSearch() {
        onSearchQueryChanged("")
    }

    fun onSearchQueryChanged(query: String) {
        val upperQuery = query.uppercase()
        updateFilteredRoutes(upperQuery)
    }

    private fun updateFilteredRoutes(query: String) {
        val all = _uiState.value.allRoutes
        
        // 🎯 修復 3b：移除 .distinctBy { it.routeName }，完整保留雙向 (Bound O/I) 路線
        val filtered = if (query.isEmpty()) {
            all
        } else {
            all.filter { it.routeName.startsWith(query, ignoreCase = true) }
        }

        val nextChars = all.mapNotNull { route ->
            val name = route.routeName.uppercase()
            if (name.startsWith(query, ignoreCase = true) && name.length > query.length) name[query.length] else null
        }.distinct().sorted()

        val (nums, letters) = if (query.isEmpty()) {
            val defaultNums = listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '0')
            val defaultLetters = all.mapNotNull { it.routeName.firstOrNull { c -> c.isLetter() }?.uppercaseChar() }.distinct().sorted()
            Pair(defaultNums, defaultLetters)
        } else {
            Pair(nextChars.filter { it.isDigit() }, nextChars.filter { it.isLetter() })
        }

        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredRoutes = filtered,
            numericChips = nums,
            letterChips = letters
        )
    }

    fun selectRoute(route: TransitRoute) {
        _uiState.value = _uiState.value.copy(
            selectedRoute = route,
            isLoadingStops = true,
            routeStops = emptyList(),
            selectedStopEtaMap = emptyMap(),
            expandedStopIds = emptySet()
        )

        viewModelScope.launch {
            val stops = busRepository.getKmbRouteStops(
                route = route.routeName,
                bound = route.bound ?: "O",
                serviceType = route.serviceType ?: "1"
            )

            // 🎯 修復 2：全部車站預設 100% 展開
            val allStopIds = stops.map { it.stopId }.toSet()

            _uiState.value = _uiState.value.copy(
                routeStops = stops,
                isLoadingStops = false,
                expandedStopIds = allStopIds
            )

            // 一次過發起所有車站 ETA 的 Fetch
            allStopIds.forEach { fetchStopEta(it) }

            startEtaAutoRefreshLoop()
        }
    }

    fun clearSelectedRoute() {
        stopEtaAutoRefreshLoop()
        _uiState.value = _uiState.value.copy(
            selectedRoute = null,
            routeStops = emptyList(),
            selectedStopEtaMap = emptyMap(),
            expandedStopIds = emptySet()
        )
    }

    fun toggleStopExpand(stopId: String) {
        val currentExpanded = _uiState.value.expandedStopIds.toMutableSet()
        if (currentExpanded.contains(stopId)) {
            currentExpanded.remove(stopId)
        } else {
            currentExpanded.add(stopId)
            fetchStopEta(stopId)
        }
        _uiState.value = _uiState.value.copy(expandedStopIds = currentExpanded)
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
                val currentMap = _uiState.value.selectedStopEtaMap.toMutableMap()
                currentMap[stopId] = etaList.filter { (it.minutesLeft ?: 0) >= 0 }
                _uiState.value = _uiState.value.copy(selectedStopEtaMap = currentMap)
            } catch (_: Exception) {}
        }
    }

    private fun startEtaAutoRefreshLoop() {
        stopEtaAutoRefreshLoop()
        etaAutoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000L) // 30 秒自動靜默刷新
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

    override fun onCleared() {
        super.onCleared()
        stopEtaAutoRefreshLoop()
        tickerJob?.cancel()
    }
}
