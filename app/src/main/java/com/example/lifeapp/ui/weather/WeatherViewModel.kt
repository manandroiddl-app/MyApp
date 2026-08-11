package com.example.lifeapp.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeapp.data.model.FullWeatherUiState
import com.example.lifeapp.data.model.LocationStation
import com.example.lifeapp.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FullWeatherUiState())
    val uiState: StateFlow<FullWeatherUiState> = _uiState.asStateFlow()

    init {
        loadWeatherData()
    }

    // 提供給 UI 的重新整理函式
    fun refresh() {
        loadWeatherData()
    }

    fun loadWeatherData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val newState = repository.fetchFullWeatherData()
            _uiState.value = newState
        }
    }

    fun selectLocation(location: LocationStation) {
        _uiState.update { it.copy(selectedLocation = location) }
    }
}
