package com.example.lifeapp.data.api

import com.example.lifeapp.data.model.HkoFndResponse
import com.example.lifeapp.data.model.HkoRhrreadResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface HkoApiService {

    @GET("weather.php")
    suspend fun getRhrread(
        @Query("dataType") dataType: String = "rhrread",
        @Query("lang") lang: String = "tc"
    ): HkoRhrreadResponse

    @GET("weather.php")
    suspend fun getFnd(
        @Query("dataType") dataType: String = "fnd",
        @Query("lang") lang: String = "tc"
    ): HkoFndResponse
}
