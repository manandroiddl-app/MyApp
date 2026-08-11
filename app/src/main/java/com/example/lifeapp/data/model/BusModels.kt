package com.example.lifeapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

// === 九巴 API 網絡回應模型 ===
data class KmbDataResponse<T>(
    @SerializedName("type") val type: String?,
    @SerializedName("version") val version: String?,
    @SerializedName("generated_timestamp") val generatedTimestamp: String?,
    @SerializedName("data") val data: T?
)

data class KmbRoute(
    @SerializedName("route") val route: String,
    @SerializedName("bound") val bound: String, // "I" 或 "O"
    @SerializedName("service_type") val serviceType: String,
    @SerializedName("orig_tc") val origTc: String,
    @SerializedName("dest_tc") val destTc: String
)

data class KmbRouteStop(
    @SerializedName("route") val route: String,
    @SerializedName("bound") val bound: String,
    @SerializedName("service_type") val serviceType: String,
    @SerializedName("seq") val seq: String,
    @SerializedName("stop") val stopId: String
)

data class KmbStopDetail(
    @SerializedName("stop") val stopId: String,
    @SerializedName("name_tc") val nameTc: String,
    @SerializedName("lat") val lat: String?,
    @SerializedName("long") val long: String?
)

data class KmbEta(
    @SerializedName("co") val company: String?,
    @SerializedName("route") val route: String,
    @SerializedName("dir") val dir: String?,
    @SerializedName("service_type") val serviceType: Int?,
    @SerializedName("seq") val seq: Int?,
    @SerializedName("dest_tc") val destTc: String?,
    @SerializedName("eta") val eta: String?,
    @SerializedName("rmk_tc") val rmkTc: String?
)

// === 本地資料庫實體：Bookmark 收藏 ===
@Entity(tableName = "bus_bookmarks")
data class BusBookmarkEntity(
    @PrimaryKey val id: String, // route_stopId_bound
    val transportCompany: String = "KMB",
    val route: String,
    val bound: String,
    val serviceType: String,
    val stopId: String,
    val stopNameTc: String,
    val destTc: String,
    val createTime: Long = System.currentTimeMillis()
)

// === UI 顯示模型 ===
data class BusEtaUiItem(
    val bookmarkId: String,
    val route: String,
    val stopName: String,
    val destName: String,
    val etaText: String,
    val remark: String
)

data class BusSearchUiState(
    val isLoading: Boolean = false,
    val routeList: List<KmbRoute> = emptyList(),
    val filteredRoutes: List<KmbRoute> = emptyList(),
    val selectedRoute: KmbRoute? = null,
    val stopList: List<Pair<KmbRouteStop, KmbStopDetail>> = emptyList(),
    val searchQuery: String = "",
    val searchType: BusSearchType = BusSearchType.BY_ROUTE,
    val errorMessage: String? = null
)

enum class BusSearchType {
    BY_ROUTE, BY_STOP
}

data class BusBookmarkUiState(
    val isLoading: Boolean = false,
    val bookmarks: List<BusBookmarkEntity> = emptyList(),
    val etaMap: Map<String, List<BusEtaUiItem>> = emptyMap(), // bookmarkId -> ETA清單
    val lastUpdatedText: String = ""
)
