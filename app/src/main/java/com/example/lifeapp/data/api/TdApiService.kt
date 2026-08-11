package com.example.lifeapp.data.api

import okhttp3.ResponseBody
import retrofit2.http.GET

interface TdApiService {
    @GET("td/tc/specialtrafficnews.xml")
    suspend fun getSpecialTrafficNewsRaw(): ResponseBody
}
