package com.example.lifeapp.ui.transit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeapp.data.local.entity.TransitBookmarkEntity
import com.example.lifeapp.data.model.OperatorCompany
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop
import com.example.lifeapp.data.model.TransitType
import com.example.lifeapp.data.repository.BusRepository
import com.example.lifeapp.ui.common.AutoRefreshDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TransitTab { SEARCH, BOOKMARK }

data class TrackedVehicleInfo(
    val stopId: String,
    val stopSequence: Int,
    val etaTimestamp: String?
)

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
    val bookmarkEtaMap: Map<String, List<TransitEta>> = emptyMap(),
    val bookmarkedStopIds: Set<String> = emptySet(),
    
    val trackedVehicle: TrackedVehicleInfo? = null
)

@HiltViewModel
class TransitSearchViewModel @Inject constructor(
    private val busRepository: BusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransitUiState())
    val uiState: StateFlow<TransitUiState> = _uiState.asStateFlow()

    private val autoRefreshDelegate = AutoRefreshDelegate(
        scope = viewModelScope,
        intervalMillis = 30000L,
        onRefresh = { refreshCurrentEtasImmediately() }
    )

    init {
        loadAllRoutes()
        observeBookmarks()
    }

    fun onResumeRefresh() {
        refreshCurrentEtasImmediately()
        autoRefreshDelegate.start()
    }

    fun onPauseStopRefresh() {
        autoRefreshDelegate.stop()
    }

    fun refreshCurrentEtasImmediately() {
        val state = _uiState.value
        if (state.selectedRoute != null) {
            state.routeStops.forEach { stop ->
                fetchStopEta(stop.stopId)
            }
        } else if (state.currentTab == TransitTab.BOOKMARK) {
            state.bookmarks.forEach { bookmark ->
                fetchBookmarkEta(bookmark)
            }
        }
    }

    private fun loadAllRoutes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRoutes = true) }
            try {
                val routes = busRepository.getRoutes()
                _uiState.update { 
                    it.copy(
                        allRoutes = routes,
                        isLoadingRoutes = false
                    )
                }
                updateFilteredRoutes(_uiState.value.searchQuery)
            } catch (_: Exception) {
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
                bookmarkList.forEach { fetchBookmarkEta(it) }
            }
        }
    }

    fun selectTab(tab: TransitTab) {
        _uiState.update { it.copy(currentTab = tab) }
        refreshCurrentEtasImmediately()
        if (tab == TransitTab.BOOKMARK) {
            autoRefreshDelegate.start()
        }
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

        val nextChars = filtered.mapNotNull { route ->
            val name = route.routeName.uppercase()
            val index = name.indexOf(query, ignoreCase = true)
            if (index != -1 && index + query.length < name.length) {
                name[index + query.length]
            } else null
        }.distinct().sorted()

        val defaultLetters = if (query.isEmpty()) {
            all.flatMap { route ->
                route.routeName.uppercase().filter { it.isLetter() }.toList()
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
                trackedVehicle = null
            )
        }

        viewModelScope.launch {
            try {
                val stops = busRepository.getRouteStops(
                    company = route.company,
                    route = route.routeName,
                    bound = route.bound ?: "O",
                    serviceType = route.serviceType ?: "1"
                )

                _uiState.update { currentState ->
                    currentState.copy(
                        routeStops = stops,
                        isLoadingStops = false
                    )
                }

                stops.forEach { fetchStopEta(it.stopId) }
                autoRefreshDelegate.start()
            } catch (_: Exception) {
                _uiState.update { currentState -> currentState.copy(isLoadingStops = false) }
            }
        }
    }

    fun selectBookmarkRoute(bookmark: TransitBookmarkEntity) {
        val operatorCompany = try {
            OperatorCompany.valueOf(bookmark.company)
        } catch (_: Exception) {
            OperatorCompany.KMB
        }

        val targetRoute = _uiState.value.allRoutes.find { route ->
            route.routeName == bookmark.routeName &&
                    route.company.name == bookmark.company &&
                    route.bound == bookmark.bound &&
                    route.serviceType == bookmark.serviceType
        } ?: TransitRoute(
            routeId = "${operatorCompany.name}_${bookmark.routeName}_${bookmark.bound ?: "O"}_${bookmark.serviceType ?: "1"}",
            routeName = bookmark.routeName,
            transitType = TransitType.BUS,
            company = operatorCompany,
            bound = bookmark.bound,
            serviceType = bookmark.serviceType,
            originZh = bookmark.originZh,
            originEn = null,
            destinationZh = bookmark.destinationZh,
            destinationEn = null
        )

        selectRoute(targetRoute)
    }

    fun clearSelectedRoute() {
        _uiState.update { currentState ->
            currentState.copy(
                selectedRoute = null,
                routeStops = emptyList(),
                selectedStopEtaMap = emptyMap(),
                trackedVehicle = null
            )
        }
        refreshCurrentEtasImmediately()
        autoRefreshDelegate.start()
    }

    fun toggleTrackVehicle(stopId: String, stopSequence: Int, etaTimestamp: String?) {
        _uiState.update { currentState ->
            val currentTracked = currentState.trackedVehicle
            if (currentTracked?.stopId == stopId && currentTracked.etaTimestamp == etaTimestamp) {
                currentState.copy(trackedVehicle = null)
            } else {
                currentState.copy(
                    trackedVehicle = TrackedVehicleInfo(
                        stopId = stopId,
                        stopSequence = stopSequence,
                        etaTimestamp = etaTimestamp
                    )
                )
            }
        }
    }

    fun fetchStopEta(stopId: String) {
        val route = _uiState.value.selectedRoute ?: return
        viewModelScope.launch {
            try {
                val etaList = busRepository.getEta(
                    company = route.company,
                    stopId = stopId,
                    route = route.routeName,
                    serviceType = route.serviceType ?: "1",
                    bound = route.bound ?: "O"
                )

                _uiState.update { currentState ->
                    val currentMap = currentState.selectedStopEtaMap.toMutableMap()
                    currentMap[stopId] = etaList
                    currentState.copy(selectedStopEtaMap = currentMap)
                }
            } catch (_: Exception) {}
        }
    }

    private fun fetchBookmarkEta(bookmark: TransitBookmarkEntity) {
        viewModelScope.launch {
            try {
                val operatorCompany = try {
                    OperatorCompany.valueOf(bookmark.company)
                } catch (_: Exception) {
                    OperatorCompany.KMB
                }

                val etaList = busRepository.getEta(
                    company = operatorCompany,
                    stopId = bookmark.stopId,
                    route = bookmark.routeName,
                    serviceType = bookmark.serviceType ?: "1",
                    bound = bookmark.bound ?: "O"
                )

                _uiState.update { currentState ->
                    val currentMap = currentState.bookmarkEtaMap.toMutableMap()
                    currentMap[bookmark.bookmarkId] = etaList
                    currentState.copy(bookmarkEtaMap = currentMap)
                }
            } catch (_: Exception) {}
        }
    }

    fun toggleBookmark(stop: TransitStop) {
        val route = _uiState.value.selectedRoute ?: return
        val bookmarkId = TransitBookmarkEntity.generateBookmarkId(
            company = route.company.name,
            routeName = route.routeName,
            bound = route.bound ?: "O",
            serviceType = route.serviceType ?: "1",
            stopId = stop.stopId
        )

        viewModelScope.launch {
            if (_uiState.value.bookmarkedStopIds.contains(bookmarkId)) {
                busRepository.removeBookmark(bookmarkId)
            } else {
                val entity = TransitBookmarkEntity(
                    bookmarkId = bookmarkId,
                    routeName = route.routeName,
                    company = route.company.name,
                    bound = route.bound ?: "O",
                    serviceType = route.serviceType ?: "1",
                    originZh = route.originZh ?: "",
                    destinationZh = route.destinationZh ?: "",
                    stopId = stop.stopId,
                    stopNameZh = stop.nameZh ?: "",
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
        autoRefreshDelegate.stop()
    }
}
