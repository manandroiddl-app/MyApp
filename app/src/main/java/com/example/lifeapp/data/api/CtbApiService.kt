package com.example.lifeapp.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

interface CtbApiService {

    @GET("v1/transport/citybus-nwfb/route/ctb")
    suspend fun getCtbAllRoutes(): CtbListResponse<CtbRouteDto>

    @GET("v1/transport/citybus-nwfb/route-stop/{company_id}/{route}/{direction}")
    suspend fun getCtbRouteStops(
        @Path("company_id") companyId: String = "CTB",
        @Path("route") route: String,
        @Path("direction") direction: String // "inbound" or "outbound"
    ): CtbListResponse<CtbRouteStopDto>

    @GET("v1/transport/citybus-nwfb/stop/{stop_id}")
    suspend fun getCtbStopInfo(
        @Path("stop_id") stopId: String
    ): CtbObjectResponse<CtbStopDto>

    @GET("v1/transport/citybus-nwfb/eta/{company_id}/{stop_id}/{route}")
    suspend fun getCtbEta(
        @Path("company_id") companyId: String = "CTB",
        @Path("stop_id") stopId: String,
        @Path("route") route: String
    ): CtbListResponse<CtbEtaDto>
}

data class CtbListResponse<T>(
    @SerializedName("type") val type: String?,
    @SerializedName("version") val version: String?,
    @SerializedName("generated_timestamp") val generatedTimestamp: String?,
    @SerializedName("data") val data: List<T>?
)

data class CtbObjectResponse<T>(
    @SerializedName("type") val type: String?,
    @SerializedName("version") val version: String?,
    @SerializedName("generated_timestamp") val generatedTimestamp: String?,
    @SerializedName("data") val data: T?
)

data class CtbRouteDto(
    @SerializedName("co") val company: String?,
    @SerializedName("route") val route: String?,
    @SerializedName("orig_tc") val origTc: String?,
    @SerializedName("orig_en") val origEn: String?,
    @SerializedName("dest_tc") val destTc: String?,
    @SerializedName("dest_en") val destEn: String?,
    @SerializedName("orig_sc") val origSc: String?,
    @SerializedName("dest_sc") val destSc: String?,
    @SerializedName("data_timestamp") val dataTimestamp: String?
)

data class CtbRouteStopDto(
    @SerializedName("co") val company: String?,
    @SerializedName("route") val route: String?,
    @SerializedName("dir") val dir: String?,
    @SerializedName("seq") val seq: Int?,
    @SerializedName("stop") val stopId: String?,
    @SerializedName("data_timestamp") val dataTimestamp: String?
)

data class CtbStopDto(
    @SerializedName("stop") val stopId: String?,
    @SerializedName("name_tc") val nameTc: String?,
    @SerializedName("name_en") val nameEn: String?,
    @SerializedName("lat") val lat: String?,
    @SerializedName("long") val long: String?,
    @SerializedName("name_sc") val nameSc: String?,
    @SerializedName("data_timestamp") val dataTimestamp: String?
)

data class CtbEtaDto(
    @SerializedName("co") val company: String?,
    @SerializedName("route") val route: String?,
    @SerializedName("dir") val dir: String?,
    @SerializedName("seq") val seq: Int?,
    @SerializedName("stop") val stopId: String?,
    @SerializedName("dest_tc") val destTc: String?,
    @SerializedName("dest_en") val destEn: String?,
    @SerializedName("eta") val eta: String?,
    @SerializedName("rmk_tc") val rmkTc: String?,
    @SerializedName("eta_seq") val etaSeq: Int?,
    @SerializedName("dest_sc") val destSc: String?,
    @SerializedName("rmk_en") val rmkEn: String?,
    @SerializedName("rmk_sc") val rmkSc: String?,
    @SerializedName("data_timestamp") val dataTimestamp: String?
)
