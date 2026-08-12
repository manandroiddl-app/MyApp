package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.HkoApiService
import com.example.lifeapp.data.model.DistrictTemperature
import com.example.lifeapp.data.model.ForecastItem
import com.example.lifeapp.data.model.ForecastVal
import com.example.lifeapp.data.model.WeatherUiState
import com.google.gson.JsonObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val hkoApiService: HkoApiService
) {
    suspend fun fetchWeatherInfo(): WeatherUiState {
        return runCatching {
            // 1. 取得即時天氣 (rhrread)
            val rawCurrent = hkoApiService.getRealtimeWeatherRaw()
            val currentObj = if (rawCurrent.isJsonObject) rawCurrent.asJsonObject else JsonObject()

            // 2. 取得九天天氣預報 (fnd)
            val rawNineDay = runCatching { hkoApiService.getNineDayForecastRaw() }.getOrNull()
            val nineDayObj = if (rawNineDay?.isJsonObject == true) rawNineDay.asJsonObject else null

            // --- 解析即時天氣與概況 ---
            val generalSituation = if (currentObj.has("generalSituation")) {
                currentObj.get("generalSituation").asString
            } else ""

            val updateTimeStr = if (currentObj.has("updateTime")) {
                currentObj.get("updateTime").asString
            } else ""

            // --- 解析分區氣溫 ---
            val districtTemps = mutableListOf<DistrictTemperature>()
            if (currentObj.has("temperature") && currentObj.getAsJsonObject("temperature").has("data")) {
                currentObj.getAsJsonObject("temperature").getAsJsonArray("data").forEach { elem ->
                    if (elem.isJsonObject) {
                        val item = elem.asJsonObject
                        val place = if (item.has("place")) item.get("place").asString else ""
                        val value = if (item.has("value")) item.get("value").asInt else 0
                        val unit = if (item.has("unit")) item.get("unit").asString else "°C"
                        if (place.isNotBlank()) {
                            districtTemps.add(DistrictTemperature(place = place, value = value, unit = unit))
                        }
                    }
                }
            }

            // --- 解析九天天氣預報 ---
            val forecastList = mutableListOf<ForecastItem>()
            if (nineDayObj != null && nineDayObj.has("weatherForecast")) {
                nineDayObj.getAsJsonArray("weatherForecast").forEach { elem ->
                    if (elem.isJsonObject) {
                        val f = elem.asJsonObject
                        val date = if (f.has("forecastDate")) f.get("forecastDate").asString else ""
                        val week = if (f.has("week")) f.get("week").asString else ""
                        val weather = if (f.has("forecastWeather")) f.get("forecastWeather").asString else ""
                        
                        val maxTemp = f.getAsJsonObject("forecastMaxtemp")?.get("value")?.asInt ?: 0
                        val minTemp = f.getAsJsonObject("forecastMintemp")?.get("value")?.asInt ?: 0

                        forecastList.add(
                            ForecastItem(
                                forecastDate = date,
                                week = week,
                                forecastWeather = weather,
                                forecastMaxtemp = ForecastVal(value = maxTemp, unit = "°C"),
                                forecastMintemp = ForecastVal(value = minTemp, unit = "°C")
                            )
                        )
                    }
                }
            }

            val nowStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            WeatherUiState(
                isLoading = false,
                generalSituation = generalSituation.ifBlank { "本港地區天氣情況良好。" },
                updateTime = updateTimeStr.ifBlank { nowStr },
                districtTemperatures = districtTemps,
                nineDayForecast = forecastList,
                errorMessage = null
            )
        }.getOrElse { e ->
            Log.e("WeatherRepo", "Fetch weather error", e)
            WeatherUiState(
                isLoading = false,
                errorMessage = "無法獲取天氣資料"
            )
        }
    }
}
