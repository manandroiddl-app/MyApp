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

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        loadWeatherData()
        startAutoRefresh()
    }

    fun loadWeatherData() {
        viewModelScope.launch {
            // 背景自動刷新時不顯示全螢幕 Loading 指示器
            if (_uiState.value.updateTime.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            val result = weatherRepository.fetchWeatherInfo()
            _uiState.value = result
        }
    }

    // 每分鐘 (60 秒) 自動更新天氣數據
    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000L) // 60秒
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
