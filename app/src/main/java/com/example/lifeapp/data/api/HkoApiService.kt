package com.example.lifeapp.data.api

import com.google.gson.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

interface HkoApiService {
    // 2. 分區天氣 (rhrread)
    @GET("weatherAPI/opendata/weather.php")
    suspend fun getRealtimeWeatherRaw(
        @Query("dataType") dataType: String = "rhrread",
        @Query("lang") lang: String = "tc"
    ): JsonElement

    // 1. 生效中警告摘要 (warnsum)
    @GET("weatherAPI/opendata/weather.php")
    suspend fun getWarningSummaryRaw(
        @Query("dataType") dataType: String = "warnsum",
        @Query("lang") lang: String = "tc"
    ): JsonElement

    // 1. 警告詳細資料 (warninginfo)
    @GET("weatherAPI/opendata/weather.php")
    suspend fun getWarningInfoRaw(
        @Query("dataType") dataType: String = "warninginfo",
        @Query("lang") lang: String = "tc"
    ): JsonElement

    // 1. 特別天氣提示 (swt)
    @GET("weatherAPI/opendata/weather.php")
    suspend fun getSpecialWeatherTipsRaw(
        @Query("dataType") dataType: String = "swt",
        @Query("lang") lang: String = "tc"
    ): JsonElement

    // 3. 今日天氣預報 (flw)
    @GET("weatherAPI/opendata/weather.php")
    suspend fun getTodayForecastRaw(
        @Query("dataType") dataType: String = "flw",
        @Query("lang") lang: String = "tc"
    ): JsonElement

    // 4. 九日天氣預報 (fnd)
    @GET("weatherAPI/opendata/weather.php")
    suspend fun getNineDayForecastRaw(
        @Query("dataType") dataType: String = "fnd",
        @Query("lang") lang: String = "tc"
    ): JsonElement
}
