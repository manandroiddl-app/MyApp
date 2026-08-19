package com.example.lifeapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Level 1: 大區 Enum
 */
enum class Region(val label: String) {
    HONG_KONG("港島"),
    KOWLOON("九龍"),
    NEW_TERRITORIES("新界")
}

/**
 * Level 4: 地點/站點類型 Enum
 */
enum class LocationType {
    BUS_STOP,     // 巴士站
    MTR_STATION,  // 鐵路站
    LANDMARK,     // 地標 / 景點
    BUILDING,     // 商場 / 屋苑
    STREET        // 街道
}

/**
 * 九巴 API 回傳之 DTO (Data Transfer Object)
 */
data class KmbStopResponseDto(
    @SerializedName("type") val type: String?,
    @SerializedName("version") val version: String?,
    @SerializedName("generated_timestamp") val generatedTimestamp: String?,
    @SerializedName("data") val data: List<KmbStopDto>?
)

data class KmbStopDto(
    @SerializedName("stop") val stopId: String,
    @SerializedName("name_tc") val nameTc: String,
    @SerializedName("name_en") val nameEn: String,
    @SerializedName("lat") val lat: String,
    @SerializedName("long") val lng: String
)

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
 * 地政總署 街道中線 (Road Centreline) Properties
 */
data class CsdiRoadProperties(
    @SerializedName("OBJECTID") val objectId: Long,
    @SerializedName("STREETCODE") val streetCode: Long?,
    @SerializedName("STREETTYPE") val streetType: String?,
    @SerializedName("ENGLISHSTREETNAME") val englishStreetName: String?,
    @SerializedName("CHINESESTREETNAME") val chineseStreetName: String?
)

/**
 * 地政總署 18 區行政分界 (District Boundary) Properties
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
 */
@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: String,                  
    val nameTc: String,                          
    val nameEn: String,                          
    val type: String,                            
    val regionName: String,                      
    val districtName: String,                    
    val subDistrictName: String,                 
    val lat: Double,                             
    val lng: Double,                             
    val polylineCoordsJson: String? = null,      
    val searchKeywords: String = "",             
    val routes: String = ""                      
)
