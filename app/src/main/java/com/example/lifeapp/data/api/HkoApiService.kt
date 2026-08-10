package com.example.lifeapp.data.api

import com.example.lifeapp.data.model.ForecastLocalWeatherResponse
import com.example.lifeapp.data.model.NineDayForecastResponse
import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

interface HkoApiService {
    // 使用 JsonObject 接收 Raw JSON，徹底防止因格式微調導致的解析失敗
    @GET("weatherAPI/opendata/weather.php")
    suspend fun getRealtimeWeatherRaw(
        @Query("dataType") dataType: String = "rhrread",
        @Query("lang") lang: String = "tc"
    ): JsonObject

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
