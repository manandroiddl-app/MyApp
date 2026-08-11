package com.example.lifeapp.ui.bus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeapp.data.model.*
import com.example.lifeapp.data.repository.KmbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private var autoRefreshJob: Job? = null

    init {
        loadAllRoutes()
        observeBookmarks()
        startAutoRefreshTimer()
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
            state.copy(searchQuery = query, filteredRoutes = filtered)
        }
    }

    fun selectRoute(route: KmbRoute) {
        viewModelScope.launch {
            _searchUiState.update { it.copy(isLoading = true, selectedRoute = route) }
            val stops = repository.fetchRouteStopsWithDetail(route.route, route.bound, route.serviceType)
            _searchUiState.update { it.copy(isLoading = false, stopList = stops) }
        }
    }

    fun clearSelectedRoute() {
        _searchUiState.update { it.copy(selectedRoute = null, stopList = emptyList()) }
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
        }
    }

    // 🌟 定時 1 分鐘自動刷新機制
    private fun startAutoRefreshTimer() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(60000) // 60秒 = 1分鐘
                refreshBookmarkEtas()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
