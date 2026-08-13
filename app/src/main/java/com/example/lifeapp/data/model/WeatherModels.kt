package com.example.lifeapp.data.model

import com.google.gson.annotations.SerializedName

// 1. 即時天氣 (rhrread) 數據模型
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

// 2. 9 天天氣預報 (fnd) 數據模型
data class HkoFndResponse(
    val weatherForecast: List<DayForecast> = emptyList(),
    val generalSituation: String? = null
)

data class DayForecast(
    val forecastDate: String = "",
    val week: String = "",
    val forecastWeather: String = "",
    val forecastMaxtemp: TemperatureValue? = null,
    val forecastMintemp: TemperatureValue? = null,
    val forecastRh: HumidityValue? = null,
    val PSR: String = "" // 每日本地預報雨量機率
)

data class TemperatureValue(
    val value: Int = 0,
    val unit: String = "C"
)

data class HumidityValue(
    val value: Int = 0,
    val unit: String = "%"
)
