package com.example.lifeapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Level 1: 大區
 */
enum class Region(val label: String) {
    HONG_KONG("港島"),
    KOWLOON("九龍"),
    NEW_TERRITORIES("新界")
}

/**
 * Level 4: 地點/站點類型
 */
enum class LocationType {
    BUS_STOP,     // 巴士站
    MTR_STATION,  // 鐵路站
    LANDMARK,     // 地標 / 景點
    BUILDING,     // 商場 / 屋苑
    STREET        // 街道
}

/**
 * Room 資料庫實體 (Level 1 ~ Level 4 + 經緯度)
 */
@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: String,              // 唯一 ID (例: "STOP_KMB_1234" 或 "LOC_998")
    val nameTc: String,                      // 中文名稱
    val nameEn: String,                      // 英文名稱
    val region: String,                      // L1: 大區 (HONG_KONG, KOWLOON, NEW_TERRITORIES)
    val district: String,                    // L2: 18 區 (例: "油尖旺區")
    val subDistrict: String,                 // L3: 次區份 (例: "旺角")
    val lat: Double,                         // 緯度
    val lng: Double,                         // 經度
    val type: String,                        // 類型 (LocationType name)
    val searchKeywords: String = "",         // 搜尋關鍵字 (以逗號分隔, 例: "MK,朗豪坊")
    val routes: String = ""                  // 行經路線 (以逗號分隔, 例: "1A,16,27")
)

/**
 * UI 使用的乾淨 Data Model
 */
data class LocationItem(
    val id: String,
    val nameTc: String,
    val nameEn: String,
    val region: Region,
    val district: String,
    val subDistrict: String,
    val lat: Double,
    val lng: Double,
    val type: LocationType,
    val searchKeywords: List<String> = emptyList(),
    val routes: List<String> = emptyList()
) {
    fun toEntity(): LocationEntity {
        return LocationEntity(
            id = id,
            nameTc = nameTc,
            nameEn = nameEn,
            region = region.name,
            district = district,
            subDistrict = subDistrict,
            lat = lat,
            lng = lng,
            type = type.name,
            searchKeywords = searchKeywords.joinToString(","),
            routes = routes.joinToString(",")
        )
    }

    companion object {
        fun fromEntity(entity: LocationEntity): LocationItem {
            return LocationItem(
                id = entity.id,
                nameTc = entity.nameTc,
                nameEn = entity.nameEn,
                region = try { Region.valueOf(entity.region) } catch (e: Exception) { Region.KOWLOON },
                district = entity.district,
                subDistrict = entity.subDistrict,
                lat = entity.lat,
                lng = entity.lng,
                type = try { LocationType.valueOf(entity.type) } catch (e: Exception) { LocationType.BUS_STOP },
                searchKeywords = if (entity.searchKeywords.isBlank()) emptyList() else entity.searchKeywords.split(","),
                routes = if (entity.routes.isBlank()) emptyList() else entity.routes.split(",")
            )
        }
    }
}
