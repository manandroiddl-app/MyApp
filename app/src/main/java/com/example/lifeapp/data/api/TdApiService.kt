package com.example.lifeapp.data.api

import com.example.lifeapp.data.model.SpecialTrafficNewsResponse
import retrofit2.http.GET

interface TdApiService {
    @GET("td/en/specialtrafficnews.xml")
    suspend fun getSpecialTrafficNews(): SpecialTrafficNewsResponse
}
