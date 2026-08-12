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
    // 地區英中名對照表 (確保中英文齊全且用英文排序)
    private val districtNameMap = mapOf(
        "Hong Kong Observatory" to "香港天文台",
        "King's Park" to "京士柏",
        "Wong Chuk Hang" to "黃竹坑",
        "Ta Kwu Ling" to "打鼓嶺",
        "Lau Fau Shan" to "流浮山",
        "Tai Po" to "大埔",
        "Sha Tin" to "沙田",
        "Tuen Mun" to "屯門",
        "Tseung Kwan O" to "將軍澳",
        "Sai Kung" to "西貢",
        "Chek Lap Kok" to "赤鱲角",
        "Tsing Yi" to "青衣",
        "Shek Kong" to "石崗",
        "Tsuen Wan Ho Koon" to "荃灣可觀",
        "Tsuen Wan Shing Mun Valley" to "荃灣城門谷",
        "Hong Kong Park" to "香港公園",
        "Shau Kei Wan" to "筲箕灣",
        "Kowloon City" to "九龍城",
        "Happy Valley" to "跑馬地",
        "Wong Tai Sin" to "黃大仙",
        "Stanley" to "赤柱",
        "Kwun Tong" to "觀塘",
        "Deep Water Bay" to "深水灣",
        "Peng Chau" to "坪洲",
        "Lamma Island" to "南丫島"
    )

    suspend fun fetchWeatherInfo(): WeatherUiState {
        return runCatching {
            val rawWarningSum = runCatching { hkoApiService.getWarningSummaryRaw() }.getOrNull()
            val rawWarningDetail = runCatching { hkoApiService.getWarningInfoRaw() }.getOrNull()
            val rawRealtime = runCatching { hkoApiService.getRealtimeWeatherRaw() }.getOrNull()
            val rawToday = runCatching { hkoApiService.getTodayForecastRaw() }.getOrNull()
            val rawNineDay = runCatching { hkoApiService.getNineDayForecastRaw() }.getOrNull()

            // 1. 解析警告詳情 (warningInfo)
            val warningDetailsMap = mutableMapOf<String, String>()
            if (rawWarningDetail?.isJsonObject == true && rawWarningDetail.asJsonObject.has("details")) {
                val detailsArr = rawWarningDetail.asJsonObject.getAsJsonArray("details")
                detailsArr.forEach { elem ->
                    if (elem.isJsonObject) {
                        val obj = elem.asJsonObject
                        val code = if (obj.has("warningStatementCode")) obj.get("warningStatementCode").asString else ""
                        val contents = if (obj.has("contents")) {
                            val arr = obj.getAsJsonArray("contents")
                            val sb = StringBuilder()
                            arr.forEach { sb.append(it.asString).append("\n") }
                            sb.toString().trim()
                        } else ""
                        if (code.isNotBlank()) warningDetailsMap[code] = contents
                    }
                }
            }

            val warnings = mutableListOf<WeatherWarningItem>()
            if (rawWarningSum?.isJsonObject == true) {
                val warnObj = rawWarningSum.asJsonObject
                warnObj.keySet().forEach { key ->
                    val itemObj = warnObj.getAsJsonObject(key)
                    val name = if (itemObj.has("name")) itemObj.get("name").asString else key
                    val code = if (itemObj.has("code")) itemObj.get("code").asString else key
                    val detail = warningDetailsMap[code] ?: "目前發出之特別天氣警告消息，請留意最新天氣廣播。"
                    warnings.add(WeatherWarningItem(code = code, name = name, details = detail))
                }
            }

            // 2. 解析分區氣溫、濕度與 UV
            val districtList = mutableListOf<DistrictTemperature>()
            var globalHumidity: Int? = null
            var uvInfo: UvIndexInfo? = null

            if (rawRealtime?.isJsonObject == true) {
                val realObj = rawRealtime.asJsonObject

                // 解析全港相對濕度 (humidity -> data)
                if (realObj.has("humidity") && realObj.getAsJsonObject("humidity").has("data")) {
                    val humArr = realObj.getAsJsonObject("humidity").getAsJsonArray("data")
                    if (humArr.size() > 0 && humArr.get(0).isJsonObject) {
                        globalHumidity = humArr.get(0).asJsonObject.get("value")?.asInt
                    }
                }

                // 解析 UV Index (uvindex -> data)
                if (realObj.has("uvindex") && realObj.getAsJsonObject("uvindex").has("data")) {
                    val uvArr = realObj.getAsJsonObject("uvindex").getAsJsonArray("data")
                    if (uvArr.size() > 0 && uvArr.get(0).isJsonObject) {
                        val uObj = uvArr.get(0).asJsonObject
                        val valStr = if (uObj.has("value")) uObj.get("value").asString else ""
                        val descStr = if (uObj.has("desc")) uObj.get("desc").asString else ""
                        if (valStr.isNotBlank()) {
                            uvInfo = UvIndexInfo(value = valStr, desc = descStr)
                        }
                    }
                }

                // 解析分區氣溫
                if (realObj.has("temperature") && realObj.getAsJsonObject("temperature").has("data")) {
                    realObj.getAsJsonObject("temperature").getAsJsonArray("data").forEach { elem ->
                        if (elem.isJsonObject) {
                            val item = elem.asJsonObject
                            val placeTc = if (item.has("place")) item.get("place").asString else ""
                            val tempVal = when {
                                !item.has("value") -> 0
                                item.get("value").isJsonPrimitive && item.get("value").asJsonPrimitive.isNumber -> item.get("value").asInt
                                else -> item.get("value").asString.toIntOrNull() ?: 0
                            }

                            // 搵番英文名以作排序
                            val placeEn = districtNameMap.entries.firstOrNull { it.value == placeTc }?.key ?: placeTc

                            if (placeTc.isNotBlank()) {
                                districtList.add(
                                    DistrictTemperature(
                                        placeTc = placeTc,
                                        placeEn = placeEn,
                                        tempValue = tempVal,
                                        humidityValue = globalHumidity
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 按照英文名稱 (Alphabetically) 排序
            val sortedDistricts = districtList.sortedBy { it.placeEn }

            // 3. 解析今日天氣預報
            var todayForecastDesc = ""
            if (rawToday?.isJsonObject == true) {
                val todayObj = rawToday.asJsonObject
                todayForecastDesc = when {
                    todayObj.has("forecastDesc") -> todayObj.get("forecastDesc").asString
                    todayObj.has("generalSituation") -> todayObj.get("generalSituation").asString
                    else -> ""
                }
            }

            // 4. 解析九天天氣預報
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

            // 要求 1：格式化為 yyyyMMdd HH:mm:ss
            val formattedTimeStr = SimpleDateFormat("yyyyMMdd HH:mm:ss", Locale.getDefault()).format(Date())

            WeatherUiState(
                isLoading = false,
                warningSummary = warnings,
                districtTemperatures = sortedDistricts,
                uvIndexInfo = uvInfo,
                todayForecast = todayForecastDesc.ifBlank { "本港地區天氣情況良好。" },
                nineDayForecast = forecastList,
                updateTime = formattedTimeStr,
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
