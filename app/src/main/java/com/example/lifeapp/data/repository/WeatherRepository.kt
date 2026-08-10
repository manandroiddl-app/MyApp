package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.HkoApiService
import com.example.lifeapp.data.model.*
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val apiService: HkoApiService
) {
    suspend fun fetchFullWeatherData(): FullWeatherUiState = coroutineScope {
        var realtime: RealtimeWeatherResponse? = null
        var warningsRaw: WarningSummaryResponse = null
        var today: ForecastLocalWeatherResponse? = null
        var nineDay: NineDayForecastResponse? = null

        runCatching { realtime = apiService.getRealtimeWeather() }
            .onFailure { Log.e("WeatherRepo", "Realtime API Error", it) }

        runCatching { warningsRaw = apiService.getWarningSummary() }
            .onFailure { Log.e("WeatherRepo", "Warning API Error", it) }

        runCatching { today = apiService.getTodayForecast() }
            .onFailure { Log.e("WeatherRepo", "Today API Error", it) }

        runCatching { nineDay = apiService.getNineDayForecast() }
            .onFailure { Log.e("WeatherRepo", "NineDay API Error", it) }

        // 1. 詳細天氣警告資訊（自動清除 HTML 標籤）
        val warningList = mutableListOf<String>()
        if (!realtime?.warningMessage.isNullOrEmpty()) {
            realtime?.warningMessage?.forEach { msg ->
                val cleanMsg = msg.replace("<br/>", "\n").replace("<br>", "\n").trim()
                if (cleanMsg.isNotEmpty()) warningList.add(cleanMsg)
            }
        }
        
        // 如果沒有 warningMessage，則使用 warnsum API 的名稱
        if (warningList.isEmpty() && warningsRaw != null) {
            warningsRaw?.values?.forEach { item ->
                if (item.name.isNotEmpty()) warningList.add(item.name)
            }
        }

        // 2. 爬取全香港 20+ 分區氣溫，並按英文名 (A-Z) 排序
        val locationList = mutableListOf<LocationStation>()
        realtime?.temperature?.data?.forEach { rec ->
            val enName = stationNameEnMap[rec.place] ?: rec.place
            locationList.add(LocationStation(nameTc = rec.place, nameEn = enName, temp = rec.value))
        }
        locationList.sortBy { it.nameEn }

        // 預設選取「香港天文台」
        val defaultLoc = locationList.firstOrNull { it.nameTc == "香港天文台" }
            ?: locationList.firstOrNull()

        // 3. 濕度與 UV
        val humidityVal = realtime?.humidity?.data?.firstOrNull()?.let { "${it.value}%" } ?: "--%"
        val uvVal = realtime?.uvindex?.data?.firstOrNull()?.let { "${it.value} (${it.desc})" } ?: "低 / 無數據"
        val todayDesc = today?.forecastDesc ?: today?.generalSituation ?: "天文台現正更新天氣預報資訊。"
        val nineDays = nineDay?.weatherForecast ?: emptyList()

        // 4. 解析真正的記錄時間 (recordTime: e.g. "2026-08-11T00:20:00+08:00")
        val rawTime = realtime?.recordTime ?: realtime?.temperature?.recordTime ?: ""
        val formattedTime = if (rawTime.length >= 16) {
            try {
                rawTime.substring(11, 16)
            } catch (e: Exception) {
                ""
            }
        } else ""

        FullWeatherUiState(
            isLoading = false,
            warnings = warningList,
            locations = locationList,
            selectedLocation = defaultLoc,
            currentHumidity = humidityVal,
            currentUv = uvVal,
            todayForecastDesc = todayDesc,
            nineDayForecasts = nineDays,
            updateTimeText = if (formattedTime.isNotEmpty()) "最後更新時間：$formattedTime" else "剛剛更新",
            errorMessage = if (realtime == null && today == null) "無法連接香港天文台，請檢查網路連線。" else null
        )
    }
}
