package com.example.lifeapp.data.model

import com.google.gson.annotations.SerializedName

data class TrafficNewsItem(
    @SerializedName("referenceDate") val referenceDate: String = "",
    @SerializedName("chinText") val chinText: String = ""
)

data class TrafficUiState(
    val isLoading: Boolean = false,
    val trafficNews: List<TrafficNewsItem> = emptyList(),
    val updateTime: String = "",
    val errorMessage: String? = null
)
