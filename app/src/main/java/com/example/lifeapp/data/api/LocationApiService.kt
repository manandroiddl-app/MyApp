package com.example.lifeapp.data.api

import com.example.lifeapp.data.model.CsdiDistrictProperties
import com.example.lifeapp.data.model.CsdiRoadProperties
import com.example.lifeapp.data.model.GeoJsonFeatureCollection
import com.example.lifeapp.data.model.KmbStopResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface LocationApiService {

    // 1. CSDI 18 區行政分界 GeoJSON (支援傳入指定或備援 Url)
    @GET
    suspend fun getCsdiDistricts(
        @Url url: String = "https://api.csdi.gov.hk/files/api/dataset/connect-spec/district_boundary.geojson"
    ): Response<GeoJsonFeatureCollection<CsdiDistrictProperties>>

    // 2. CSDI 街道中線 GeoJSON
    @GET
    suspend fun getCsdiRoads(
        @Url url: String = "https://api.csdi.gov.hk/files/api/dataset/connect-spec/road_centreline.geojson"
    ): Response<GeoJsonFeatureCollection<CsdiRoadProperties>>

    // 3. 九巴全港巴士站清單
    @GET("v1/transport/kmb/stop")
    suspend fun getKmbStops(): Response<KmbStopResponseDto>
}
