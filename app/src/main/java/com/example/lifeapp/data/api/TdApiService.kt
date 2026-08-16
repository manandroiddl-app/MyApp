package com.example.lifeapp.data.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

interface TdApiService {
    // 🛡️ 核心修復：使用 @Url 覆蓋預設 Base URL，直接請求官方 XML 數據，徹底解決 HTTP 404 問題
    @GET
    suspend fun getSpecialTrafficNewsXml(
        @Url url: String = "https://resource.data.one.gov.hk/td/tc/specialtrafficnews.xml"
    ): ResponseBody
}
