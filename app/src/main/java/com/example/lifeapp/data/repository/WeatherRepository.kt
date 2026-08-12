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
    // 預設地區英中對照表 (僅作排序輔助，找不到也不會掛掉)
    private val districtNameMap = mapOf(
        "Hong Kong Observatory" to "香港天文台",
        "Chek Lap Kok" to "赤鱲角",
        "Cheung Chau" to "長洲",
        "Clear Water Bay" to "清水灣",
        "Deep Water Bay" to "深水灣",
        "Happy Valley" to "跑馬地",
        "Hong Kong Park" to "香港公園",
        "Kai Tak Runway Park" to "啟德跑道公園",
        "King's Park" to "京士柏",
        "Kowloon City" to "九龍城",
        "Kwun Tong" to "觀塘",
        "Lamma Island" to "南丫島",
        "Lau Fau Shan" to "流浮山",
        "Ngong Ping" to "昂坪",
        "Peng Chau" to "坪洲",
        "Sai Kung" to "西貢",
        "Sha Tin" to "沙田",
        "Sham Shui Po" to "深水埗",
        "Shau Kei Wan" to "筲箕灣",
        "Shek Kong" to "石崗",
        "Stanley" to "赤柱",
        "Ta Kwu Ling" to "打鼓嶺",
        "Tai Mei Tuk" to "大美督",
        "Tai Mo Shan" to "大帽山",
        "Tai Po" to "大埔",
        "Tate's Cairn" to "大老山",
        "Tseung Kwan O" to "將軍澳",
        "Tsing Yi" to "青衣",
        "Tsuen Wan Ho Koon" to "荃灣可觀",
        "Tsuen Wan Shing Mun Valley" to "荃灣城門谷",
        "Tuen Mun" to "屯門",
        "Wong Chuk Hang" to "黃竹坑",
        "Wong Tai Sin" to "黃大仙",
        "Yuen Long Park" to "元朗公園"
    )

    suspend fun fetchWeatherInfo(): WeatherUiState {
        return runCatching {
            // 1. 即時天氣 (rhrread) - 核心
            val rawRealtime = runCatching { hkoApiService.getRealtimeWeatherRaw() }.getOrNull()
            val realObj = if (rawRealtime?.isJsonObject == true) rawRealtime.asJsonObject else JsonObject()

            // 2. 今日天氣預報 (flw)
            val rawToday = runCatching { hkoApiService.getTodayForecastRaw() }.getOrNull()
            val todayObj = if (rawToday?.isJsonObject == true) rawToday.asJsonObject else JsonObject()

            // 3. 九天天氣預報 (fnd)
            val rawNineDay = runCatching { hkoApiService.getNineDayForecastRaw() }.getOrNull()
            val nineDayObj = if (rawNineDay?.isJsonObject == true) rawNineDay.asJsonObject else JsonObject()

            // 4. 生效中警告 (warnsum & warningInfo)
            val rawWarningSum = runCatching { hkoApiService.getWarningSummaryRaw() }.getOrNull()
            val rawWarningDetail = runCatching { hkoApiService.getWarningInfoRaw() }.getOrNull()

            // --- A. 解析警告 ---
            val warningDetailsMap = mutableMapOf<String, String>()
            if (rawWarningDetail?.isJsonObject == true && rawWarningDetail.asJsonObject.has("details")) {
                runCatching {
                    val detailsArr = rawWarningDetail.asJsonObject.getAsJsonArray("details")
                    detailsArr.forEach { elem ->
                        if (elem.isJsonObject) {
                            val obj = elem.asJsonObject
                            val code = if (obj.has("warningStatementCode")) obj.get("warningStatementCode").asString else ""
                            val contents = if (obj.has("contents")) {
                                val arr = obj.getAsJsonArray("contents")
                                val sb = StringBuilder()
                                arr.forEach { sb.append(it.asString).append("\n\n") }
                                sb.toString().trim()
                            } else ""
                            if (code.isNotBlank()) warningDetailsMap[code] = contents
                        }
                    }
                }
            }

            val warnings = mutableListOf<WeatherWarningItem>()
            if (rawWarningSum?.isJsonObject == true) {
                val warnObj = rawWarningSum.asJsonObject
                warnObj.keySet().forEach { key ->
                    runCatching {
                        val itemObj = warnObj.getAsJsonObject(key)
                        val name = if (itemObj.has("name")) itemObj.get("name").asString else key
                        val code = if (itemObj.has("code")) itemObj.get("code").asString else key
                        val detail = warningDetailsMap[code] ?: "特別天氣警告生效中，請留意最新廣播。"
                        warnings.add(WeatherWarningItem(code = code, name = name, details = detail))
                    }
                }
            }

            // --- B. 解析分區氣溫、濕度與 UV ---
            val districtList = mutableListOf<DistrictTemperature>()
            var globalHumidity: Int? = null
            var uvInfo: UvIndexInfo? = null

            // 濕度
            if (realObj.has("humidity") && realObj.getAsJsonObject("humidity").has("data")) {
                runCatching {
                    val humArr = realObj.getAsJsonObject("humidity").getAsJsonArray("data")
                    if (humArr.size() > 0 && humArr.get(0).isJsonObject) {
                        globalHumidity = humArr.get(0).asJsonObject.get("value")?.asInt
                    }
                }
            }

            // UV
            if (realObj.has("uvindex") && realObj.getAsJsonObject("uvindex").has("data")) {
                runCatching {
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
            }

            // 分區氣溫 (超強容錯解析)
            if (realObj.has("temperature") && realObj.getAsJsonObject("temperature").has("data")) {
                val dataArr = realObj.getAsJsonObject("temperature").getAsJsonArray("data")
                dataArr.forEach { elem ->
                    if (elem.isJsonObject) {
                        val item = elem.asJsonObject
                        val placeTc = if (item.has("place")) item.get("place").asString else ""
                        
                        val tempVal = when {
                            !item.has("value") -> 0
                            item.get("value").isJsonPrimitive && item.get("value").asJsonPrimitive.isNumber -> item.get("value").asInt
                            else -> item.get("value").asString.toIntOrNull() ?: 0
                        }

                        // 自動配對英文名稱，若找不到則降級顯示原名
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

            // 依英文排序
            val sortedDistricts = districtList.sortedBy { it.placeEn }

            // --- C. 解析今日天氣預報 ---
            val todayForecastDesc = when {
                todayObj.has("forecastDesc") -> todayObj.get("forecastDesc").asString
                todayObj.has("generalSituation") -> todayObj.get("generalSituation").asString
                realObj.has("generalSituation") -> realObj.get("generalSituation").asString
                else -> "本港地區天氣情況良好。"
            }

            // --- D. 解析九天天氣預報 ---
            val forecastList = mutableListOf<ForecastItem>()
            if (nineDayObj.has("weatherForecast")) {
                nineDayObj.getAsJsonArray("weatherForecast").forEach { elem ->
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

            val formattedTimeStr = SimpleDateFormat("yyyyMMdd HH:mm:ss", Locale.getDefault()).format(Date())

            WeatherUiState(
                isLoading = false,
                warningSummary = warnings,
                districtTemperatures = sortedDistricts,
                uvIndexInfo = uvInfo,
                todayForecast = todayForecastDesc,
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
