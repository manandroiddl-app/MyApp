package com.example.lifeapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * 通用 GeoJSON FeatureCollection 封裝
 */
data class GeoJsonFeatureCollection<T>(
    @SerializedName("type") val type: String?,
    @SerializedName("features") val features: List<GeoJsonFeature<T>>?
)

data class GeoJsonFeature<T>(
    @SerializedName("type") val type: String?,
    @SerializedName("geometry") val geometry: GeoJsonGeometry?,
    @SerializedName("properties") val properties: T?
)

data class GeoJsonGeometry(
    @SerializedName("type") val type: String?,
    @SerializedName("coordinates") val rawCoordinates: Any?
)

/**
 * 1. 地政總署 街道中線 (Road Centreline) Properties
 */
data class CsdiRoadProperties(
    @SerializedName("OBJECTID") val objectId: Long,
    @SerializedName("STREETCODE") val streetCode: Long?,
    @SerializedName("STREETTYPE") val streetType: String?,
    @SerializedName("ENGLISHSTREETNAME") val englishStreetName: String?,
    @SerializedName("CHINESESTREETNAME") val chineseStreetName: String?
)

/**
 * 2. 地政總署 18 區行政分界 (District Boundary) Properties
 */
data class CsdiDistrictProperties(
    @SerializedName("OBJECTID") val objectId: Long,
    @SerializedName("AREA_CODE") val areaCode: String?,
    @SerializedName("NAME_TC") val nameTc: String?,
    @SerializedName("NAME_EN") val nameEn: String?
)

/**
 * 🗄️ 四層區域結構數據表 (district_hierarchy)
 */
@Entity(tableName = "district_hierarchy")
data class DistrictHierarchyEntity(
    @PrimaryKey val id: String,                  
    val regionName: String,                      
    val regionLat: Double? = null,               
    val regionLng: Double? = null,
    val districtName: String,                    
    val districtLat: Double? = null,             
    val districtLng: Double? = null,
    val districtPolygonGeoJson: String? = null,  
    val subDistrictName: String,                 
    val subDistrictLat: Double? = null,          
    val subDistrictLng: Double? = null
)

/**
 * 🗄️ 實體地點 / 街道 / 巴士站資料庫實體 (locations)
 * 🛡️ 核心修復：欄位已確立為 regionName, districtName, subDistrictName
 */
@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: String,                  // 唯一 ID (例如 "ROAD_CSDI_123" 或 "STOP_KMB_456")
    val nameTc: String,                          // 中文名稱
    val nameEn: String,                          // 英文名稱
    val type: String,                            // LocationType (STREET, BUS_STOP, MTR_STATION, LANDMARK)
    val regionName: String,                      // 所屬大區 (L1) -> "regionName"
    val districtName: String,                    // 所屬 18 區 (L2) -> "districtName"
    val subDistrictName: String,                 // 所屬次區份 (L3) -> "subDistrictName"
    val lat: Double,                             // 精準/中心點緯度
    val lng: Double,                             // 精準/中心點經度
    val polylineCoordsJson: String? = null,      // 若為 STREET，儲存 LineString 座標點陣列 JSON
    val searchKeywords: String = "",             // 搜尋關鍵字
    val routes: String = ""                      // 行經路線
)
