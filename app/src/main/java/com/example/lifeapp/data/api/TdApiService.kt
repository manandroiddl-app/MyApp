package com.example.lifeapp.data.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

interface TdApiService {
    // 🛡️ 繁體中文特別交通消息官方 XML Endpoint
    @GET
    suspend fun getSpecialTrafficNewsXml(
        @Url url: String = "https://resource.data.one.gov.hk/td/tc/specialtrafficnews.xml"
    ): ResponseBody
}
