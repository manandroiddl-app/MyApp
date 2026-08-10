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

        // 1. 詳細警告資訊整理
        val warningList = mutableListOf<String>()
        // 優先顯示詳細 warningMessage
        if (!realtime?.warningMessage.isNullOrEmpty()) {
            realtime?.warningMessage?.let { warningList.addAll(it) }
        } else {
            warningsRaw?.values?.forEach { warningList.add(it.name) }
        }

        // 2. 分區氣溫 (按英文名 A-Z 排序)
        val locationList = mutableListOf<LocationStation>()
        realtime?.temperature?.data?.forEach { rec ->
            val enName = stationNameEnMap[rec.place] ?: rec.place
            locationList.add(LocationStation(nameTc = rec.place, nameEn = enName, temp = rec.value))
        }
        locationList.sortBy { it.nameEn }

        val defaultLoc = locationList.firstOrNull { it.nameTc == "香港天文台" } 
            ?: locationList.firstOrNull() 
            ?: LocationStation("香港天文台", "Hong Kong Observatory", null)

        val humidityVal = realtime?.humidity?.data?.firstOrNull()?.let { "${it.value}%" } ?: "--%"
        val uvVal = realtime?.uvindex?.data?.firstOrNull()?.let { "${it.value} (${it.desc})" } ?: "低 / 無數據"
        val todayDesc = today?.forecastDesc ?: today?.generalSituation ?: "天文台現正更新天氣預報資訊。"
        val nineDays = nineDay?.weatherForecast ?: emptyList()

        // 3. 格式化最後更新時間
        val rawTime = realtime?.updateTime ?: ""
        val formattedTime = if (rawTime.length >= 16) {
            try {
                // e.g. 2026-08-10T23:50:00+08:00 -> 23:50
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
            updateTimeText = if (formattedTime.isNotEmpty()) "最後更新時間：$formattedTime" else "",
            errorMessage = if (realtime == null && today == null) "無法連接香港天文台，請檢查網路連線。" else null
        )
    }
}
