package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.HkoApiService
import com.example.lifeapp.data.model.*
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
            // 併發取得 4 支天文台 API 資料
            val rawWarning = runCatching { hkoApiService.getWarningSummaryRaw() }.getOrNull()
            val rawRealtime = runCatching { hkoApiService.getRealtimeWeatherRaw() }.getOrNull()
            val rawToday = runCatching { hkoApiService.getTodayForecastRaw() }.getOrNull()
            val rawNineDay = runCatching { hkoApiService.getNineDayForecastRaw() }.getOrNull()

            // 1. 解析生效中警告 (warnsum)
            val warnings = mutableListOf<WeatherWarningItem>()
            if (rawWarning?.isJsonObject == true) {
                val warnObj = rawWarning.asJsonObject
                warnObj.keySet().forEach { key ->
                    val itemObj = warnObj.getAsJsonObject(key)
                    val name = if (itemObj.has("name")) itemObj.get("name").asString else key
                    val code = if (itemObj.has("code")) itemObj.get("code").asString else ""
                    warnings.add(WeatherWarningItem(code = code, name = name))
                }
            }

            // 2. 解析分區氣溫 (rhrread)
            val districtTemps = mutableListOf<DistrictTemperature>()
            var updateTimeStr = ""
            if (rawRealtime?.isJsonObject == true) {
                val realObj = rawRealtime.asJsonObject
                if (realObj.has("updateTime")) updateTimeStr = realObj.get("updateTime").asString
                
                if (realObj.has("temperature") && realObj.getAsJsonObject("temperature").has("data")) {
                    realObj.getAsJsonObject("temperature").getAsJsonArray("data").forEach { elem ->
                        if (elem.isJsonObject) {
                            val item = elem.asJsonObject
                            val place = if (item.has("place")) item.get("place").asString else ""
                            val valueInt = when {
                                !item.has("value") -> 0
                                item.get("value").isJsonPrimitive && item.get("value").asJsonPrimitive.isNumber -> item.get("value").asInt
                                else -> item.get("value").asString.toIntOrNull() ?: 0
                            }
                            if (place.isNotBlank()) {
                                districtTemps.add(DistrictTemperature(place = place, value = valueInt))
                            }
                        }
                    }
                }
            }

            // 3. 解析今日天氣預報 (flw)
            var todayForecastDesc = ""
            if (rawToday?.isJsonObject == true) {
                val todayObj = rawToday.asJsonObject
                todayForecastDesc = when {
                    todayObj.has("forecastDesc") -> todayObj.get("forecastDesc").asString
                    todayObj.has("generalSituation") -> todayObj.get("generalSituation").asString
                    else -> ""
                }
            }

            // 4. 解析九天天氣預報 (fnd)
            val forecastList = mutableListOf<ForecastItem>()
            if (rawNineDay?.isJsonObject == true && rawNineDay.asJsonObject.has("weatherForecast")) {
                rawNineDay.asJsonObject.getAsJsonArray("weatherForecast").forEach { elem ->
                    if (elem.isJsonObject) {
                        val f = elem.asJsonObject
                        val date = if (f.has("forecastDate")) f.get("forecastDate").asString else ""
                        val week = if (f.has("week")) f.get("week").asString else ""
                        val weather = if (f.has("forecastWeather")) f.get("forecastWeather").asString else ""
                        
                        val maxTempInt = runCatching { f.getAsJsonObject("forecastMaxtemp")?.get("value")?.asInt ?: 0 }.getOrDefault(0)
                        val minTempInt = runCatching { f.getAsJsonObject("forecastMintemp")?.get("value")?.asInt ?: 0 }.getOrDefault(0)

                        if (date.isNotBlank()) {
                            forecastList.add(
                                ForecastItem(
                                    forecastDate = date,
                                    week = week,
                                    forecastWeather = weather,
                                    forecastMaxtemp = ForecastVal(value = maxTempInt, unit = "°C"),
                                    forecastMintemp = ForecastVal(value = minTempInt, unit = "°C")
                                )
                            )
                        }
                    }
                }
            }

            val nowStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            WeatherUiState(
                isLoading = false,
                warningSummary = warnings,
                districtTemperatures = districtTemps,
                todayForecast = todayForecastDesc.ifBlank { "本港地區天氣情況良好。" },
                nineDayForecast = forecastList,
                updateTime = updateTimeStr.ifBlank { nowStr },
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
