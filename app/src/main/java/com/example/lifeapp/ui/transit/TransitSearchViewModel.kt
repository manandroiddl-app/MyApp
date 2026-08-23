app/src/main/java/com/example/lifeapp/ui/transit/TransitSearchViewModel.kt
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    val availableChips: List<Char> = emptyList(), // 動態 Chip 鍵盤可用字元
    val allRoutes: List<TransitRoute> = emptyList(),
    val filteredRoutes: List<TransitRoute> = emptyList(),
    val bookmarks: List<TransitBookmarkEntity> = emptyList(),
    val isLoadingRoutes: Boolean = false,
    
    // Level 2 子頁面狀態
    val selectedRoute: TransitRoute? = null,
    val routeStops: List<TransitStop> = emptyList(),
    val isLoadingStops: Boolean = false,
    val selectedStopEtaMap: Map<String, List<TransitEta>> = emptyMap(), // key: stopId
    val bookmarkedStopIds: Set<String> = emptySet()
)

@HiltViewModel
class TransitSearchViewModel @Inject constructor(
    private val busRepository: BusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransitUiState())
    val uiState: StateFlow<TransitUiState> = _uiState.asStateFlow()

    init {
        loadAllRoutes()
        observeBookmarks()
    }

    /**
     * 靜默 Resume 刷新（配合 OnLifecycleResume / Unlock）
     */
    fun onResumeRefresh() {
        if (_uiState.value.selectedRoute != null) {
            // 如果在 Level 2 站頁，更新車站 ETA
            refreshCurrentStopsEta()
        }
    }

    private fun loadAllRoutes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingRoutes = true)
            val routes = busRepository.getKmbRoutes()
            val sortedRoutes = routes.distinctBy { it.routeName }.sortedBy { it.routeName }
            
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

        // 計算下一個可用的 Chip 字元 (例: 輸入 "2" 後，找出所有開頭為 "2" 的路線第 2 個字元)
        val nextChars = all.mapNotNull { route ->
            val name = route.routeName.uppercase()
            if (name.startsWith(query, ignoreCase = true) && name.length > query.length) {
                name[query.length]
            } else null
        }.distinct().sorted()

        // 如果全新搜尋，提供 0-9 與常見開頭字元
        val finalChips = if (query.isEmpty()) {
            listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'A', 'B', 'C', 'E', 'K', 'N', 'P', 'R', 'S', 'T', 'W', 'X')
        } else {
            nextChars
        }

        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredRoutes = filtered,
            availableChips = finalChips
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
            // 自動預載首三個站的 ETA
            stops.take(5).forEach { stop ->
                fetchStopEta(stop.stopId)
            }
        }
    }

    fun clearSelectedRoute() {
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
}
