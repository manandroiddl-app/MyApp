package com.example.lifeapp.ui.weather

import androidx.lifecycle.SavedStateHandle
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
    private val weatherRepository: WeatherRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 透過 SavedStateHandle 自動記憶 State，解決 Process Death 與 Unlock 恢復白屏
    private val _uiState = MutableStateFlow(
        savedStateHandle.get<WeatherUiState>("weather_ui_state") ?: WeatherUiState()
    )
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        loadWeatherData()
        startAutoRefresh()
    }

    fun loadWeatherData() {
        viewModelScope.launch {
            // 只有在完全無資料時（如首次啟動）才設定 isLoading = true
            if (_uiState.value.updateTime.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            val result = weatherRepository.fetchWeatherInfo()
            val newState = result.copy(isLoading = false)

            // 更新記憶體狀態並寫入 SavedStateHandle
            _uiState.value = newState
            savedStateHandle["weather_ui_state"] = newState
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
