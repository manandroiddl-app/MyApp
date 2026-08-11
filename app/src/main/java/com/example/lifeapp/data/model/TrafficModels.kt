package com.example.lifeapp.data.model

// UI 顯示用的乾淨模型
data class TrafficItem(
    val id: String,
    val title: String,
    val timeText: String
)

// Traffic UI 綜合狀態
data class TrafficUiState(
    val isLoading: Boolean = true,
    val items: List<TrafficItem> = emptyList(),
    val updateTimeText: String = "",
    val errorMessage: String? = null
)
