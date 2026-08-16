package com.example.lifeapp.ui.bus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeapp.data.model.*
import com.example.lifeapp.data.repository.KmbRepository
import com.example.lifeapp.ui.common.AutoRefreshDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BusViewModel @Inject constructor(
    private val repository: KmbRepository
) : ViewModel() {

    private val _searchUiState = MutableStateFlow(BusSearchUiState())
    val searchUiState: StateFlow<BusSearchUiState> = _searchUiState.asStateFlow()

    private val _bookmarkUiState = MutableStateFlow(BusBookmarkUiState())
    val bookmarkUiState: StateFlow<BusBookmarkUiState> = _bookmarkUiState.asStateFlow()

    private val _routeStopsEtaMap = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val routeStopsEtaMap: StateFlow<Map<String, List<String>>> = _routeStopsEtaMap.asStateFlow()

    private val _routeFareMap = MutableStateFlow<Map<Int, String>>(emptyMap())
    val routeFareMap: StateFlow<Map<Int, String>> = _routeFareMap.asStateFlow()

    private val _nextAvailableChars = MutableStateFlow<List<String>>(emptyList())
    val nextAvailableChars: StateFlow<List<String>> = _nextAvailableChars.asStateFlow()

    // 🛡️ 引入 Common 輪詢元件（預設 60 秒）
    private val autoRefreshDelegate = AutoRefreshDelegate(viewModelScope) {
        refreshBookmarkEtas()
    }

    init {
        loadAllRoutes()
        observeBookmarks()
        startAutoRefresh()
    }

    fun startAutoRefresh() {
        autoRefreshDelegate.start()
    }

    fun stopAutoRefresh() {
        autoRefreshDelegate.stop()
    }

    private fun loadAllRoutes() {
        viewModelScope.launch {
            _searchUiState.update { it.copy(isLoading = true) }
            val routes = repository.fetchAllRoutes()
            _searchUiState.update {
                it.copy(
                    isLoading = false,
                    routeList = routes,
                    filteredRoutes = routes.take(50)
                )
            }
            updateNextAvailableChars("", routes)
        }
    }

    fun onSearchQueryChange(query: String) {
        val trimmed = query.trim().uppercase()
        _searchUiState.update { state ->
            val filtered = if (trimmed.isEmpty()) {
                state.routeList.take(50)
            } else {
                if (state.searchType == BusSearchType.BY_ROUTE) {
                    state.routeList.filter { it.route.uppercase().startsWith(trimmed) }
                } else {
                    state.routeList.filter { it.origTc.contains(trimmed) || it.destTc.contains(trimmed) }
                }
            }
            updateNextAvailableChars(trimmed, state.routeList)
            state.copy(searchQuery = query, filteredRoutes = filtered)
        }
    }

    fun appendSearchChar(charStr: String) {
        val newQuery = _searchUiState.value.searchQuery + charStr
        onSearchQueryChange(newQuery)
    }

    private fun updateNextAvailableChars(prefix: String, allRoutes: List<KmbRoute>) {
        val matchedRoutes = if (prefix.isEmpty()) {
            allRoutes
        } else {
            allRoutes.filter { it.route.uppercase().startsWith(prefix) }
        }

        val chars = matchedRoutes
            .mapNotNull { r ->
                val name = r.route.uppercase()
                if (name.length > prefix.length) name[prefix.length].toString() else null
            }
            .distinct()
            .sortedWith(comparator = compareBy({ it.first().isLetter() }, { it }))

        _nextAvailableChars.value = chars
    }

    fun selectRoute(route: KmbRoute) {
        viewModelScope.launch {
            _searchUiState.update { it.copy(isLoading = true, selectedRoute = route) }
            _routeStopsEtaMap.value = emptyMap()
            _routeFareMap.value = emptyMap()

            val stops = repository.fetchRouteStopsWithDetail(route.route, route.bound, route.serviceType)
            _searchUiState.update { it.copy(isLoading = false, stopList = stops) }

            fetchRouteFares(route)
            fetchRouteStopsEtas(route, stops)
        }
    }

    private fun fetchRouteFares(route: KmbRoute) {
        viewModelScope.launch {
            val fares = repository.fetchRouteFares(route.route, route.bound, route.serviceType)
            _routeFareMap.value = fares
        }
    }

    private fun fetchRouteStopsEtas(route: KmbRoute, stops: List<Pair<KmbRouteStop, KmbStopDetail>>) {
        viewModelScope.launch {
            val etaMapResult = mutableMapOf<String, List<String>>()
            val deferreds = stops.map { (_, detail) ->
                async {
                    val etas = repository.fetchEtaForStop(detail.stopId, route.route, route.serviceType)
                    detail.stopId to etas
                }
            }
            val results = deferreds.awaitAll()
            results.forEach { (stopId, etas) ->
                if (etas.isNotEmpty()) {
                    etaMapResult[stopId] = etas
                }
            }
            _routeStopsEtaMap.value = etaMapResult
        }
    }

    fun clearSelectedRoute() {
        _searchUiState.update { it.copy(selectedRoute = null, stopList = emptyList()) }
        _routeStopsEtaMap.value = emptyMap()
        _routeFareMap.value = emptyMap()
    }

    fun toggleBookmark(route: KmbRoute, stopDetail: KmbStopDetail) {
        viewModelScope.launch {
            val bookmarkId = "${route.route}_${stopDetail.stopId}_${route.bound}"
            val entity = BusBookmarkEntity(
                id = bookmarkId,
                route = route.route,
                bound = route.bound,
                serviceType = route.serviceType,
                stopId = stopDetail.stopId,
                stopNameTc = stopDetail.nameTc,
                destTc = route.destTc
            )
            repository.toggleBookmark(entity)
            refreshBookmarkEtas()
        }
    }

    private fun observeBookmarks() {
        viewModelScope.launch {
            repository.getAllBookmarks().collect { list ->
                _bookmarkUiState.update { it.copy(bookmarks = list) }
                refreshBookmarkEtas()
            }
        }
    }

    fun refreshBookmarkEtas() {
        viewModelScope.launch {
            _bookmarkUiState.update { it.copy(isLoading = true) }
            val currentBookmarks = _bookmarkUiState.value.bookmarks
            val resultMap = mutableMapOf<String, List<BusEtaUiItem>>()

            for (bm in currentBookmarks) {
                val etas = repository.fetchEtaForBookmark(bm)
                resultMap[bm.id] = etas
            }

            val nowStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            _bookmarkUiState.update {
                it.copy(
                    isLoading = false,
                    etaMap = resultMap,
                    lastUpdatedText = "最後更新：$nowStr (每分鐘自動更新)"
                )
            }

            val currentSelectedRoute = _searchUiState.value.selectedRoute
            val currentStopList = _searchUiState.value.stopList
            if (currentSelectedRoute != null && currentStopList.isNotEmpty()) {
                fetchRouteStopsEtas(currentSelectedRoute, currentStopList)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}
