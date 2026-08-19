package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.local.HongKongDistricts
import com.example.lifeapp.data.local.LocationDao
import com.example.lifeapp.data.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class LocationRepository @Inject constructor(
    private val locationDao: LocationDao
) {

    /**
     * 首次啟動 Seed 種子資料初始化
     */
    suspend fun seedInitialLocationsIfEmpty() {
        try {
            val count = locationDao.getLocationCount()
            if (count == 0) {
                val seedList = getInitialSeedLocations()
                locationDao.upsertLocations(seedList.map { it.toEntity() })
                Log.d("LocationRepository", "Seeded ${seedList.size} initial locations into Room DB.")
            }
        } catch (e: Exception) {
            Log.e("LocationRepository", "Error seeding initial locations", e)
        }
    }

    /**
     * 關鍵字搜尋 (支援站名、次區份、18區、路線)
     */
    suspend fun searchLocations(query: String): List<LocationItem> {
        if (query.isBlank()) return emptyList()
        return try {
            locationDao.searchLocations(query.trim()).map { LocationItem.fromEntity(it) }
        } catch (e: Exception) {
            Log.e("LocationRepository", "Search error", e)
            emptyList()
        }
    }

    /**
     * 按 Region 篩選
     */
    suspend fun getLocationsByRegion(region: Region): List<LocationItem> {
        return try {
            locationDao.getLocationsByRegion(region.name).map { LocationItem.fromEntity(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 批量寫入/更新地點資料 (為 Phase 2 自動更新做準備)
     */
    suspend fun saveLocations(locations: List<LocationItem>) {
        try {
            locationDao.upsertLocations(locations.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e("LocationRepository", "Upsert error", e)
        }
    }

    /**
     * 經緯度距離計算 (Haversine Formula) - 回傳米 (m)
     */
    fun calculateDistanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0 // 地球半徑 (米)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * 內建預設種子數據 (地標、轉車站與主要鐵路站)
     */
    private fun getInitialSeedLocations(): List<LocationItem> {
        return listOf(
            LocationItem(
                id = "SEED_MTR_WTS",
                nameTc = "黃大仙站",
                nameEn = "Wong Tai Sin Station",
                region = Region.KOWLOON,
                district = "黃大仙區",
                subDistrict = "黃大仙",
                lat = 22.3416,
                lng = 114.1942,
                type = LocationType.MTR_STATION,
                searchKeywords = listOf("WTS", "黃大仙廟", "黃大仙中心"),
                routes = listOf("290", "38", "80", "89", "觀塘綫")
            ),
            LocationItem(
                id = "SEED_MTR_MK",
                nameTc = "旺角站",
                nameEn = "Mong Kok Station",
                region = Region.KOWLOON,
                district = "油尖旺區",
                subDistrict = "旺角",
                lat = 22.3193,
                lng = 114.1694,
                type = LocationType.MTR_STATION,
                searchKeywords = listOf("MK", "朗豪坊", "雅蘭中心"),
                routes = listOf("1A", "16", "27", "104", "荃灣綫", "觀塘綫")
            ),
            LocationItem(
                id = "SEED_BUS_TM_INT",
                nameTc = "屯門公路轉車站",
                nameEn = "Tuen Mun Road Interchange",
                region = Region.NEW_TERRITORIES,
                district = "屯門區",
                subDistrict = "掃管笏",
                lat = 22.3683,
                lng = 114.0135,
                type = LocationType.BUS_STOP,
                searchKeywords = listOf("屯轉", "轉車站"),
                routes = listOf("58X", "60X", "67X", "960", "961", "260X")
            ),
            LocationItem(
                id = "SEED_MTR_CEN",
                nameTc = "中環站",
                nameEn = "Central Station",
                region = Region.HONG_KONG,
                district = "中西區",
                subDistrict = "中環",
                lat = 22.2819,
                lng = 114.1581,
                type = LocationType.MTR_STATION,
                searchKeywords = listOf("Central", "置地廣場", "IFC"),
                routes = listOf("307", "601", "930", "港島綫", "荃灣綫")
            )
        )
    }
}
