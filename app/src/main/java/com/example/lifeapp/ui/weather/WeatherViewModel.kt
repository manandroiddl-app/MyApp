package com.example.lifeapp.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeapp.data.model.WeatherUiState
import com.example.lifeapp.data.repository.WeatherRepository
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
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    companion object {
        // 全局記憶體快取：確保解鎖或背景切回時數據持續存在
        private var memoryCache: WeatherUiState? = null
    }

    private val _uiState = MutableStateFlow(memoryCache ?: WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        loadWeatherData()
        startAutoRefresh()
    }

    fun loadWeatherData() {
        viewModelScope.launch {
            // 只有在完全無歷史資料時才顯示 Initial Loading
            if (_uiState.value.updateTime.isEmpty()) {
                _uiState.update { it.copy(isLoading = true) }
            }

            try {
                val result = weatherRepository.fetchWeatherInfo()

                // 🛡️ 需求 2 核心防禦：只有當 API 成功回傳且帶有 updateTime 時才覆蓋
                // 如果 Unlock 時網路未就緒導致 API 失敗/回傳空值，嚴格保留舊數據！
                if (result.updateTime.isNotEmpty()) {
                    val newState = result.copy(isLoading = false)
                    _uiState.value = newState
                    memoryCache = newState
                } else {
                    // API 回傳無效/空資料，僅關閉 loading 標記，舊資料完好保留
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                // 捕獲所有網絡斷連 Exception，確保清空資料的情況 0 發生
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                loadWeatherData()
            }
        }
    }

    fun refresh() {
        loadWeatherData()
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
