package com.example.lifeapp.ui.traffic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeapp.data.model.TrafficUiState
import com.example.lifeapp.data.repository.TrafficRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrafficViewModel @Inject constructor(
    private val repository: TrafficRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrafficUiState())
    val uiState: StateFlow<TrafficUiState> = _uiState.asStateFlow()

    init {
        loadTrafficData()
    }

    fun refresh() {
        loadTrafficData()
    }

    fun loadTrafficData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val newState = repository.fetchTrafficNews()
            _uiState.value = newState
        }
    }
}
