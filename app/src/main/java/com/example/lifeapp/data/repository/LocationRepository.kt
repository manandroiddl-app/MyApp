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

data class SpatialLocationResult(
    val matchedDistrict: String?,               
    val matchedSubDistrict: String?,             
    val nearestStreetName: String?,             
    val distanceToStreetMeters: Double?,         
    val nearbyLocations: List<LocationEntity>   
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
     * 🛡️ 包含 CSDI 404 容錯備援機制，確保下載流程 100% 成功
     */
    suspend fun downloadAndSyncAllLocations(): LocationSyncState {
        var totalCount = 0
        var syncedDistricts = false

        // 1. 同步 CSDI 18 區分界 (含 404 自動降級備援)
        try {
            val districtResponse = locationApiService.getCsdiDistricts()
            if (districtResponse.isSuccessful && districtResponse.body()?.features != null) {
                val features = districtResponse.body()!!.features!!
                val hierarchies = parseCsdiDistricts(features)
                if (hierarchies.isNotEmpty()) {
                    locationDao.upsertDistrictHierarchies(hierarchies)
                    syncedDistricts = true
                    Log.d("LocationRepository", "Synced ${hierarchies.size} CSDI district hierarchies from API.")
                }
            } else {
                Log.w("LocationRepository", "CSDI District API returned code: ${districtResponse.code()}, falling back to static map.")
            }
        } catch (e: Exception) {
            Log.e("LocationRepository", "CSDI District API failed, falling back to static map", e)
        }

        // 🛡️ 備援機制：如果 CSDI API 404，使用內建 HongKongDistricts 建立 18 區架構，絕不拋出 404 錯誤
        if (!syncedDistricts) {
            val fallbackHierarchies = generateFallbackDistrictHierarchies()
            locationDao.upsertDistrictHierarchies(fallbackHierarchies)
            Log.d("LocationRepository", "Generated ${fallbackHierarchies.size} fallback district hierarchies.")
        }

        // 2. 同步 CSDI 街道中線
        try {
            val roadResponse = locationApiService.getCsdiRoads()
            if (roadResponse.isSuccessful && roadResponse.body()?.features != null) {
                val roadEntities = parseCsdiRoads(roadResponse.body()!!.features!!)
                locationDao.upsertLocations(roadEntities)
                totalCount += roadEntities.size
                Log.d("LocationRepository", "Synced ${roadEntities.size} CSDI roads.")
            }
        } catch (e: Exception) {
            Log.e("LocationRepository", "CSDI Road sync failed, continuing...", e)
        }

        // 3. 同步 九巴全港站點
        try {
            val kmbResponse = locationApiService.getKmbStops()
            if (kmbResponse.isSuccessful && kmbResponse.body()?.data != null) {
                val rawStops: List<KmbStopDto> = kmbResponse.body()!!.data!!
                val kmbEntities = rawStops.mapNotNull { dto: KmbStopDto ->
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
        } catch (e: Exception) {
            Log.e("LocationRepository", "KMB Stop sync failed", e)
        }

        return if (totalCount > 0) {
            LocationSyncState.Success(totalCount, "成功同步 $totalCount 個地區及交通站點數據！")
        } else {
            LocationSyncState.Error("下載完成，但未有新增的站點數據。")
        }
    }

    suspend fun searchByName(query: String): List<LocationEntity> {
        if (query.isBlank()) return emptyList()
        return try {
            locationDao.searchLocations(query.trim())
        } catch (e: Exception) {
            Log.e("LocationRepository", "Search error", e)
            emptyList()
        }
    }

    suspend fun searchByCoordinates(lat: Double, lng: Double): SpatialLocationResult {
        return try {
            val hierarchies = locationDao.getAllDistrictHierarchies()
            var matchedDistrict: String? = null
            var matchedSubDistrict: String? = null

            for (h in hierarchies) {
                val polyGeoJson = h.districtPolygonGeoJson ?: continue
                val polygon = parsePolygonCoords(polyGeoJson) ?: continue
                if (GeoUtils.isPointInPolygon(lat, lng, polygon)) {
                    matchedDistrict = h.districtName
                    matchedSubDistrict = h.subDistrictName
                    break
                }
            }

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

    // --- Private Helper Parsers & Fallbacks ---

    private fun generateFallbackDistrictHierarchies(): List<DistrictHierarchyEntity> {
        var index = 100
        return HongKongDistricts.districtSubMap.flatMap { (districtName, subDistricts) ->
            val region = HongKongDistricts.getRegionByDistrict(districtName)
            subDistricts.map { subName ->
                index++
                DistrictHierarchyEntity(
                    id = "DIST_FALLBACK_$index",
                    regionName = region.label,
                    districtName = districtName,
                    districtLat = null,
                    districtLng = null,
                    districtPolygonGeoJson = null,
                    subDistrictName = subName,
                    subDistrictLat = null,
                    subDistrictLng = null
                )
            }
        }
    }

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
            val midPoint = lineCoords[midIndex]

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
