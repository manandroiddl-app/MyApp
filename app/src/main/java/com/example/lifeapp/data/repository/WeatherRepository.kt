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
    private val nameMap = mapOf(
        "香港天文台" to "Hong Kong Observatory",
        "赤鱲角" to "Chek Lap Kok",
        "長洲" to "Cheung Chau",
        "清水灣" to "Clear Water Bay",
        "深水灣" to "Deep Water Bay",
        "跑馬地" to "Happy Valley",
        "香港公園" to "Hong Kong Park",
        "啟德跑道公園" to "Kai Tak Runway Park",
        "京士柏" to "King's Park",
        "九龍城" to "Kowloon City",
        "觀塘" to "Kwun Tong",
        "南丫島" to "Lamma Island",
        "流浮山" to "Lau Fau Shan",
        "昂坪" to "Ngong Ping",
        "坪洲" to "Peng Chau",
        "西貢" to "Sai Kung",
        "沙田" to "Sha Tin",
        "深水埗" to "Sham Shui Po",
        "筲箕灣" to "Shau Kei Wan",
        "石崗" to "Shek Kong",
        "赤柱" to "Stanley",
        "打鼓嶺" to "Ta Kwu Ling",
        "大美督" to "Tai Mei Tuk",
        "大帽山" to "Tai Mo Shan",
        "大埔" to "Tai Po",
        "大老山" to "Tate's Cairn",
        "將軍澳" to "Tseung Kwan O",
        "青衣" to "Tsing Yi",
        "荃灣可觀" to "Tsuen Wan Ho Koon",
        "荃灣城門谷" to "Tsuen Wan Shing Mun Valley",
        "屯門" to "Tuen Mun",
        "黃竹坑" to "Wong Chuk Hang",
        "黃大仙" to "Wong Tai Sin",
        "元朗公園" to "Yuen Long Park"
    )

    suspend fun fetchWeatherInfo(): WeatherUiState {
        return try {
            val rawRealtime = runCatching { hkoApiService.getRealtimeWeatherRaw() }.getOrNull()
            val rawToday = runCatching { hkoApiService.getTodayForecastRaw() }.getOrNull()
            val rawNineDay = runCatching { hkoApiService.getNineDayForecastRaw() }.getOrNull()
            val rawWarningSum = runCatching { hkoApiService.getWarningSummaryRaw() }.getOrNull()
            val rawWarningDetail = runCatching { hkoApiService.getWarningInfoRaw() }.getOrNull()
            val rawSwt = runCatching { hkoApiService.getSpecialWeatherTipsRaw() }.getOrNull()

            // 1. 警告內文對照
            val detailsMap = mutableMapOf<String, String>()
            if (rawWarningDetail?.isJsonObject == true && rawWarningDetail.asJsonObject.has("details")) {
                try {
                    rawWarningDetail.asJsonObject.getAsJsonArray("details").forEach { elem ->
                        if (elem.isJsonObject) {
                            val obj = elem.asJsonObject
                            val code = if (obj.has("warningStatementCode")) obj.get("warningStatementCode").asString else ""
                            val contents = if (obj.has("contents")) {
                                val sb = StringBuilder()
                                obj.getAsJsonArray("contents").forEach { sb.append(it.asString).append("\n\n") }
                                sb.toString().trim()
                            } else ""
                            if (code.isNotBlank()) detailsMap[code] = contents
                        }
                    }
                } catch (e: Exception) { Log.e("WeatherRepo", "Warning detail parse error", e) }
            }

            val warnings = mutableListOf<WeatherWarningItem>()

            // A. 生效中警告 (warnsum)
            if (rawWarningSum?.isJsonObject == true) {
                val warnObj = rawWarningSum.asJsonObject
                warnObj.keySet().forEach { key ->
                    try {
                        val itemObj = warnObj.getAsJsonObject(key)
                        val name = if (itemObj.has("name")) itemObj.get("name").asString else key
                        val code = if (itemObj.has("code")) itemObj.get("code").asString else key
                        val detail = detailsMap[code] ?: "特別天氣警告生效中，請留意最新廣播。"
                        warnings.add(WeatherWarningItem(code = code, name = name, details = detail))
                    } catch (e: Exception) { Log.e("WeatherRepo", "Warning sum parse error", e) }
                }
            }

            // B. 特別天氣提示 (swt)
            if (rawSwt?.isJsonObject == true && rawSwt.asJsonObject.has("swt")) {
                try {
                    rawSwt.asJsonObject.getAsJsonArray("swt").forEach { elem ->
                        if (elem.isJsonObject) {
                            val obj = elem.asJsonObject
                            val desc = if (obj.has("desc")) obj.get("desc").asString else ""
                            if (desc.isNotBlank()) {
                                warnings.add(
                                    WeatherWarningItem(
                                        code = "SWT",
                                        name = "特別天氣提示",
                                        details = desc
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) { Log.e("WeatherRepo", "SWT parse error", e) }
            }

            // 2. 即時天氣/分區/濕度/UV
            val districtList = mutableListOf<DistrictTemperature>()
            var globalHumidity: Int? = null
            var uvInfo: UvIndexInfo? = null

            if (rawRealtime?.isJsonObject == true) {
                val realObj = rawRealtime.asJsonObject

                // 濕度
                try {
                    if (realObj.has("humidity") && realObj.getAsJsonObject("humidity").has("data")) {
                        val humArr = realObj.getAsJsonObject("humidity").getAsJsonArray("data")
                        if (humArr.size() > 0) {
                            globalHumidity = humArr.get(0).asJsonObject.get("value").asInt
                        }
                    }
                } catch (e: Exception) { Log.e("WeatherRepo", "Humidity error", e) }

                // UV 紫外線指數
                try {
                    if (realObj.has("uvindex") && realObj.getAsJsonObject("uvindex").has("data")) {
                        val uvArr = realObj.getAsJsonObject("uvindex").getAsJsonArray("data")
                        if (uvArr.size() > 0) {
                            val uObj = uvArr.get(0).asJsonObject
                            val vStr = if (uObj.has("value")) uObj.get("value").toString() else ""
                            val dStr = if (uObj.has("desc")) uObj.get("desc").asString else ""
                            if (vStr.isNotBlank()) uvInfo = UvIndexInfo(value = vStr, desc = dStr)
                        }
                    }
                } catch (e: Exception) { Log.e("WeatherRepo", "UV error", e) }

                // 分區氣溫
                try {
                    if (realObj.has("temperature") && realObj.getAsJsonObject("temperature").has("data")) {
                        realObj.getAsJsonObject("temperature").getAsJsonArray("data").forEach { elem ->
                            if (elem.isJsonObject) {
                                val item = elem.asJsonObject
                                val placeTc = if (item.has("place")) item.get("place").asString else ""
                                val tempVal = if (item.has("value")) item.get("value").asInt else 0
                                val placeEn = nameMap[placeTc] ?: placeTc

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
                } catch (e: Exception) { Log.e("WeatherRepo", "Temp array error", e) }
            }

            val sortedDistricts = districtList.sortedBy { it.placeEn }

            // 3. 今日天氣預報
            var todayDesc = ""
            if (rawToday?.isJsonObject == true) {
                val tObj = rawToday.asJsonObject
                todayDesc = when {
                    tObj.has("forecastDesc") -> tObj.get("forecastDesc").asString
                    tObj.has("generalSituation") -> tObj.get("generalSituation").asString
                    else -> ""
                }
            }

            // 4. 九天天氣預報
            val forecastList = mutableListOf<ForecastItem>()
            if (rawNineDay?.isJsonObject == true && rawNineDay.asJsonObject.has("weatherForecast")) {
                try {
                    rawNineDay.asJsonObject.getAsJsonArray("weatherForecast").forEach { elem ->
                        if (elem.isJsonObject) {
                            val f = elem.asJsonObject
                            val date = if (f.has("forecastDate")) f.get("forecastDate").asString else ""
                            val week = if (f.has("week")) f.get("week").asString else ""
                            val weather = if (f.has("forecastWeather")) f.get("forecastWeather").asString else ""
                            
                            val maxTemp = f.getAsJsonObject("forecastMaxtemp")?.get("value")?.asInt ?: 0
                            val minTemp = f.getAsJsonObject("forecastMintemp")?.get("value")?.asInt ?: 0

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
                } catch (e: Exception) { Log.e("WeatherRepo", "Nine day error", e) }
            }

            val formattedTime = SimpleDateFormat("yyyyMMdd HH:mm:ss", Locale.getDefault()).format(Date())

            WeatherUiState(
                isLoading = false,
                warningSummary = warnings,
                districtTemperatures = sortedDistricts,
                uvIndexInfo = uvInfo,
                todayForecast = todayDesc.ifBlank { "本港地區天氣情況良好。" },
                nineDayForecast = forecastList,
                updateTime = formattedTime,
                errorMessage = null
            )
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Fatal Fetch Error", e)
            WeatherUiState(
                isLoading = false,
                errorMessage = "加載天氣失敗: ${e.localizedMessage}"
            )
        }
    }
}
