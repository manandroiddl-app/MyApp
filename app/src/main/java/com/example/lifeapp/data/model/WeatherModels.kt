package com.example.lifeapp.data.model

import com.google.gson.annotations.SerializedName

data class DistrictTemperature(
    val place: String = "",
    val value: Int = 0,
    val unit: String = "C"
)

data class ForecastVal(
    val value: Int = 0,
    val unit: String = ""
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
