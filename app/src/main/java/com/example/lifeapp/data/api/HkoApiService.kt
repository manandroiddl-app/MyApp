package com.example.lifeapp.data.api

import com.example.lifeapp.data.model.*
import retrofit2.http.GET
import retrofit2.http.Query

interface HkoApiService {
    @GET("weatherAPI/opendata/weather.php")
    suspend fun getRealtimeWeather(
        @Query("dataType") dataType: String = "rhrread",
        @Query("lang") lang: String = "tc"
    ): RealtimeWeatherResponse

    @GET("weatherAPI/opendata/weather.php")
    suspend fun getWarningSummary(
        @Query("dataType") dataType: String = "warnsum",
        @Query("lang") lang: String = "tc"
    ): WarningSummaryResponse

    @GET("weatherAPI/opendata/weather.php")
    suspend fun getTodayForecast(
        @Query("dataType") dataType: String = "flw",
        @Query("lang") lang: String = "tc"
    ): ForecastLocalWeatherResponse

    @GET("weatherAPI/opendata/weather.php")
    suspend fun getNineDayForecast(
        @Query("dataType") dataType: String = "fnd",
        @Query("lang") lang: String = "tc"
    ): NineDayForecastResponse
}
