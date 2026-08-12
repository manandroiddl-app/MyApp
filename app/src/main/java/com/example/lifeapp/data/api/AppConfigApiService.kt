package com.example.lifeapp.data.api

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Url

interface AppConfigApiService {
    @GET
    suspend fun getRemoteConfigRaw(@Url url: String): JsonObject
}
