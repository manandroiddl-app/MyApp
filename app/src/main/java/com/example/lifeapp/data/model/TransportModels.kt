package com.example.lifeapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * 交通工具類型
 */
enum class TransitType {
    BUS,       // 巴士
    MINIBUS,   // 小巴
    MTR,       // 港鐵
    FERRY      // 渡輪
}

/**
 * 營運公司
 */
enum class OperatorCompany {
    KMB,       // 九巴
    CTB,       // 城巴
    NLB,       // 嶼巴
    GMB,       // 綠色專線小巴
    MTR,       // 港鐵
    FERRY      // 渡輪
}

/**
 * 統一路線 Domain Model (供 UI 顯示與搜尋使用)
 */
data class TransitRoute(
    val routeId: String,               // 路線識別碼 (如 "1A-O-1")
    val routeName: String,             // 路線編號 (如 "1A")
    val transitType: TransitType,      // 交通工具類型
    val company: OperatorCompany,     // 營運公司
    val originZh: String,              // 起點中文名
    val originEn: String?,             // 起點英文名
    val destinationZh: String,         // 終點中文名
    val destinationEn: String?,        // 終點英文名
    val bound: String? = null,         // 方向 ("I": Inbound, "O": Outbound)
    val serviceType: String? = null    // 服務類型 ("1": 常規)
)

/**
 * 路線車站 Domain Model
 */
data class TransitStop(
    val stopId: String,                // 車站 ID
    val sequence: Int,                 // 站序 (1, 2, 3...)
    val nameZh: String,                // 車站中文名
    val nameEn: String?,               // 車站英文名
    val latitude: Double,              // 緯度
    val longitude: Double              // 經度
)

/**
 * 到站時間 ETA Domain Model
 */
data class TransitEta(
    val routeName: String,
    val company: OperatorCompany,
    val destinationZh: String,
    val etaTimestamp: String?,         // ISO 8601 到站時間字串
    val remarkZh: String?,             // 備註 (例: "原定班次", "最後班次")
    val minutesLeft: Int? = null,      // 離到站剩餘分鐘數
    val etaSeq: Int? = null            // 到站班次序號 (由 API eta_seq 驅動)
)

// ==========================================
// 九巴 KMB Open Data Raw API DTOs
// ==========================================

data class KmbRouteResponse(
    @SerializedName("type") val type: String? = null,
    @SerializedName("version") val version: String? = null,
    @SerializedName("generated_timestamp") val generatedTimestamp: String? = null,
    @SerializedName("data") val data: List<KmbRouteDto>? = null
)

data class KmbRouteDto(
    @SerializedName("route") val route: String,
    @SerializedName("bound") val bound: String? = null,
    @SerializedName("service_type") val serviceType: String? = null,
    @SerializedName("orig_tc") val origTc: String? = null,
    @SerializedName("orig_en") val origEn: String? = null,
    @SerializedName("dest_tc") val destTc: String? = null,
    @SerializedName("dest_en") val destEn: String? = null
)

data class KmbRouteStopResponse(
    @SerializedName("type") val type: String? = null,
    @SerializedName("version") val version: String? = null,
    @SerializedName("generated_timestamp") val generatedTimestamp: String? = null,
    @SerializedName("data") val data: List<KmbRouteStopDto>? = null
)

data class KmbRouteStopDto(
    @SerializedName("route") val route: String,
    @SerializedName("bound") val bound: String,
    @SerializedName("service_type") val serviceType: String,
    @SerializedName("seq") val sequence: Int,
    @SerializedName("stop") val stopId: String
)

data class KmbStopDetailResponse(
    @SerializedName("type") val type: String? = null,
    @SerializedName("version") val version: String? = null,
    @SerializedName("generated_timestamp") val generatedTimestamp: String? = null,
    @SerializedName("data") val data: KmbStopDetailDto? = null
)

data class KmbStopDetailDto(
    @SerializedName("stop") val stopId: String,
    @SerializedName("name_tc") val nameTc: String,
    @SerializedName("name_en") val nameEn: String?,
    @SerializedName("lat") val lat: String?,
    @SerializedName("long") val long: String?
)

data class KmbEtaResponse(
    @SerializedName("type") val type: String? = null,
    @SerializedName("version") val version: String? = null,
    @SerializedName("generated_timestamp") val generatedTimestamp: String? = null,
    @SerializedName("data") val data: List<KmbEtaDto>? = null
)

data class KmbEtaDto(
    @SerializedName("co") val company: String? = null,
    @SerializedName("route") val route: String? = null,
    @SerializedName("dir") val dir: String? = null,
    @SerializedName("seq") val sequence: Int? = null,
    @SerializedName("eta_seq") val etaSeq: Int? = null,
    @SerializedName("stop") val stopId: String? = null,
    @SerializedName("dest_tc") val destTc: String? = null,
    @SerializedName("eta") val etaTimestamp: String? = null,
    @SerializedName("rmk_tc") val remarkTc: String? = null
)
