package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.HkoApiService
import com.example.lifeapp.data.model.*
import com.google.gson.JsonElement
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
            val rawJson: JsonElement = hkoApiService.getRealtimeWeatherRaw()
            val jsonObj = rawJson.asJsonObject

            // 解析 generalSituation
            val generalSituation = jsonObj.get("generalSituation")?.asString ?: ""
            val updateTime = jsonObj.get("updateTime")?.asString ?: ""

            // 解析地區氣溫
            val districtTemps = mutableListOf<DistrictTemperature>()
            jsonObj.getAsJsonObject("temperature")?.getAsJsonArray("data")?.forEach { elem ->
                val obj = elem.asJsonObject
                districtTemps.add(
                    DistrictTemperature(
                        place = obj.get("place")?.asString ?: "",
                        value = obj.get("value")?.asInt ?: 0,
                        unit = obj.get("unit")?.asString ?: "C"
                    )
                )
            }

            val nowStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            WeatherUiState(
                isLoading = false,
                generalSituation = generalSituation,
                updateTime = if (updateTime.isNotBlank()) updateTime else nowStr,
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
