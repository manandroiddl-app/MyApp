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
            // 1. 即時天氣 (rhrread)
            val rawCurrent = hkoApiService.getRealtimeWeatherRaw()
            val currentObj = if (rawCurrent.isJsonObject) rawCurrent.asJsonObject else JsonObject()

            // 2. 九天天氣預報 (fnd)
            val rawNineDay = runCatching { hkoApiService.getNineDayForecastRaw() }.getOrNull()
            val nineDayObj = if (rawNineDay?.isJsonObject == true) rawNineDay.asJsonObject else null

            // --- 1. 頂部與本港概況 ---
            // 天文台 rhrread 在不同時間可能是 generalSituation 或 forecastPeriod
            val generalSituation = when {
                currentObj.has("generalSituation") && !currentObj.get("generalSituation").asString.isNullOrBlank() -> 
                    currentObj.get("generalSituation").asString
                currentObj.has("forecastPeriod") && !currentObj.get("forecastPeriod").asString.isNullOrBlank() -> 
                    currentObj.get("forecastPeriod").asString
                else -> "本港地區天氣情況良好。"
            }

            val updateTimeStr = when {
                currentObj.has("updateTime") -> currentObj.get("updateTime").asString
                else -> ""
            }

            // --- 2. 分區氣溫 (temperature -> data 陣列) ---
            val districtTemps = mutableListOf<DistrictTemperature>()
            if (currentObj.has("temperature")) {
                val tempObj = currentObj.get("temperature")
                if (tempObj.isJsonObject && tempObj.asJsonObject.has("data")) {
                    val dataArray = tempObj.asJsonObject.getAsJsonArray("data")
                    for (elem in dataArray) {
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
            }

            // --- 3. 九天天氣預報 (weatherForecast 陣列) ---
            val forecastList = mutableListOf<ForecastItem>()
            if (nineDayObj != null && nineDayObj.has("weatherForecast")) {
                val forecastArray = nineDayObj.getAsJsonArray("weatherForecast")
                for (elem in forecastArray) {
                    if (elem.isJsonObject) {
                        val f = elem.asJsonObject
                        val date = if (f.has("forecastDate")) f.get("forecastDate").asString else ""
                        val week = if (f.has("week")) f.get("week").asString else ""
                        val weather = if (f.has("forecastWeather")) f.get("forecastWeather").asString else ""
                        
                        // 處理最高與最低溫 (forecastMaxtemp -> value)
                        val maxTemp = if (f.has("forecastMaxtemp") && f.getAsJsonObject("forecastMaxtemp").has("value")) {
                            f.getAsJsonObject("forecastMaxtemp").get("value").asInt
                        } else 0

                        val minTemp = if (f.has("forecastMintemp") && f.getAsJsonObject("forecastMintemp").has("value")) {
                            f.getAsJsonObject("forecastMintemp").get("value").asInt
                        } else 0

                        if (date.isNotBlank()) {
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
            }

            val nowStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            WeatherUiState(
                isLoading = false,
                generalSituation = generalSituation,
                updateTime = if (updateTimeStr.isNotBlank()) updateTimeStr else nowStr,
                districtTemperatures = districtTemps,
                nineDayForecast = forecastList,
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
