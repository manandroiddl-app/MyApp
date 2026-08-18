package com.example.lifeapp.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeapp.data.model.WeatherUiState
import com.example.lifeapp.data.repository.WeatherRepository
import com.example.lifeapp.ui.common.AutoRefreshDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    companion object {
        private var memoryCache: WeatherUiState? = null
    }

    private val _uiState = MutableStateFlow(memoryCache ?: WeatherUiState())
    
    val uiState: StateFlow<WeatherUiState> = _uiState
        .asStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = memoryCache ?: WeatherUiState()
        )

    // 🛡️ 引入 Common 輪詢元件（預設 60 秒）
    private val autoRefreshDelegate = AutoRefreshDelegate(viewModelScope) {
        loadWeatherData(isSilent = true)
    }

    init {
        loadWeatherData()
        startAutoRefresh()
    }

    fun startAutoRefresh() {
        autoRefreshDelegate.start()
    }

    fun stopAutoRefresh() {
        autoRefreshDelegate.stop()
    }

    fun loadWeatherData(isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent && _uiState.value.updateTime.isEmpty()) {
                _uiState.update { it.copy(isLoading = true) }
            }

            try {
                val result = weatherRepository.fetchWeatherInfo()

                if (result.updateTime.isNotEmpty()) {
                    val newState = result.copy(
                        isLoading = false,
                        isApparentTempMode = _uiState.value.isApparentTempMode
                    )
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

    /**
     * 🎯 切換分區顯示模式 (氣溫 <-> 體感溫度)
     */
    fun toggleTemperatureMode() {
        _uiState.update { currentState ->
            val updatedMode = !currentState.isApparentTempMode
            val newState = currentState.copy(isApparentTempMode = updatedMode)
            memoryCache = newState
            newState
        }
    }

    fun refresh() {
        loadWeatherData(isSilent = false)
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}
