package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.LocationApiService
import com.example.lifeapp.data.common.GeoUtils
import com.example.lifeapp.data.local.HongKongDistricts
import com.example.lifeapp.data.local.LocationDao
import com.example.lifeapp.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 空間位置檢索結果封裝 (Approach 2 結果)
 */
data class SpatialLocationResult(
    val matchedDistrict: String?,                // 所屬 18 區 (Point-in-Polygon 比對結果)
    val matchedSubDistrict: String?,             // 推算之次區份
    val nearestStreetName: String?,              // 最近街道名稱 (Point-to-Line 比對結果)
    val distanceToStreetMeters: Double?,         // 距離該街道之距離 (米)
    val nearbyLocations: List<LocationEntity>   // 周邊 500 米內之巴士站或地標
)

sealed class LocationSyncState {
    object Idle : LocationSyncState()
    object Loading : LocationSyncState()
    data class Success(val count: Int, val message: String) : LocationSyncState()
    data class Error(val message: String) : LocationSyncState()
}

@Singleton
class LocationRepository @Inject constructor(
    private val locationDao: LocationDao,
    private val locationApiService: LocationApiService
) {
    private val gson = Gson()

    suspend fun getLocationCount(): Int {
        return try {
            locationDao.getLocationCount()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 🚀 下載與同步 CSDI (18區分界 + 街道) 與九巴全港站點
     */
    suspend fun downloadAndSyncAllLocations(): LocationSyncState {
        return try {
            var totalCount = 0

            // 1. 同步 CSDI 18 區分界
            val districtResponse = locationApiService.getCsdiDistricts()
            if (districtResponse.isSuccessful) {
                districtResponse.body()?.features?.let { features ->
                    val hierarchies = parseCsdiDistricts(features)
                    locationDao.upsertDistrictHierarchies(hierarchies)
                    Log.d("LocationRepository", "Synced ${hierarchies.size} district hierarchies.")
                }
            }

            // 2. 同步 CSDI 街道中線
            val roadResponse = locationApiService.getCsdiRoads()
            if (roadResponse.isSuccessful) {
                roadResponse.body()?.features?.let { features ->
                    val roadEntities = parseCsdiRoads(features)
                    locationDao.upsertLocations(roadEntities)
                    totalCount += roadEntities.size
                    Log.d("LocationRepository", "Synced ${roadEntities.size} CSDI roads.")
                }
            }

            // 3. 同步 九巴全港站點
            val kmbResponse = locationApiService.getKmbStops()
            if (kmbResponse.isSuccessful) {
                kmbResponse.body()?.data?.let { rawStops ->
                    val kmbEntities = rawStops.mapNotNull { dto ->
                        val lat = dto.lat.toDoubleOrNull() ?: return@mapNotNull null
                        val lng = dto.lng.toDoubleOrNull() ?: return@mapNotNull null
                        val (region, district, subDistrict) = HongKongDistricts.inferHierarchy(dto.nameTc)

                        LocationEntity(
                            id = "STOP_KMB_${dto.stopId}",
                            nameTc = dto.nameTc,
                            nameEn = dto.nameEn,
                            type = LocationType.BUS_STOP.name,
                            regionName = region.label,
                            districtName = district,
                            subDistrictName = subDistrict,
                            lat = lat,
                            lng = lng,
                            searchKeywords = "$subDistrict,${dto.nameEn}",
                            routes = ""
                        )
                    }
                    locationDao.upsertLocations(kmbEntities)
                    totalCount += kmbEntities.size
                    Log.d("LocationRepository", "Synced ${kmbEntities.size} KMB stops.")
                }
            }

            if (totalCount == 0) {
                LocationSyncState.Error("未有成功下載任何數據")
            } else {
                LocationSyncState.Success(totalCount, "成功同步 $totalCount 個地點、街道與站點！")
            }
        } catch (e: Exception) {
            Log.e("LocationRepository", "Error syncing data", e)
            LocationSyncState.Error(e.localizedMessage ?: "下載開放數據時發生網絡錯誤")
        }
    }

    /**
     * 🔍 Approach 1: 文字模糊搜尋 (名稱, 18區, 次區份, 關鍵字)
     */
    suspend fun searchByName(query: String): List<LocationEntity> {
        if (query.isBlank()) return emptyList()
        return try {
            locationDao.searchLocations(query.trim())
        } catch (e: Exception) {
            Log.e("LocationRepository", "Search error", e)
            emptyList()
        }
    }

    /**
     * 📍 Approach 2: 空間位置搜尋 (傳入用戶 GPS 座標, 反查 18區, 街道, 附近站點)
     */
    suspend fun searchByCoordinates(lat: Double, lng: Double): SpatialLocationResult {
        return try {
            val hierarchies = locationDao.getAllDistrictHierarchies()
            var matchedDistrict: String? = null
            var matchedSubDistrict: String? = null

            // Step 1: 利用 Point-in-Polygon 比對 18 區
            for (h in hierarchies) {
                val polyGeoJson = h.districtPolygonGeoJson ?: continue
                val polygon = parsePolygonCoords(polyGeoJson) ?: continue
                if (GeoUtils.isPointInPolygon(lat, lng, polygon)) {
                    matchedDistrict = h.districtName
                    matchedSubDistrict = h.subDistrictName
                    break
                }
            }

            // Step 2: 利用 Point-to-Line 比對最近街道
            val streets = locationDao.getAllStreets()
            var nearestStreet: String? = null
            var minStreetDistance = Double.MAX_VALUE

            for (street in streets) {
                val polylineJson = street.polylineCoordsJson ?: continue
                val lineCoords = parseLineStringCoords(polylineJson) ?: continue
                val dist = GeoUtils.minDistanceToPolylineMeters(lat, lng, lineCoords)
                if (dist < minStreetDistance) {
                    minStreetDistance = dist
                    nearestStreet = street.nameTc
                }
            }

            // Step 3: 找出周圍 500 米內所有站點
            val allLocations = locationDao.getAllLocations()
            val nearby = allLocations.filter {
                GeoUtils.calculateHaversineDistanceMeters(lat, lng, it.lat, it.lng) <= 500.0
            }

            SpatialLocationResult(
                matchedDistrict = matchedDistrict,
                matchedSubDistrict = matchedSubDistrict,
                nearestStreetName = nearestStreet,
                distanceToStreetMeters = if (minStreetDistance == Double.MAX_VALUE) null else minStreetDistance,
                nearbyLocations = nearby
            )
        } catch (e: Exception) {
            Log.e("LocationRepository", "Spatial search error", e)
            SpatialLocationResult(null, null, null, null, emptyList())
        }
    }

    // --- Private Helper Parsers ---

    private fun parseCsdiDistricts(features: List<GeoJsonFeature<CsdiDistrictProperties>>): List<DistrictHierarchyEntity> {
        val result = mutableListOf<DistrictHierarchyEntity>()
        for (f in features) {
            val props = f.properties ?: continue
            val distName = props.nameTc ?: continue
            val region = HongKongDistricts.getRegionByDistrict(distName)

            val rawCoords = f.geometry?.rawCoordinates
            val polygonCoords = extractFirstPolygonCoords(rawCoords)
            val centroid = polygonCoords?.let { GeoUtils.calculatePolygonCentroid(it) }

            result.add(
                DistrictHierarchyEntity(
                    id = "DIST_${props.objectId}",
                    regionName = region.label,
                    districtName = distName,
                    districtLat = centroid?.first,
                    districtLng = centroid?.second,
                    districtPolygonGeoJson = gson.toJson(rawCoords),
                    subDistrictName = distName.replace("區", ""),
                    subDistrictLat = centroid?.first,
                    subDistrictLng = centroid?.second
                )
            )
        }
        return result
    }

    private fun parseCsdiRoads(features: List<GeoJsonFeature<CsdiRoadProperties>>): List<LocationEntity> {
        val result = mutableListOf<LocationEntity>()
        for (f in features) {
            val props = f.properties ?: continue
            val nameTc = props.chineseStreetName
            if (nameTc.isNullOrBlank() || nameTc == "null") continue

            val lineCoords = extractLineStringCoords(f.geometry?.rawCoordinates) ?: continue
            val midIndex = lineCoords.size / 2
            val midPoint = lineCoords[midIndex] // (lng, lat)

            val (region, district, subDistrict) = HongKongDistricts.inferHierarchy(nameTc)

            result.add(
                LocationEntity(
                    id = "ROAD_CSDI_${props.objectId}",
                    nameTc = nameTc,
                    nameEn = props.englishStreetName ?: "",
                    type = LocationType.STREET.name,
                    regionName = region.label,
                    districtName = district,
                    subDistrictName = subDistrict,
                    lat = midPoint.second,
                    lng = midPoint.first,
                    polylineCoordsJson = gson.toJson(lineCoords),
                    searchKeywords = "$subDistrict,${props.englishStreetName ?: ""}",
                    routes = ""
                )
            )
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractFirstPolygonCoords(raw: Any?): List<Pair<Double, Double>>? {
        if (raw == null) return null
        return try {
            // Polygon: [[[lng, lat], ...]]
            val list = raw as? List<List<List<Double>>> ?: return null
            if (list.isEmpty()) return null
            list[0].mapNotNull { if (it.size >= 2) Pair(it[0], it[1]) else null }
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractLineStringCoords(raw: Any?): List<Pair<Double, Double>>? {
        if (raw == null) return null
        return try {
            // LineString: [[lng, lat], ...]
            val list = raw as? List<List<Double>> ?: return null
            list.mapNotNull { if (it.size >= 2) Pair(it[0], it[1]) else null }
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePolygonCoords(json: String): List<Pair<Double, Double>>? {
        val type = object : TypeToken<List<List<List<Double>>>>() {}.type
        val raw: List<List<List<Double>>> = gson.fromJson(json, type) ?: return null
        if (raw.isEmpty()) return null
        return raw[0].mapNotNull { if (it.size >= 2) Pair(it[0], it[1]) else null }
    }

    private fun parseLineStringCoords(json: String): List<Pair<Double, Double>>? {
        val type = object : TypeToken<List<List<Double>>>() {}.type
        val raw: List<List<Double>> = gson.fromJson(json, type) ?: return null
        return raw.mapNotNull { if (it.size >= 2) Pair(it[0], it[1]) else null }
    }
}
