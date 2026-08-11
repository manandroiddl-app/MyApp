package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.HkoApiService
import com.example.lifeapp.data.model.*
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
        var warnsumRaw: JsonObject? = null
        var warningInfoRaw: JsonObject? = null
        var swtRaw: JsonObject? = null
        var today: ForecastLocalWeatherResponse? = null
        var nineDay: NineDayForecastResponse? = null

        // 平行請求 API
        runCatching { realtimeRaw = apiService.getRealtimeWeatherRaw() }
            .onFailure { Log.e("WeatherRepo", "rhrread API Error", it) }

        runCatching { warnsumRaw = apiService.getWarningSummaryRaw() }
            .onFailure { Log.e("WeatherRepo", "warnsum API Error", it) }

        runCatching { warningInfoRaw = apiService.getWarningInfoRaw() }
            .onFailure { Log.e("WeatherRepo", "warninginfo API Error", it) }

        runCatching { swtRaw = apiService.getSpecialWeatherTipsRaw() }
            .onFailure { Log.e("WeatherRepo", "swt API Error", it) }

        runCatching { today = apiService.getTodayForecast() }
            .onFailure { Log.e("WeatherRepo", "flw API Error", it) }

        runCatching { nineDay = apiService.getNineDayForecast() }
            .onFailure { Log.e("WeatherRepo", "fnd API Error", it) }

        // === 1. 解析生效中警告 (warnsum) ===
        val warningList = mutableListOf<String>()
        warnsumRaw?.entrySet()?.forEach { entry ->
            val obj = entry.value.asJsonObject
            if (obj.has("name")) {
                warningList.add(obj.get("name").asString)
            }
        }

        // === 1. 解析警告詳細內文 (warninginfo) ===
        val warningDetailMap = mutableMapOf<String, String>()
        warningInfoRaw?.run {
            if (has("details")) {
                val detailsArray = getAsJsonArray("details")
                for (i in 0 until detailsArray.size()) {
                    val item = detailsArray.get(i).asJsonObject
                    val name = if (item.has("warningStatementCode")) item.get("warningStatementCode").asString else "警告詳細資料"
                    val contents = mutableListOf<String>()
                    if (item.has("contents")) {
                        val contentsArray = item.getAsJsonArray("contents")
                        for (j in 0 until contentsArray.size()) {
                            contents.add(contentsArray.get(j).asString)
                        }
                    }
                    warningDetailMap[name] = contents.joinToString("\n\n")
                }
            }
        }

        // === 1. 解析特別天氣提示 (swt) ===
        val swtTips = mutableListOf<String>()
        swtRaw?.run {
            if (has("swt")) {
                val swtArray = getAsJsonArray("swt")
                for (i in 0 until swtArray.size()) {
                    val item = swtArray.get(i).asJsonObject
                    if (item.has("desc")) {
                        swtTips.add(item.get("desc").asString)
                    }
                }
            }
        }

        // === 2. 解析分區天氣 (rhrread) ===
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

            // 濕度
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

            // UV 指數
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
