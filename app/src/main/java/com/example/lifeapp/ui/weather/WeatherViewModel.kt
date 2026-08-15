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
        // 全局記憶體快取：確保解鎖或背景切回時數據持續存在
        private var memoryCache: WeatherUiState? = null
    }

    private val _uiState = MutableStateFlow(memoryCache ?: WeatherUiState())
    
    // 🛡️ 關鍵修復：加入 WhileSubscribed(5_000)，給予 5 秒緩衝記憶期！
    // 當 App 切換 Tab、切背景或螢幕鎖定時，Compose 訂閱斷開，此機制能確保 State 絕不被重置拋空，實現 0 秒無縫交接。
    val uiState: StateFlow<WeatherUiState> = _uiState
        .asStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = memoryCache ?: WeatherUiState()
        )

    private var autoRefreshJob: Job? = null

    init {
        loadWeatherData()
        startAutoRefresh()
    }

    fun loadWeatherData() {
        viewModelScope.launch {
            // 只有在完全無歷史資料（包括 memoryCache 也是空）時才顯示 Initial Loading
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
                    _uiState.update { currentState ->
                        currentState.copy(isLoading = false)
                    }
                }
            } catch (e: Exception) {
                // 捕獲所有網絡斷連 Exception，確保清空資料的情況 0 發生
                _uiState.update { currentState ->
                    currentState.copy(isLoading = false)
                }
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
