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
        // 全局記憶體快取 (In-Memory Cache)
        private var memoryCache: WeatherUiState? = null
    }

    // 初始化時優先使用記憶體快取
    private val _uiState = MutableStateFlow(memoryCache ?: WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        loadWeatherData()
        startAutoRefresh()
    }

    fun loadWeatherData() {
        viewModelScope.launch {
            // 只有在完全沒有歷史數據（updateTime 為空）時，才顯示轉圈加載
            if (_uiState.value.updateTime.isEmpty()) {
                _uiState.update { it.copy(isLoading = true) }
            }

            val result = weatherRepository.fetchWeatherInfo()

            // 🛡️ 核心修復防禦機制：
            // 只有當 Fetch 到的結果帶有有效 updateTime 時才覆蓋舊數據！
            // 避免 Unlock 或網絡斷連回傳空值時把既有數據洗掉導致白屏。
            if (result.updateTime.isNotEmpty()) {
                val newState = result.copy(isLoading = false)
                _uiState.value = newState
                memoryCache = newState
            } else {
                // 如果抓取失敗或空數據，僅關閉 loading 標記，嚴格保留原有 State/快取數據
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // 每分鐘 (60 秒) 背景自動刷新
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
