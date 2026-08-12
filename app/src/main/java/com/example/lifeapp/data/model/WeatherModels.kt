package com.example.lifeapp.data.model

import com.google.gson.annotations.SerializedName

// --- 天氣 API 實體 Response DTO ---

data class TempDataNode(
    @SerializedName("place") val place: String? = null,
    @SerializedName("value") val value: Int? = null,
    @SerializedName("unit") val unit: String? = null
)

data class TempContainer(
    @SerializedName("data") val data: List<TempDataNode>? = null
)

data class CurrentWeatherResponse(
    @SerializedName("generalSituation") val generalSituation: String? = null,
    @SerializedName("updateTime") val updateTime: String? = null,
    @SerializedName("temperature") val temperature: TempContainer? = null,
    @SerializedName("warningMessage") val warningMessage: List<String>? = null
)

data class ApiForecastItem(
    @SerializedName("forecastDate") val forecastDate: String? = null,
    @SerializedName("week") val week: String? = null,
    @SerializedName("forecastWeather") val forecastWeather: String? = null,
    @SerializedName("forecastMaxtemp") val forecastMaxtemp: ForecastVal? = null,
    @SerializedName("forecastMintemp") val forecastMintemp: ForecastVal? = null,
    @SerializedName("forecastMinRh") val forecastMinRh: ForecastVal? = null,
    @SerializedName("forecastMaxRh") val forecastMaxRh: ForecastVal? = null
)

data class NineDayForecastResponse(
    @SerializedName("weatherForecast") val weatherForecast: List<ApiForecastItem>? = null
)

// --- 交通 API 實體 Response DTO ---

data class TrafficNewsItem(
    @SerializedName("ReferenceDate") val referenceDate: String? = null,
    @SerializedName("MsgText") val chinText: String? = null
)

data class TrafficNewsResponse(
    @SerializedName("trafficnews") val trafficnews: List<TrafficNewsItem>? = null
)

// --- UI Domain Models ---

data class DistrictTemperature(
    val place: String = "",
    val value: Int = 0,
    val unit: String = "C"
)

data class ForecastVal(
    @SerializedName("value") val value: Int = 0,
    @SerializedName("unit") val unit: String = ""
)

data class ForecastItem(
    val forecastDate: String = "",
    val week: String = "",
    val forecastWeather: String = "",
    val forecastMaxtemp: ForecastVal = ForecastVal(),
    val forecastMintemp: ForecastVal = ForecastVal(),
    val forecastMinRh: ForecastVal = ForecastVal(),
    val forecastMaxRh: ForecastVal = ForecastVal()
)

data class WeatherUiState(
    val isLoading: Boolean = false,
    val warningStatement: String = "",
    val generalSituation: String = "",
    val updateTime: String = "",
    val districtTemperatures: List<DistrictTemperature> = emptyList(),
    val nineDayForecast: List<ForecastItem> = emptyList(),
    val errorMessage: String? = null
)

data class TrafficUiState(
    val isLoading: Boolean = false,
    val trafficNews: List<TrafficNewsItem> = emptyList(),
    val updateTime: String = "",
    val errorMessage: String? = null
)
