package com.example.lifeapp.data.api

import com.example.lifeapp.data.model.*
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("weather.php")
    suspend fun getCurrentWeather(
        @Query("dataType") dataType: String = "rhrread",
        @Query("lang") lang: String = "tc"
    ): CurrentWeatherResponse

    @GET("weather.php")
    suspend fun getWarningInfo(
        @Query("dataType") dataType: String = "warnsum",
        @Query("lang") lang: String = "tc"
    ): WarningResponse

    @GET("weather.php")
    suspend fun getNineDayForecast(
        @Query("dataType") dataType: String = "fnd",
        @Query("lang") lang: String = "tc"
    ): NineDayForecastResponse
}

interface TrafficApiService {
    @GET("datagovhk/get-specially-traffic-news")
    suspend fun getTrafficNews(
        @Query("lang") lang: String = "tc"
    ): List<TrafficNewsItem>
}
