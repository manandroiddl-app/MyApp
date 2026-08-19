package com.example.lifeapp.data.api

import com.example.lifeapp.data.model.CsdiDistrictProperties
import com.example.lifeapp.data.model.CsdiRoadProperties
import com.example.lifeapp.data.model.GeoJsonFeatureCollection
import com.example.lifeapp.data.model.KmbStopResponseDto
import retrofit2.Response
import retrofit2.http.GET

interface LocationApiService {

    // 1. CSDI 18 區行政分界 GeoJSON
    @GET("files/api/dataset/connect-spec/district_boundary.geojson")
    suspend fun getCsdiDistricts(): Response<GeoJsonFeatureCollection<CsdiDistrictProperties>>

    // 2. CSDI 街道中線 GeoJSON
    @GET("files/api/dataset/connect-spec/road_centreline.geojson")
    suspend fun getCsdiRoads(): Response<GeoJsonFeatureCollection<CsdiRoadProperties>>

    // 3. 九巴全港巴士站清單
    @GET("v1/transport/kmb/stop")
    suspend fun getKmbStops(): Response<KmbStopResponseDto>
}
