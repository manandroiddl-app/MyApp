package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.HkoApiService
import com.example.lifeapp.data.model.*
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val apiService: HkoApiService
) {
    suspend fun fetchFullWeatherData(): FullWeatherUiState = coroutineScope {
        var realtimeRaw: JsonObject? = null
        // 修正：宣告型態為 JsonElement?，對齊 HkoApiService
        var warnsumRaw: JsonElement? = null
        var warningInfoRaw: JsonElement? = null
        var swtRaw: JsonElement? = null
        var today: ForecastLocalWeatherResponse? = null
        var nineDay: NineDayForecastResponse? = null

        // 獨立抓取，單一 API 失敗不影響其他區塊
        runCatching { realtimeRaw = apiService.getRealtimeWeatherRaw() }
            .onFailure { Log.e("WeatherRepo", "rhrread Error", it) }

        runCatching { warnsumRaw = apiService.getWarningSummaryRaw() }
            .onFailure { Log.e("WeatherRepo", "warnsum Error", it) }

        runCatching { warningInfoRaw = apiService.getWarningInfoRaw() }
            .onFailure { Log.e("WeatherRepo", "warninginfo Error", it) }

        runCatching { swtRaw = apiService.getSpecialWeatherTipsRaw() }
            .onFailure { Log.e("WeatherRepo", "swt Error", it) }

        runCatching { today = apiService.getTodayForecast() }
            .onFailure { Log.e("WeatherRepo", "flw Error", it) }

        runCatching { nineDay = apiService.getNineDayForecast() }
            .onFailure { Log.e("WeatherRepo", "fnd Error", it) }

        // === 1. 安全解析生效中警告 (warnsum) ===
        val warningList = mutableListOf<String>()
        try {
            if (warnsumRaw != null && warnsumRaw!!.isJsonObject) {
                warnsumRaw!!.asJsonObject.entrySet().forEach { entry ->
                    val obj = entry.value.asJsonObject
                    if (obj.has("name")) {
                        warningList.add(obj.get("name").asString)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Parse warnsum failed", e)
        }

        // === 1. 安全解析警告詳細內文 (warninginfo) ===
        val warningDetailMap = mutableMapOf<String, String>()
        try {
            if (warningInfoRaw != null && warningInfoRaw!!.isJsonObject) {
                val infoObj = warningInfoRaw!!.asJsonObject
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

        // === 1. 安全解析特別天氣提示 (swt) ===
        val swtTips = mutableListOf<String>()
        try {
            if (swtRaw != null && swtRaw!!.isJsonObject) {
                val swtObj = swtRaw!!.asJsonObject
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

        // === 2. 安全解析分區天氣 (rhrread) ===
        val locationList = mutableListOf<LocationStation>()
        var humidityVal = "--%"
        var uvVal = "無數據"
        var updateTimeText = "剛剛更新"

        realtimeRaw?.let { root ->
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

            // 更新時間
            val rawTime = if (root.has("recordTime")) {
                root.get("recordTime").asString
            } else if (root.has("temperature") && root.getAsJsonObject("temperature").has("recordTime")) {
                root.getAsJsonObject("temperature").get("recordTime").asString
            } else ""

            if (rawTime.length >= 16) {
                try {
                    updateTimeText = "最後更新時間：${rawTime.substring(11, 16)}"
                } catch (e: Exception) {
                    updateTimeText = "剛剛更新"
                }
            }
        }

        val defaultLoc = locationList.firstOrNull { it.nameTc == "香港天文台" }
            ?: locationList.firstOrNull()
            ?: LocationStation("香港天文台", "Hong Kong Observatory", null)

        // 3. 今日預報 (flw)
        val todayDesc = today?.forecastDesc ?: today?.generalSituation ?: "天文台現正更新天氣預報資訊。"
        
        // 4. 九日預報 (fnd)
        val nineDays = nineDay?.weatherForecast ?: emptyList()

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
            errorMessage = if (realtimeRaw == null && today == null) "無法連接香港天文台，請檢查網路連線。" else null
        )
    }
}
