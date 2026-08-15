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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    companion object {
        // 全局記憶體快取 (In-Memory Cache)，避免 SavedStateHandle Parcelable 序列化崩潰問題
        // 確保 App 鎖屏解鎖或切換 App 時 100% 不丟失數據且不崩潰
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
            // 只有在完全沒有數據（快取與現有 State 都為空）時才顯示 Loading 轉圈
            if (_uiState.value.updateTime.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            
            val result = weatherRepository.fetchWeatherInfo()
            val newState = result.copy(isLoading = false)

            // 更新記憶體 State 及 Companion Object 快取
            _uiState.value = newState
            memoryCache = newState
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
