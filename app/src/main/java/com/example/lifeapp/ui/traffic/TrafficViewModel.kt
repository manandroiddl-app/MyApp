package com.example.lifeapp.ui.traffic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeapp.data.model.TrafficUiState
import com.example.lifeapp.data.repository.TrafficRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrafficViewModel @Inject constructor(
    private val repository: TrafficRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrafficUiState())
    val uiState: StateFlow<TrafficUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        loadTrafficData(isSilent = false)
    }

    fun refresh() {
        loadTrafficData(isSilent = false)
    }

    /**
     * @param isSilent 若為 true，則背景靜默更新，不跳出全頁 Loading 畫面，達成無縫體驗
     */
    fun loadTrafficData(isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent && _uiState.value.trafficNews.isEmpty()) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            val newState = repository.fetchTrafficNews()
            _uiState.value = newState
        }
    }

    /**
     * 啟動 1 分鐘（60 秒）自動靜默輪詢 Timer
     */
    fun startAutoRefresh() {
        stopAutoRefresh()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(60000L) // 每 60 秒（1 分鐘）觸發一次
                loadTrafficData(isSilent = true)
            }
        }
    }

    /**
     * 停止自動輪詢 Timer（省電與資源釋放）
     */
    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}
