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
        var today: ForecastLocalWeatherResponse? = null
        var nineDay: NineDayForecastResponse? = null

        runCatching { realtimeRaw = apiService.getRealtimeWeatherRaw() }
            .onFailure { Log.e("WeatherRepo", "Realtime API Error", it) }

        runCatching { today = apiService.getTodayForecast() }
            .onFailure { Log.e("WeatherRepo", "Today API Error", it) }

        runCatching { nineDay = apiService.getNineDayForecast() }
            .onFailure { Log.e("WeatherRepo", "NineDay API Error", it) }

        val warningList = mutableListOf<String>()
        val locationList = mutableListOf<LocationStation>()
        var humidityVal = "--%"
        var uvVal = "無數據"
        var updateTimeText = "剛剛更新"

        // === 手動精密解析 rhrread JSON ===
        realtimeRaw?.let { root ->
            // 1. 解析生效中警告訊息
            if (root.has("warningMessage") && !root.get("warningMessage").isJsonNull) {
                val warnArray = root.getAsJsonArray("warningMessage")
                warnArray.forEach { element ->
                    val msg = element.asString.replace("<br/>", "\n").replace("<br>", "\n").trim()
                    if (msg.isNotEmpty()) warningList.add(msg)
                }
            }

            // 2. 解析分區氣溫 (全港測量站)
            if (root.has("temperature") && root.getAsJsonObject("temperature").has("data")) {
                val tempArray = root.getAsJsonObject("temperature").getAsJsonArray("data")
                tempArray.forEach { element ->
                    val obj = element.asJsonObject
                    val place = obj.get("place").asString
                    val value = obj.get("value").asInt
                    val enName = stationNameEnMap[place] ?: place
                    locationList.add(LocationStation(nameTc = place, nameEn = enName, temp = value))
                }
                locationList.sortBy { it.nameEn } // 依英文 Alphabetical A-Z 排序
            }

            // 3. 解析相對濕度
            if (root.has("humidity") && root.getAsJsonObject("humidity").has("data")) {
                val humiArray = root.getAsJsonObject("humidity").getAsJsonArray("data")
                if (humiArray.size() > 0) {
                    val humiVal = humiArray.get(0).asJsonObject.get("value").asInt
                    humidityVal = "$humiVal%"
                }
            }

            // 4. 解析紫外線指數
            if (root.has("uvindex") && !root.get("uvindex").isJsonNull) {
                val uvObj = root.getAsJsonObject("uvindex")
                if (uvObj.has("data")) {
                    val uvArray = uvObj.getAsJsonArray("data")
                    if (uvArray.size() > 0) {
                        val firstUv = uvArray.get(0).asJsonObject
                        val valNum = firstUv.get("value").asFloat
                        val desc = if (firstUv.has("desc")) firstUv.get("desc").asString else ""
                        uvVal = "$valNum ($desc)"
                    }
                }
            }

            // 5. 解析更新時間 (記錄時間)
            val rawTime = if (root.has("recordTime")) root.get("recordTime").asString
            else if (root.has("temperature") && root.getAsJsonObject("temperature").has("recordTime")) {
                root.getAsJsonObject("temperature").get("recordTime").asString
            } else ""

            if (rawTime.length >= 16) {
                try {
                    val timeSub = rawTime.substring(11, 16) // 截取 HH:mm
                    updateTimeText = "最後更新時間：$timeSub"
                } catch (e: Exception) {
                    updateTimeText = "剛剛更新"
                }
            }
        }

        // 預設選取「香港天文台」
        val defaultLoc = locationList.firstOrNull { it.nameTc == "香港天文台" }
            ?: locationList.firstOrNull()
            ?: LocationStation("香港天文台", "Hong Kong Observatory", null)

        val todayDesc = today?.forecastDesc ?: today?.generalSituation ?: "天文台現正更新天氣預報資訊。"
        val nineDays = nineDay?.weatherForecast ?: emptyList()

        FullWeatherUiState(
            isLoading = false,
            warnings = warningList,
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
