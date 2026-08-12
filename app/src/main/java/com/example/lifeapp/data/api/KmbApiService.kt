package com.example.lifeapp.data.api

import com.example.lifeapp.data.model.*
import retrofit2.http.GET
import retrofit2.http.Path

interface KmbApiService {

    @GET("v1/transport/kmb/route/")
    suspend fun getAllRoutes(): KmbDataResponse<List<KmbRoute>>

    @GET("v1/transport/kmb/route-stop/{route}/{bound}/{service_type}")
    suspend fun getRouteStops(
        @Path("route") route: String,
        @Path("bound") bound: String,
        @Path("service_type") serviceType: String
    ): KmbDataResponse<List<KmbRouteStop>>

    @GET("v1/transport/kmb/stop/{stop_id}")
    suspend fun getStopDetail(
        @Path("stop_id") stopId: String
    ): KmbDataResponse<KmbStopDetail>

    @GET("v1/transport/kmb/eta/{stop_id}/{route}/{service_type}")
    suspend fun getStopEta(
        @Path("stop_id") stopId: String,
        @Path("route") route: String,
        @Path("service_type") serviceType: String
    ): KmbDataResponse<List<KmbEta>>

    // 🌟 車站車費 API 端點 (獲取路線各站收費)
    @GET("v1/transport/kmb/route-fare/{route}/{bound}/{service_type}")
    suspend fun getRouteFare(
        @Path("route") route: String,
        @Path("bound") bound: String,
        @Path("service_type") serviceType: String
    ): KmbDataResponse<List<KmbRouteFare>>
}
