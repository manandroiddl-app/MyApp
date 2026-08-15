package com.example.lifeapp.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeapp.data.model.WeatherUiState
import com.example.lifeapp.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    companion object {
        // 🛡️ 全局記憶體快取：即使 Process 被殺掉重新開啟（Process Death），也能瞬間取得快取
        private var memoryCache: WeatherUiState? = null
    }

    private val _uiState = MutableStateFlow(memoryCache ?: WeatherUiState())
    
    // 🛡️ 使用 Eagerly 保持長連接，確保切換 APP 切回來時 State 完全不丟失
    val uiState: StateFlow<WeatherUiState> = _uiState
        .asStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = memoryCache ?: WeatherUiState()
        )

    private var autoRefreshJob: Job? = null

    init {
        loadWeatherData()
        startAutoRefresh()
    }

    /**
     * @param isSilent 是否為背景靜默更新（若為 true，絕不出轉圈全頁覆蓋，保持舊數據）
     */
    fun loadWeatherData(isSilent: Boolean = false) {
        viewModelScope.launch {
            // 只有在完全無歷史資料且非靜默更新時，才顯示 Initial Loading
            if (!isSilent && _uiState.value.updateTime.isEmpty()) {
                _uiState.update { it.copy(isLoading = true) }
            }

            try {
                val result = weatherRepository.fetchWeatherInfo()

                // 只有當 API 成功回傳且帶有 updateTime 時才覆蓋舊數據
                if (result.updateTime.isNotEmpty()) {
                    val newState = result.copy(isLoading = false)
                    _uiState.value = newState
                    memoryCache = newState
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                loadWeatherData(isSilent = true)
            }
        }
    }

    fun refresh() {
        loadWeatherData(isSilent = false)
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
