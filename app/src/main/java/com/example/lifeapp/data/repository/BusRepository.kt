package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.BusApiService
import com.example.lifeapp.data.local.GenericCacheDao
import com.example.lifeapp.data.local.GenericCacheEntity
import com.example.lifeapp.data.local.dao.TransitBookmarkDao
import com.example.lifeapp.data.local.entity.TransitBookmarkEntity
import com.example.lifeapp.data.model.OperatorCompany
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop
import com.example.lifeapp.data.model.TransitType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusRepository @Inject constructor(
    private val busApiService: BusApiService,
    private val bookmarkDao: TransitBookmarkDao,
    private val genericCacheDao: GenericCacheDao,
    private val gson: Gson
) {

    suspend fun getKmbRoutes(): List<TransitRoute> {
        val cacheKey = "kmb_all_routes"
        val cached = getCacheList<TransitRoute>(cacheKey)
        if (!cached.isNullOrEmpty()) {
            return cached
        }

        return try {
            val response = busApiService.getKmbRoutes()
            val routes = response.data?.map { dto ->
                TransitRoute(
                    routeId = "${dto.route}-${dto.bound ?: ""}-${dto.serviceType ?: ""}",
                    routeName = dto.route,
                    transitType = TransitType.BUS,
                    company = OperatorCompany.KMB,
                    originZh = dto.origTc ?: "",
                    originEn = dto.origEn,
                    destinationZh = dto.destTc ?: "",
                    destinationEn = dto.destEn,
                    bound = dto.bound,
                    serviceType = dto.serviceType
                )
            } ?: emptyList()

            if (routes.isNotEmpty()) {
                saveCacheList(cacheKey, routes)
            }
            routes
        } catch (e: Exception) {
            Log.e("BusRepository", "Error fetching KMB routes", e)
            emptyList()
        }
    }

    suspend fun getKmbRouteStops(route: String, bound: String, serviceType: String): List<TransitStop> {
        val dirParam = if (bound.equals("O", ignoreCase = true)) "outbound" else "inbound"
        val cacheKey = "kmb_stops_${route}_${dirParam}_$serviceType"
        val cached = getCacheList<TransitStop>(cacheKey)
        
        if (!cached.isNullOrEmpty()) {
            return cached
        }

        return try {
            val response = busApiService.getKmbRouteStops(route, dirParam, serviceType)
            val stops = response.data?.map { dto ->
                TransitStop(
                    stopId = dto.stopId,
                    sequence = dto.sequence,
                    nameZh = "載入中...",
                    nameEn = null,
                    latitude = 0.0,
                    longitude = 0.0
                )
            } ?: emptyList()

            if (stops.isNotEmpty()) {
                saveCacheList(cacheKey, stops)
            }
            stops
        } catch (e: Exception) {
            Log.e("BusRepository", "Error fetching route stops for $route", e)
            emptyList()
        }
    }

    suspend fun getKmbEta(stopId: String, route: String, serviceType: String): List<TransitEta> {
        return try {
            val response = busApiService.getKmbEta(stopId, route, serviceType)
            val now = ZonedDateTime.now()
            response.data?.map { dto ->
                val minutesLeft = calculateMinutesLeft(dto.etaTimestamp, now)
                TransitEta(
                    routeName = dto.route ?: route,
                    company = OperatorCompany.KMB,
                    destinationZh = dto.destTc ?: "",
                    etaTimestamp = dto.etaTimestamp,
                    remarkZh = dto.remarkTc,
                    minutesLeft = minutesLeft
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("BusRepository", "Error fetching ETA for stop $stopId", e)
            emptyList()
        }
    }

    fun getAllBookmarks(): Flow<List<TransitBookmarkEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun addBookmark(bookmark: TransitBookmarkEntity) {
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun removeBookmark(bookmarkId: String) {
        bookmarkDao.deleteBookmarkById(bookmarkId)
    }

    private suspend inline fun <reified T> getCacheList(key: String): List<T>? {
        return try {
            val entity = genericCacheDao.getCache(key)
            if (entity != null && entity.jsonContent.isNotBlank()) {
                val type = object : TypeToken<List<T>>() {}.type
                gson.fromJson<List<T>>(entity.jsonContent, type)
            } else null
        } catch (e: Exception) {
            Log.e("BusRepository", "Error reading cache for key: $key", e)
            null
        }
    }

    private suspend fun <T> saveCacheList(key: String, data: List<T>) {
        try {
            val jsonStr = gson.toJson(data)
            genericCacheDao.saveCache(
                GenericCacheEntity(cacheKey = key, jsonContent = jsonStr)
            )
        } catch (e: Exception) {
            Log.e("BusRepository", "Error saving cache for key: $key", e)
        }
    }

    private fun calculateMinutesLeft(etaStr: String?, now: ZonedDateTime): Int? {
        if (etaStr.isNullOrEmpty()) return null
        return try {
            val etaTime = ZonedDateTime.parse(etaStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val diff = ChronoUnit.MINUTES.between(now, etaTime).toInt()
            if (diff < 0) 0 else diff
        } catch (e: Exception) {
            null
        }
    }
}
