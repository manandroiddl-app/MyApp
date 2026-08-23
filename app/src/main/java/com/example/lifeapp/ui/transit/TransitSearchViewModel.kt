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

// UI 頂層狀態 (搜尋 vs Bookmark)
enum class TransitTab {
    SEARCH, BOOKMARK
}

// 畫面 UI 狀態結構
data class TransitUiState(
    val currentTab: TransitTab = TransitTab.SEARCH,
    val searchQuery: String = "",
    val numericChips: List<Char> = emptyList(), // 第一行：數字 Chip 鍵盤
    val letterChips: List<Char> = emptyList(),  // 第二行：字母 Chip 鍵盤
    val allRoutes: List<TransitRoute> = emptyList(),
    val filteredRoutes: List<TransitRoute> = emptyList(),
    val bookmarks: List<TransitBookmarkEntity> = emptyList(),
    val isLoadingRoutes: Boolean = false,
    
    // Level 2 子頁面狀態
    val selectedRoute: TransitRoute? = null,
    val routeStops: List<TransitStop> = emptyList(),
    val isLoadingStops: Boolean = false,
    val selectedStopEtaMap: Map<String, List<TransitEta>> = emptyMap(), // key: stopId -> value: 全部 ETA 班次
    val bookmarkedStopIds: Set<String> = emptySet()
)

@HiltViewModel
class TransitSearchViewModel @Inject constructor(
    private val busRepository: BusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransitUiState())
    val uiState: StateFlow<TransitUiState> = _uiState.asStateFlow()

    // 🎯 30 秒自動刷新 ETA 的 Coroutine Job
    private var etaAutoRefreshJob: Job? = null

    init {
        loadAllRoutes()
        observeBookmarks()
    }

    /**
     * 靜默 Resume 刷新（配合 OnLifecycleResume / Unlock）
     */
    fun onResumeRefresh() {
        if (_uiState.value.selectedRoute != null) {
            refreshCurrentStopsEta()
            startEtaAutoRefreshLoop()
        }
    }

    /**
     * 當 App 進入背景或離開頁面時停止 30 秒 Timer
     */
    fun onPauseStopRefresh() {
        stopEtaAutoRefreshLoop()
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

    /**
     * 處理 Chip 鍵盤輸入與過濾
     */
    fun onChipClicked(char: Char) {
        val newQuery = _uiState.value.searchQuery + char
        onSearchQueryChanged(newQuery)
    }

    fun onBackspaceClicked() {
        val current = _uiState.value.searchQuery
        if (current.isNotEmpty()) {
            val newQuery = current.substring(0, current.length - 1)
            onSearchQueryChanged(newQuery)
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
            all.filter { it.routeName.startsWith(query, ignoreCase = true) }
                .distinctBy { it.routeName }
        }

        // 計算下一個可用的 Chip 字元
        val nextChars = all.mapNotNull { route ->
            val name = route.routeName.uppercase()
            if (name.startsWith(query, ignoreCase = true) && name.length > query.length) {
                name[query.length]
            } else null
        }.distinct().sorted()

        val (nums, letters) = if (query.isEmpty()) {
            // 空白時：預設 0-9 與 所有出現過的英文字母字頭
            val defaultNums = listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '0')
            val defaultLetters = all.mapNotNull { route ->
                route.routeName.firstOrNull { it.isLetter() }?.uppercaseChar()
            }.distinct().sorted()
            
            Pair(defaultNums, defaultLetters)
        } else {
            // 輸入中：把下一個可能的字元拆分成數字與字母
            Pair(nextChars.filter { it.isDigit() }, nextChars.filter { it.isLetter() })
        }

        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredRoutes = filtered,
            numericChips = nums,
            letterChips = letters
        )
    }

    // ==========================================
    // Level 2 路線詳情 & 車站 ETA
    // ==========================================

    fun selectRoute(route: TransitRoute) {
        _uiState.value = _uiState.value.copy(
            selectedRoute = route,
            isLoadingStops = true,
            routeStops = emptyList(),
            selectedStopEtaMap = emptyMap()
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
            // 自動預載首 5 個站的 ETA
            stops.take(5).forEach { stop ->
                fetchStopEta(stop.stopId)
            }

            // 🎯 啟動 30 秒自動刷新迴圈
            startEtaAutoRefreshLoop()
        }
    }

    fun clearSelectedRoute() {
        stopEtaAutoRefreshLoop()
        _uiState.value = _uiState.value.copy(
            selectedRoute = null,
            routeStops = emptyList(),
            selectedStopEtaMap = emptyMap()
        )
    }

    fun fetchStopEta(stopId: String) {
        val route = _uiState.value.selectedRoute ?: return
        viewModelScope.launch {
            val etaList = busRepository.getKmbEta(
                stopId = stopId,
                route = route.routeName,
                serviceType = route.serviceType ?: "1"
            )
            val currentMap = _uiState.value.selectedStopEtaMap.toMutableMap()
            currentMap[stopId] = etaList
            _uiState.value = _uiState.value.copy(selectedStopEtaMap = currentMap)
        }
    }

    private fun refreshCurrentStopsEta() {
        val route = _uiState.value.selectedRoute ?: return
        val currentStops = _uiState.value.routeStops
        viewModelScope.launch {
            currentStops.forEach { stop ->
                if (_uiState.value.selectedStopEtaMap.containsKey(stop.stopId)) {
                    fetchStopEta(stop.stopId)
                }
            }
        }
    }

    // 🎯 每 30 秒自動靜默刷新 Timer
    private fun startEtaAutoRefreshLoop() {
        stopEtaAutoRefreshLoop()
        etaAutoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000L) // 30 秒
                if (_uiState.value.selectedRoute != null) {
                    refreshCurrentStopsEta()
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
    }
}
