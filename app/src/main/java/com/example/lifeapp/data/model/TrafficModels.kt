package com.example.lifeapp.data.model

import com.google.gson.annotations.SerializedName

data class TrafficNewsItem(
    @SerializedName("ReferenceDate") val referenceDate: String? = "",
    @SerializedName("MsgText") val chinText: String? = ""
)

data class TrafficNewsResponse(
    @SerializedName("trafficnews") val trafficnews: List<TrafficNewsItem>? = null
)

data class TrafficUiState(
    val isLoading: Boolean = false,
    val trafficNews: List<TrafficNewsItem> = emptyList(),
    val updateTime: String = "",
    val errorMessage: String? = null
)
