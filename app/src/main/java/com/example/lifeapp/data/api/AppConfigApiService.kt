package com.example.lifeapp.data.api

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Url

interface AppConfigApiService {
    // 加入強制破快取 Header，讓 GitHub CDN 即時回傳最新 JSON
    @Headers(
        "Cache-Control: no-cache, no-store, must-revalidate",
        "Pragma: no-cache",
        "Expires: 0"
    )
    @GET
    suspend fun getRemoteConfigRaw(@Url url: String): JsonObject
}
