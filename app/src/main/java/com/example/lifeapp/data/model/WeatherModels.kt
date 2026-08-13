package com.example.lifeapp.data.model

import com.google.gson.annotations.SerializedName

data class HkoRhrreadResponse(
    val temperature: TemperatureContainer? = null,
    val humidity: HumidityContainer? = null,
    val rainfall: RainfallContainer? = null,
    val warningMessage: List<String>? = null,
    val uvindex: UvIndexContainer? = null,
    val updateTime: String? = null
)

data class TemperatureContainer(
    val data: List<PlaceData> = emptyList(),
    val recordTime: String? = null
)

data class HumidityContainer(
    val data: List<PlaceHumidityData> = emptyList(),
    val recordTime: String? = null
)

data class PlaceData(
    val place: String = "",
    val value: Int = 0,
    val unit: String = "C"
)

data class PlaceHumidityData(
    val place: String = "",
    val value: Int = 0,
    val unit: String = "%"
)

// 👈 新增雨量資料模型
data class RainfallContainer(
    val data: List<PlaceRainfallData> = emptyList(),
    val startTime: String? = null,
    val endTime: String? = null
)

data class PlaceRainfallData(
    val place: String = "",
    val max: Int = 0,
    val min: Int = 0,
    val unit: String = "mm"
)

data class UvIndexContainer(
    val data: List<UvData> = emptyList()
)

data class UvData(
    val place: String = "",
    val value: Double = 0.0,
    val desc: String = ""
)
