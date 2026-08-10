package com.example.lifeapp.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeapp.data.model.FullWeatherUiState
import com.example.lifeapp.data.model.LocationStation
import com.example.lifeapp.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FullWeatherUiState())
    val uiState: StateFlow<FullWeatherUiState> = _uiState

    init {
        fetchAllWeatherData()
    }

    fun fetchAllWeatherData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val newState = repository.fetchFullWeatherData()
                _uiState.value = newState
            } catch (e: Exception) {
                _uiState.value = FullWeatherUiState(
                    isLoading = false,
                    errorMessage = "數據載入失敗，請檢查網絡連線。"
                )
            }
        }
    }

    fun selectLocation(location: LocationStation) {
        _uiState.value = _uiState.value.copy(selectedLocation = location)
    }
}
