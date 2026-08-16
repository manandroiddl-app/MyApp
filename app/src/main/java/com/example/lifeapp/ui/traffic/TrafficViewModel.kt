package com.example.lifeapp.ui.traffic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeapp.data.model.TrafficUiState
import com.example.lifeapp.data.repository.TrafficRepository
import com.example.lifeapp.ui.common.AutoRefreshDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrafficViewModel @Inject constructor(
    private val repository: TrafficRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrafficUiState())
    val uiState: StateFlow<TrafficUiState> = _uiState.asStateFlow()

    // 🛡️ 引入 Common 輪詢元件（預設 60 秒）
    private val autoRefreshDelegate = AutoRefreshDelegate(viewModelScope) {
        loadTrafficData(isSilent = true)
    }

    init {
        loadTrafficData(isSilent = false)
    }

    fun refresh() {
        loadTrafficData(isSilent = false)
    }

    fun loadTrafficData(isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent && _uiState.value.trafficNews.isEmpty()) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            val newState = repository.fetchTrafficNews()
            _uiState.value = newState
        }
    }

    fun startAutoRefresh() {
        autoRefreshDelegate.start()
    }

    fun stopAutoRefresh() {
        autoRefreshDelegate.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}
