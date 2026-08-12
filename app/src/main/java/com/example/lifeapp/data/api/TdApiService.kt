package com.example.lifeapp.data.api

import com.google.gson.JsonElement
import retrofit2.http.GET

interface TdApiService {
    @GET("v1/sc/special-traffic-news")
    suspend fun getSpecialTrafficNewsRaw(): JsonElement
}
