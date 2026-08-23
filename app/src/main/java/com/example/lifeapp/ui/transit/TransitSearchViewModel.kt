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
    val expandedStopIds: Set<String> = emptySet() // 記錄已展開的車站，針對性刷新
)

@HiltViewModel
class TransitSearchViewModel @Inject constructor(
    private val busRepository: BusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransitUiState())
    val uiState: StateFlow<TransitUiState> = _uiState.asStateFlow()

    // 1秒 UI 刷新 Tick (驅動實時倒數)
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
        val filtered = if (query.isEmpty()) {
            all.distinctBy { it.routeName }
        } else {
            all.filter { it.routeName.startsWith(query, ignoreCase = true) }.distinctBy { it.routeName }
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
            _uiState.value = _uiState.value.copy(
                routeStops = stops,
                isLoadingStops = false
            )
            
            // 預設展開首 3 個站並獲取 ETA
            val initialExpandIds = stops.take(3).map { it.stopId }.toSet()
            _uiState.value = _uiState.value.copy(expandedStopIds = initialExpandIds)
            initialExpandIds.forEach { fetchStopEta(it) }

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
            fetchStopEta(stopId) // 展開時立即載入
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
                // 不論回傳幾班車（1-3班），完整保留並過濾掉已開出的舊班次
                currentMap[stopId] = etaList.filter { (it.minutesLeft ?: 0) >= 0 }
                _uiState.value = _uiState.value.copy(selectedStopEtaMap = currentMap)
            } catch (e: Exception) {
                // 錯誤處理留白，可加入 Logger
            }
        }
    }

    private fun startEtaAutoRefreshLoop() {
        stopEtaAutoRefreshLoop()
        etaAutoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000L) // 每 30 秒靜默刷新
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
