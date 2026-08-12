package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.HkoApiService
import com.example.lifeapp.data.model.DistrictTemperature
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
            val rawJson = hkoApiService.getRealtimeWeatherRaw()
            val jsonObj = if (rawJson.isJsonObject) rawJson.asJsonObject else JsonObject()

            // 1. 概況與時間
            val generalSituation = when {
                jsonObj.has("generalSituation") -> jsonObj.get("generalSituation").asString
                jsonObj.has("forecastPeriod") -> jsonObj.get("forecastPeriod").asString
                else -> ""
            }
            
            val updateTimeStr = if (jsonObj.has("updateTime")) jsonObj.get("updateTime").asString else ""

            // 2. 地區氣溫解析
            val districtTemps = mutableListOf<DistrictTemperature>()
            if (jsonObj.has("temperature") && jsonObj.getAsJsonObject("temperature").has("data")) {
                val tempArray = jsonObj.getAsJsonObject("temperature").getAsJsonArray("data")
                for (elem in tempArray) {
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

            val nowStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            WeatherUiState(
                isLoading = false,
                generalSituation = generalSituation.ifBlank { "本港地區天氣情況良好。" },
                updateTime = updateTimeStr.ifBlank { nowStr },
                districtTemperatures = districtTemps,
                errorMessage = null
            )
        }.getOrElse { e ->
            Log.e("WeatherRepo", "Fetch weather error", e)
            WeatherUiState(
                isLoading = false,
                errorMessage = "無法獲取天氣資料：${e.localizedMessage}"
            )
        }
    }
}
