package com.example.lifeapp.data.api

import com.example.lifeapp.data.model.KmbEtaResponse
import com.example.lifeapp.data.model.KmbRouteResponse
import com.example.lifeapp.data.model.KmbRouteStopResponse
import com.example.lifeapp.data.model.KmbStopDetailResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface BusApiService {

    /**
     * 取得九巴所有路線列表
     */
    @GET("v1/transport/kmb/route")
    suspend fun getKmbRoutes(): KmbRouteResponse

    /**
     * 取得特定路線的站順列表
     */
    @GET("v1/transport/kmb/route-stop/{route}/{bound}/{service_type}")
    suspend fun getKmbRouteStops(
        @Path("route") route: String,
        @Path("bound") bound: String,
        @Path("service_type") serviceType: String
    ): KmbRouteStopResponse

    /**
     * 取得特定車站詳情 (站名與座標)
     */
    @GET("v1/transport/kmb/stop/{stop_id}")
    suspend fun getKmbStopDetail(
        @Path("stop_id") stopId: String
    ): KmbStopDetailResponse

    /**
     * 取得特定車站 + 路線的即時 ETA (預計到站時間)
     */
    @GET("v1/transport/kmb/eta/{stop_id}/{route}/{service_type}")
    suspend fun getKmbEta(
        @Path("stop_id") stopId: String,
        @Path("route") route: String,
        @Path("service_type") serviceType: String
    ): KmbEtaResponse
}
