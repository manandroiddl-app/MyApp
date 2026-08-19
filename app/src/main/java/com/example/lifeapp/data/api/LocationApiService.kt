package com.example.lifeapp.data.api

import com.example.lifeapp.data.model.CsdiDistrictProperties
import com.example.lifeapp.data.model.CsdiRoadProperties
import com.example.lifeapp.data.model.GeoJsonFeatureCollection
import com.example.lifeapp.data.model.KmbStopResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface LocationApiService {

    /**
     * 九巴全港站點 API
     */
    @GET("v1/transport/kmb/stop")
    suspend fun getKmbStops(): Response<KmbStopResponseDto>

    /**
     * CSDI 地政總署 - 街道中線 (Road Centreline) GeoJSON
     */
    @GET
    suspend fun getCsdiRoads(
        @Url url: String = "https://static.csdi.gov.hk/csdi-webpage/download/c7c82aae782b5fc5b1f8eb8fa8696428/geojson"
    ): Response<GeoJsonFeatureCollection<CsdiRoadProperties>>

    /**
     * CSDI 地方行政區分界 (18 區分界) GeoJSON
     */
    @GET
    suspend fun getCsdiDistricts(
        @Url url: String = "https://static.csdi.gov.hk/csdi-webpage/download/83cd933a39c7525581d6aa429a981c90/geojson"
    ): Response<GeoJsonFeatureCollection<CsdiDistrictProperties>>
}
