package com.example.lifeapp.data.api

import com.example.lifeapp.data.model.*
import retrofit2.http.GET
import retrofit2.http.Path

interface KmbApiService {

    // 1. 獲取所有路線
    @GET("v1/transport/kmb/route/")
    suspend fun getAllRoutes(): KmbDataResponse<List<KmbRoute>>

    // 2. 獲取路線站序
    @GET("v1/transport/kmb/route-stop/{route}/{bound}/{service_type}")
    suspend fun getRouteStops(
        @Path("route") route: String,
        @Path("bound") bound: String,
        @Path("service_type") serviceType: String
    ): KmbDataResponse<List<KmbRouteStop>>

    // 3. 獲取車站詳細資料
    @GET("v1/transport/kmb/stop/{stop_id}")
    suspend fun getStopDetail(
        @Path("stop_id") stopId: String
    ): KmbDataResponse<KmbStopDetail>

    // 4. 獲取車站特定路線 ETA
    @GET("v1/transport/kmb/eta/{stop_id}/{route}/{service_type}")
    suspend fun getStopEta(
        @Path("stop_id") stopId: String,
        @Path("route") route: String,
        @Path("service_type") serviceType: String
    ): KmbDataResponse<List<KmbEta>>
}
