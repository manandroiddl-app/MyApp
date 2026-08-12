package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.WeatherApiService
import com.example.lifeapp.data.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApiService: WeatherApiService
) {
    suspend fun fetchWeatherInfo(): WeatherUiState {
        return runCatching {
            val currRes = weatherApiService.getCurrentWeather()
            val warnRes = runCatching { weatherApiService.getWarningInfo() }.getOrNull()
            val nineRes = runCatching { weatherApiService.getNineDayForecast() }.getOrNull()

            // 解析警告訊息
            val warnText = warnRes?.details?.firstOrNull()?.contents?.firstOrNull() ?: ""

            // 明確指定 DistrictTemperature 型別，防止 Kotlin 編譯器推導失敗
            val districtTemps: List<DistrictTemperature> = currRes.temperature?.data?.map { node ->
                DistrictTemperature(
                    place = node.place ?: "",
                    value = node.value ?: 0,
                    unit = node.unit ?: "C"
                )
            } ?: emptyList()

            // 明確指定 ForecastItem 型別
            val forecasts: List<ForecastItem> = nineRes?.weatherForecast?.map { f ->
                ForecastItem(
                    forecastDate = f.forecastDate ?: "",
                    week = f.week ?: "",
                    forecastWeather = f.forecastWeather ?: "",
                    forecastMaxtemp = ForecastVal(f.forecastMaxtemp?.value ?: 0, f.forecastMaxtemp?.unit ?: "C"),
                    forecastMintemp = ForecastVal(f.forecastMintemp?.value ?: 0, f.forecastMintemp?.unit ?: "C"),
                    forecastMinRh = ForecastVal(f.forecastMinRh?.value ?: 0, f.forecastMinRh?.unit ?: "%"),
                    forecastMaxRh = ForecastVal(f.forecastMaxRh?.value ?: 0, f.forecastMaxRh?.unit ?: "%")
                )
            } ?: emptyList()

            val nowStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            WeatherUiState(
                isLoading = false,
                warningStatement = warnText,
                generalSituation = currRes.generalSituation ?: "",
                updateTime = currRes.updateTime ?: nowStr,
                districtTemperatures = districtTemps,
                nineDayForecast = forecasts,
                errorMessage = null
            )
        }.getOrElse { e ->
            Log.e("WeatherRepo", "Fetch weather error", e)
            WeatherUiState(
                isLoading = false,
                errorMessage = "無法獲取天氣資料，請再試一遍"
            )
        }
    }
}
