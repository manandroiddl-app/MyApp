package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.HkoApiService
import com.example.lifeapp.data.model.*
import com.google.gson.JsonElement
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val apiService: HkoApiService
) {
    suspend fun fetchFullWeatherData(): FullWeatherUiState = supervisorScope {
        val realtimeDeferred = async { runCatching { apiService.getRealtimeWeatherRaw() }.getOrNull() }
        val warnsumDeferred = async { runCatching { apiService.getWarningSummaryRaw() }.getOrNull() }
        val warningInfoDeferred = async { runCatching { apiService.getWarningInfoRaw() }.getOrNull() }
        val swtDeferred = async { runCatching { apiService.getSpecialWeatherTipsRaw() }.getOrNull() }
        val todayDeferred = async { runCatching { apiService.getTodayForecastRaw() }.getOrNull() }
        val nineDayDeferred = async { runCatching { apiService.getNineDayForecastRaw() }.getOrNull() }

        val realtimeRaw = realtimeDeferred.await()
        val warnsumRaw = warnsumDeferred.await()
        val warningInfoRaw = warningInfoDeferred.await()
        val swtRaw = swtDeferred.await()
        val todayRaw = todayDeferred.await()
        val nineDayRaw = nineDayDeferred.await()

        // === 1. 解析生效中警告 (warnsum) ===
        val warningList = mutableListOf<String>()
        try {
            if (warnsumRaw != null && warnsumRaw.isJsonObject) {
                warnsumRaw.asJsonObject.entrySet().forEach { entry ->
                    val obj = entry.value.asJsonObject
                    if (obj.has("name")) {
                        warningList.add(obj.get("name").asString)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Parse warnsum failed", e)
        }

        // === 1. 解析警告詳細內文 (warninginfo) ===
        val warningDetailMap = mutableMapOf<String, String>()
        try {
            if (warningInfoRaw != null && warningInfoRaw.isJsonObject) {
                val infoObj = warningInfoRaw.asJsonObject
                if (infoObj.has("details") && !infoObj.get("details").isJsonNull) {
                    val detailsArray = infoObj.getAsJsonArray("details")
                    for (i in 0 until detailsArray.size()) {
                        val item = detailsArray.get(i).asJsonObject
                        val name = if (item.has("warningStatementCode")) item.get("warningStatementCode").asString else "詳細說明"
                        val contents = mutableListOf<String>()
                        if (item.has("contents") && !item.get("contents").isJsonNull) {
                            val contentsArray = item.getAsJsonArray("contents")
                            for (j in 0 until contentsArray.size()) {
                                contents.add(contentsArray.get(j).asString)
                            }
                        }
                        warningDetailMap[name] = contents.joinToString("\n\n")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Parse warninginfo failed", e)
        }

        // === 1. 解析特別天氣提示 (swt) ===
        val swtTips = mutableListOf<String>()
        try {
            if (swtRaw != null && swtRaw.isJsonObject) {
                val swtObj = swtRaw.asJsonObject
                if (swtObj.has("swt") && !swtObj.get("swt").isJsonNull) {
                    val swtArray = swtObj.getAsJsonArray("swt")
                    for (i in 0 until swtArray.size()) {
                        val item = swtArray.get(i).asJsonObject
                        if (item.has("desc")) {
                            swtTips.add(item.get("desc").asString)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Parse swt failed", e)
        }

        // === 2. 解析分區天氣 (rhrread) ===
        val locationList = mutableListOf<LocationStation>()
        var humidityVal = "--%"
        var uvVal = "無數據"
        
        // 預設更新時間格式
        val outputFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault())
        var updateTimeText = "最後更新時間：${outputFormat.format(Date())}"

        try {
            if (realtimeRaw != null && realtimeRaw.isJsonObject) {
                val root = realtimeRaw.asJsonObject

                // 分區氣溫
                if (root.has("temperature") && !root.get("temperature").isJsonNull) {
                    val tempObj = root.getAsJsonObject("temperature")
                    if (tempObj.has("data") && !tempObj.get("data").isJsonNull) {
                        val tempArray = tempObj.getAsJsonArray("data")
                        for (i in 0 until tempArray.size()) {
                            val obj = tempArray.get(i).asJsonObject
                            val place = obj.get("place").asString
                            val value = obj.get("value").asInt
                            val enName = stationNameEnMap[place] ?: place
                            locationList.add(LocationStation(nameTc = place, nameEn = enName, temp = value))
                        }
                        locationList.sortBy { it.nameEn }
                    }
                }

                // 相對濕度
                if (root.has("humidity") && !root.get("humidity").isJsonNull) {
                    val humiObj = root.getAsJsonObject("humidity")
                    if (humiObj.has("data") && !humiObj.get("data").isJsonNull) {
                        val humiArray = humiObj.getAsJsonArray("data")
                        if (humiArray.size() > 0) {
                            val humiVal = humiArray.get(0).asJsonObject.get("value").asInt
                            humidityVal = "$humiVal%"
                        }
                    }
                }

                // 紫外線指數
                if (root.has("uvindex") && !root.get("uvindex").isJsonNull) {
                    val uvObj = root.getAsJsonObject("uvindex")
                    if (uvObj.has("data") && !uvObj.get("data").isJsonNull) {
                        val uvArray = uvObj.getAsJsonArray("data")
                        if (uvArray.size() > 0) {
                            val firstUv = uvArray.get(0).asJsonObject
                            val valNum = firstUv.get("value").asFloat
                            val desc = if (firstUv.has("desc")) firstUv.get("desc").asString else ""
                            uvVal = "$valNum ($desc)"
                        }
                    }
                }

                // 更新時間：解析 rawTime 轉為 yyyy年MM月dd日 HH:mm:ss 格式
                val rawTime = if (root.has("recordTime")) {
                    root.get("recordTime").asString
                } else if (root.has("temperature") && root.getAsJsonObject("temperature").has("recordTime")) {
                    root.getAsJsonObject("temperature").get("recordTime").asString
                } else ""

                if (rawTime.isNotEmpty()) {
                    try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val date = inputFormat.parse(rawTime)
                        if (date != null) {
                            updateTimeText = "最後更新時間：${outputFormat.format(date)}"
                        }
                    } catch (e: Exception) {
                        updateTimeText = "最後更新時間：${outputFormat.format(Date())}"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Parse rhrread failed", e)
        }

        // === 3. 解析今日天氣預報 (flw) ===
        var todayDesc = "天文台現正更新天氣預報資訊。"
        try {
            if (todayRaw != null && todayRaw.isJsonObject) {
                val todayObj = todayRaw.asJsonObject
                if (todayObj.has("forecastDesc") && !todayObj.get("forecastDesc").isJsonNull) {
                    todayDesc = todayObj.get("forecastDesc").asString
                } else if (todayObj.has("generalSituation") && !todayObj.get("generalSituation").isJsonNull) {
                    todayDesc = todayObj.get("generalSituation").asString
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Parse flw failed", e)
        }

        // === 4. 解析九日天氣預報 (fnd) ===
        val nineDays = mutableListOf<DayForecast>()
        try {
            if (nineDayRaw != null && nineDayRaw.isJsonObject) {
                val fndObj = nineDayRaw.asJsonObject
                if (fndObj.has("weatherForecast") && !fndObj.get("weatherForecast").isJsonNull) {
                    val forecastArray = fndObj.getAsJsonArray("weatherForecast")
                    for (i in 0 until forecastArray.size()) {
                        val item = forecastArray.get(i).asJsonObject
                        val date = item.get("forecastDate")?.asString ?: ""
                        val week = item.get("week")?.asString ?: ""
                        val wind = item.get("forecastWind")?.asString ?: ""
                        val weather = item.get("forecastWeather")?.asString ?: ""

                        val maxT = if (item.has("forecastMaxtemp")) item.getAsJsonObject("forecastMaxtemp").get("value").asInt else 0
                        val minT = if (item.has("forecastMintemp")) item.getAsJsonObject("forecastMintemp").get("value").asInt else 0
                        val maxRh = if (item.has("forecastMaxrh")) item.getAsJsonObject("forecastMaxrh").get("value").asInt else 0
                        val minRh = if (item.has("forecastMinrh")) item.getAsJsonObject("forecastMinrh").get("value").asInt else 0

                        nineDays.add(
                            DayForecast(
                                forecastDate = date,
                                week = week,
                                forecastWind = wind,
                                forecastWeather = weather,
                                forecastMaxtemp = TempVal(maxT, "C"),
                                forecastMintemp = TempVal(minT, "C"),
                                forecastMaxrh = TempVal(maxRh, "%"),
                                forecastMinrh = TempVal(minRh, "%")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Parse fnd failed", e)
        }

        val defaultLoc = locationList.firstOrNull { it.nameTc == "香港天文台" }
            ?: locationList.firstOrNull()
            ?: LocationStation("香港天文台", "Hong Kong Observatory", null)

        val hasAnyData = locationList.isNotEmpty() || nineDays.isNotEmpty() || todayDesc != "天文台現正更新天氣預報資訊。"

        FullWeatherUiState(
            isLoading = false,
            warnings = warningList,
            warningDetailsMap = warningDetailMap,
            specialWeatherTips = swtTips,
            locations = locationList,
            selectedLocation = defaultLoc,
            currentHumidity = humidityVal,
            currentUv = uvVal,
            todayForecastDesc = todayDesc,
            nineDayForecasts = nineDays,
            updateTimeText = updateTimeText,
            errorMessage = if (!hasAnyData) "無法連接香港天文台，請檢查網路連線。" else null
        )
    }
}
