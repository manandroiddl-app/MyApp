package com.example.lifeapp.data.api

import com.google.gson.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

interface HkoApiService {
    // 1. 生效中警告摘要 (warnsum)
    @GET("weatherAPI/opendata/weather.php")
    suspend fun getWarningSummaryRaw(
        @Query("dataType") dataType: String = "warnsum",
        @Query("lang") lang: String = "tc"
    ): JsonElement

    // 2. 特別天氣提示 (swt)
    @GET("weatherAPI/opendata/weather.php")
    suspend fun getSpecialWeatherTipsRaw(
        @Query("dataType") dataType: String = "swt",
        @Query("lang") lang: String = "tc"
    ): JsonElement

    // 3. 詳細警告訊息 (warningInfo)
    @GET("weatherAPI/opendata/weather.php")
    suspend fun getWarningInfoRaw(
        @Query("dataType") dataType: String = "warningInfo",
        @Query("lang") lang: String = "tc"
    ): JsonElement

    // 4. 分區天氣/濕度/UV (rhrread)
    @GET("weatherAPI/opendata/weather.php")
    suspend fun getRealtimeWeatherRaw(
        @Query("dataType") dataType: String = "rhrread",
        @Query("lang") lang: String = "tc"
    ): JsonElement

    // 5. 今日天氣預報 (flw)
    @GET("weatherAPI/opendata/weather.php")
    suspend fun getTodayForecastRaw(
        @Query("dataType") dataType: String = "flw",
        @Query("lang") lang: String = "tc"
    ): JsonElement

    // 6. 九日天氣預報 (fnd)
    @GET("weatherAPI/opendata/weather.php")
    suspend fun getNineDayForecastRaw(
        @Query("dataType") dataType: String = "fnd",
        @Query("lang") lang: String = "tc"
    ): JsonElement
}
