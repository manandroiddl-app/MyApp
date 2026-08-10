package com.example.lifeapp.data.repository

import com.example.lifeapp.data.api.HkoApiService
import com.example.lifeapp.data.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val apiService: HkoApiService
) {
    suspend fun fetchFullWeatherData(): FullWeatherUiState = coroutineScope {
        val realtimeDeferred = async { apiService.getRealtimeWeather() }
        val warningDeferred = async { apiService.getWarningSummary() }
        val todayDeferred = async { apiService.getTodayForecast() }
        val nineDayDeferred = async { apiService.getNineDayForecast() }

        val realtime = realtimeDeferred.await()
        val warningsRaw = warningDeferred.await()
        val today = todayDeferred.await()
        val nineDay = nineDayDeferred.await()

        val warningList = mutableListOf<String>()
        warningsRaw?.values?.forEach { warningList.add(it.name) }
        if (warningList.isEmpty() && !realtime.warningMessage.isNullOrEmpty()) {
            warningList.addAll(realtime.warningMessage)
        }

        val locationList = mutableListOf<LocationStation>()
        realtime.temperature?.data?.forEach { rec ->
            val enName = stationNameEnMap[rec.place] ?: rec.place
            locationList.add(LocationStation(nameTc = rec.place, nameEn = enName, temp = rec.value))
        }
        locationList.sortBy { it.nameEn }

        val defaultLoc = locationList.firstOrNull { it.nameTc == "香港天文台" } ?: locationList.firstOrNull()
        val humidityVal = realtime.humidity?.data?.firstOrNull()?.let { "${it.value}%" } ?: "--%"
        val uvVal = realtime.uvindex?.data?.firstOrNull()?.let { "${it.value} (${it.desc})" } ?: "低 / 無數據"
        val todayDesc = today.forecastDesc ?: today.generalSituation ?: "暫無天氣預報資訊"
        val nineDays = nineDay.weatherForecast ?: emptyList()

        FullWeatherUiState(
            isLoading = false,
            warnings = warningList,
            locations = locationList,
            selectedLocation = defaultLoc,
            currentHumidity = humidityVal,
            currentUv = uvVal,
            todayForecastDesc = todayDesc,
            nineDayForecasts = nineDays
        )
    }
}
