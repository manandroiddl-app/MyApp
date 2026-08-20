package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.HkoApiService
import com.example.lifeapp.data.local.GenericCacheDao
import com.example.lifeapp.data.model.*
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

@Singleton
class WeatherRepository @Inject constructor(
    private val hkoApiService: HkoApiService,
    genericCacheDao: GenericCacheDao,
    gson: Gson
) : BaseCacheRepository<WeatherUiState>(
    genericCacheDao = genericCacheDao,
    gson = gson,
    cacheKey = "WEATHER_UI_CACHE",
    clazz = WeatherUiState::class.java
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
        "元朗公園" to "Yuen Long Park",
        "中西區" to "Central & Western",
        "灣仔" to "Wan Chai",
        "灣仔區" to "Wan Chai District",
        "東區" to "Eastern District",
        "南區" to "Southern District",
        "油尖旺" to "Yau Tsim Mong",
        "深水埗區" to "Sham Shui Po District",
        "九龍城區" to "Kowloon City District",
        "黃大仙區" to "Wong Tai Sin District",
        "觀塘區" to "Kwun Tong District",
        "葵青" to "Kwai Tsing",
        "葵青區" to "Kwai Tsing District",
        "荃灣" to "Tsuen Wan",
        "荃灣區" to "Tsuen Wan District",
        "屯門區" to "Tuen Mun District",
        "元朗" to "Yuen Long",
        "元朗區" to "Yuen Long District",
        "北區" to "North District",
        "大埔區" to "Tai Po District",
        "沙田區" to "Sha Tin District",
        "西貢區" to "Sai Kung District",
        "離島" to "Islands",
        "離島區" to "Islands District"
    )

    private val windStationAliasMap = mapOf(
        "沙田" to listOf("沙田"),
        "大埔" to listOf("大埔"),
        "西貢" to listOf("西貢"),
        "屯門" to listOf("屯門"),
        "將軍澳" to listOf("將軍澳"),
        "青衣" to listOf("青衣"),
        "赤鱲角" to listOf("赤鱲角"),
        "長洲" to listOf("長洲"),
        "流浮山" to listOf("流浮山"),
        "黃竹坑" to listOf("黃竹坑"),
        "坪洲" to listOf("坪洲"),
        "打鼓嶺" to listOf("打鼓嶺"),
        "昂坪" to listOf("昂坪"),
        "石崗" to listOf("石崗"),
        "大老山" to listOf("大老山"),
        "大帽山" to listOf("大帽山"),
        "京士柏" to listOf("京士柏", "九龍城", "黃大仙", "觀塘", "深水埗", "油尖旺"),
        "香港公園" to listOf("香港公園", "中西區", "灣仔", "跑馬地"),
        "筲箕灣" to listOf("筲箕灣", "東區"),
        "赤柱" to listOf("赤柱", "深水灣", "南區"),
        "啟德跑道公園" to listOf("啟德跑道公園", "啟德")
    )

    suspend fun getWeatherCacheState(): WeatherUiState? {
        return loadFromCache()
    }

    suspend fun fetchWeatherInfo(): WeatherUiState {
        return try {
            val rawRealtime = runCatching { hkoApiService.getRealtimeWeatherRaw() }.getOrNull()
            val rawToday = runCatching { hkoApiService.getTodayForecastRaw() }.getOrNull()
            val rawNineDay = runCatching { hkoApiService.getNineDayForecastRaw() }.getOrNull()
            val rawWarningSum = runCatching { hkoApiService.getWarningSummaryRaw() }.getOrNull()
            val rawWarningDetail = runCatching { hkoApiService.getWarningInfoRaw() }.getOrNull()
            val rawSwt = runCatching { hkoApiService.getSpecialWeatherTipsRaw() }.getOrNull()
            val rawWindCsv = runCatching { hkoApiService.getRegionalWindCsv().string() }.getOrNull()

            val windSpeedMap = parseWindCsv(rawWindCsv)

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

            if (rawSwt?.isJsonObject == true && rawSwt.asJsonObject.has("swt")) {
                try {
                    val swtList = mutableListOf<String>()
                    rawSwt.asJsonObject.getAsJsonArray("swt").forEach { elem ->
                        if (elem.isJsonObject) {
                            val obj = elem.asJsonObject
                            val desc = if (obj.has("desc")) obj.get("desc").asString else ""
                            if (desc.isNotBlank()) {
                                swtList.add(desc)
                            }
                        }
                    }
                    if (swtList.isNotEmpty()) {
                        warnings.add(
                            WeatherWarningItem(
                                code = "SWT",
                                name = "特別天氣提示",
                                details = swtList.joinToString("\n\n---\n\n"),
                                detailsList = swtList
                            )
                        )
                    }
                } catch (e: Exception) { Log.e("WeatherRepo", "SWT parse error", e) }
            }

            val districtList = mutableListOf<DistrictTemperature>()
            val rainfallList = mutableListOf<DistrictRainfall>()
            var globalHumidity: Int? = null
            var uvInfo: UvIndexInfo? = null

            if (rawRealtime?.isJsonObject == true) {
                val realObj = rawRealtime.asJsonObject

                try {
                    if (realObj.has("humidity") && realObj.getAsJsonObject("humidity").has("data")) {
                        val humArr = realObj.getAsJsonObject("humidity").getAsJsonArray("data")
                        if (humArr.size() > 0 && humArr.get(0).isJsonObject) {
                            globalHumidity = humArr.get(0).asJsonObject.get("value")?.asInt
                        }
                    }
                } catch (e: Exception) { Log.e("WeatherRepo", "Humidity error", e) }

                try {
                    if (realObj.has("uvindex") && realObj.getAsJsonObject("uvindex").has("data")) {
                        val uvData = realObj.getAsJsonObject("uvindex").get("data")
                        if (uvData.isJsonArray && uvData.asJsonArray.size() > 0) {
                            val uObj = uvData.asJsonArray.get(0).asJsonObject
                            val valStr = when {
                                !uObj.has("value") -> "0"
                                uObj.get("value").isJsonPrimitive -> uObj.get("value").asString
                                else -> "0"
                            }
                            val descStr = when {
                                uObj.has("desc") -> uObj.get("desc").asString
                                else -> "低"
                            }
                            uvInfo = UvIndexInfo(value = valStr, desc = descStr)
                        }
                    }
                } catch (e: Exception) { Log.e("WeatherRepo", "UV Index parse error", e) }

                if (uvInfo == null) {
                    uvInfo = UvIndexInfo(value = "0", desc = "低 (夜間或未有數據)")
                }

                try {
                    if (realObj.has("temperature") && realObj.getAsJsonObject("temperature").has("data")) {
                        realObj.getAsJsonObject("temperature").getAsJsonArray("data").forEach { elem ->
                            if (elem.isJsonObject) {
                                val item = elem.asJsonObject
                                val placeTc = if (item.has("place")) item.get("place").asString else ""
                                val tempVal = if (item.has("value")) item.get("value").asInt else 0
                                val placeEn = nameMap[placeTc] ?: placeTc

                                if (placeTc.isNotBlank()) {
                                    val windSpeedKmh = findWindSpeedForPlace(placeTc, windSpeedMap)
                                    val apparentTemp = calculateApparentTemperature(
                                        temperature = tempVal.toDouble(),
                                        relativeHumidity = (globalHumidity ?: 75).toDouble(),
                                        windSpeedKmPerHour = windSpeedKmh
                                    )

                                    districtList.add(
                                        DistrictTemperature(
                                            placeTc = placeTc,
                                            placeEn = placeEn,
                                            tempValue = tempVal,
                                            apparentTempValue = apparentTemp,
                                            humidityValue = globalHumidity
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) { Log.e("WeatherRepo", "Temp array error", e) }

                try {
                    if (realObj.has("rainfall") && realObj.getAsJsonObject("rainfall").has("data")) {
                        realObj.getAsJsonObject("rainfall").getAsJsonArray("data").forEach { elem ->
                            if (elem.isJsonObject) {
                                val item = elem.asJsonObject
                                val placeTc = if (item.has("place")) item.get("place").asString else ""
                                val maxVal = if (item.has("max")) item.get("max").asInt else 0
                                val minVal = if (item.has("min")) item.get("min").asInt else 0
                                val placeEn = nameMap[placeTc] ?: placeTc

                                if (placeTc.isNotBlank()) {
                                    rainfallList.add(
                                        DistrictRainfall(
                                            placeTc = placeTc,
                                            placeEn = placeEn,
                                            max = maxVal,
                                            min = minVal
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) { Log.e("WeatherRepo", "Rainfall array error", e) }
            }

            val sortedDistricts = districtList.sortedBy { it.placeEn }
            val sortedRainfall = rainfallList.sortedBy { it.placeEn }

            var todayDesc = ""
            var generalSituation = ""
            var outlook = ""
            var tcInfo = ""
            var fireDangerWarning = ""

            if (rawToday?.isJsonObject == true) {
                val tObj = rawToday.asJsonObject
                if (tObj.has("forecastDesc")) todayDesc = tObj.get("forecastDesc").asString
                if (tObj.has("generalSituation")) generalSituation = tObj.get("generalSituation").asString
                if (tObj.has("outlook")) outlook = tObj.get("outlook").asString
                if (tObj.has("tcInfo")) tcInfo = tObj.get("tcInfo").asString
                if (tObj.has("fireDangerWarning")) fireDangerWarning = tObj.get("fireDangerWarning").asString
            }

            val forecastList = mutableListOf<NineDayForecastItem>()
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

                            val windStr = if (f.has("forecastWind")) f.get("forecastWind").asString else ""

                            val minRh = f.getAsJsonObject("forecastMinrh")?.get("value")?.asInt ?: 0
                            val maxRh = f.getAsJsonObject("forecastMaxrh")?.get("value")?.asInt ?: 0

                            val psrStr = if (f.has("PSR")) f.get("PSR").asString else ""
                            val iconNum = if (f.has("ForecastIcon")) f.get("ForecastIcon").asInt else 0

                            if (date.isNotBlank()) {
                                forecastList.add(
                                    NineDayForecastItem(
                                        forecastDate = date,
                                        week = week,
                                        forecastWeather = weather,
                                        forecastMaxtemp = ForecastVal(value = maxTemp, unit = "°C"),
                                        forecastMintemp = ForecastVal(value = minTemp, unit = "°C"),
                                        forecastRh = ForecastRhRange(minrh = minRh, maxrh = maxRh, unit = "%"),
                                        psr = psrStr,
                                        wind = windStr,
                                        iconCode = iconNum
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) { Log.e("WeatherRepo", "Nine day error", e) }
            }

            val formattedTime = SimpleDateFormat("yyyyMMdd HH:mm:ss", Locale.getDefault()).format(Date())

            val newState = WeatherUiState(
                isLoading = false,
                warningSummary = warnings,
                districtTemperatures = sortedDistricts,
                districtRainfall = sortedRainfall,
                uvIndexInfo = uvInfo,
                todayForecast = todayDesc.ifBlank { "本港地區天氣情況良好。" },
                generalSituation = generalSituation,
                outlook = outlook,
                tcInfo = tcInfo,
                fireDangerWarning = fireDangerWarning,
                nineDayForecast = forecastList,
                updateTime = formattedTime,
                errorMessage = null
            )

            if (newState.updateTime.isNotEmpty()) {
                saveToCache(newState)
            }

            newState
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Fatal Fetch Error", e)
            WeatherUiState(
                isLoading = false,
                errorMessage = "加載天氣失敗: ${e.localizedMessage}"
            )
        }
    }

    private fun parseWindCsv(csvContent: String?): Map<String, Double> {
        if (csvContent.isNullOrBlank()) return emptyMap()
        val resultMap = mutableMapOf<String, Double>()
        try {
            val lines = csvContent.lines()
            for (i in 1 until lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty()) continue
                val cols = line.split(",").map { it.replace("\"", "").trim() }
                
                if (cols.size >= 4) {
                    val placeName = cols[1]
                    val speed = cols[3].toDoubleOrNull() ?: 0.0
                    if (placeName.isNotBlank()) {
                        resultMap[placeName] = speed
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Parse wind csv error", e)
        }
        return resultMap
    }

    private fun findWindSpeedForPlace(placeTc: String, windSpeedMap: Map<String, Double>): Double {
        if (windSpeedMap.containsKey(placeTc)) {
            return windSpeedMap[placeTc] ?: 0.0
        }
        for ((station, aliases) in windStationAliasMap) {
            if (aliases.contains(placeTc) && windSpeedMap.containsKey(station)) {
                return windSpeedMap[station] ?: 0.0
            }
        }
        return windSpeedMap.values.average().takeIf { !it.isNaN() } ?: 5.0
    }

    private fun calculateApparentTemperature(
        temperature: Double,
        relativeHumidity: Double,
        windSpeedKmPerHour: Double
    ): Double {
        val windSpeedMS = windSpeedKmPerHour / 3.6
        val e = (relativeHumidity / 100.0) * 6.105 * exp((17.27 * temperature) / (237.7 + temperature))
        val apparentTemp = temperature + (0.33 * e) - (0.70 * windSpeedMS) - 4.00
        return String.format(Locale.US, "%.1f", apparentTemp).toDouble()
    }
}
