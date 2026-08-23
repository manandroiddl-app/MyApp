package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.BusApiService
import com.example.lifeapp.data.local.GenericCacheDao
import com.example.lifeapp.data.local.GenericCacheEntity
import com.example.lifeapp.data.local.dao.TransitBookmarkDao
import com.example.lifeapp.data.local.entity.TransitBookmarkEntity
import com.example.lifeapp.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    // 🎯修復「載入中...」：使用 async 並行查詢每個 stopId 的中文站名
    suspend fun getKmbRouteStops(route: String, bound: String, serviceType: String): List<TransitStop> = coroutineScope {
        val dirParam = if (bound.equals("O", ignoreCase = true)) "outbound" else "inbound"
        val cacheKey = "kmb_stops_${route}_${dirParam}_$serviceType"
        val cached = getCacheList<TransitStop>(cacheKey)
        
        if (!cached.isNullOrEmpty()) {
            return@coroutineScope cached
        }

        try {
            val response = busApiService.getKmbRouteStops(route, dirParam, serviceType)
            val dtoList = response.data ?: emptyList()

            val deferredStops = dtoList.map { dto ->
                async {
                    val detail = getStopDetail(dto.stopId)
                    TransitStop(
                        stopId = dto.stopId,
                        sequence = dto.sequence,
                        nameZh = detail?.nameTc ?: "車站 ${dto.sequence}",
                        nameEn = detail?.nameEn,
                        latitude = detail?.lat?.toDoubleOrNull() ?: 0.0,
                        longitude = detail?.long?.toDoubleOrNull() ?: 0.0
                    )
                }
            }

            val stops = deferredStops.awaitAll()

            if (stops.isNotEmpty()) {
                saveCacheList(cacheKey, stops)
            }
            stops
        } catch (e: Exception) {
            Log.e("BusRepository", "Error fetching route stops for $route", e)
            emptyList()
        }
    }

    private suspend fun getStopDetail(stopId: String): KmbStopDetailDto? {
        val cacheKey = "kmb_stop_detail_$stopId"
        val cached = getCacheObject<KmbStopDetailDto>(cacheKey)
        if (cached != null) return cached

        return try {
            val res = busApiService.getKmbStopDetail(stopId)
            res.data?.also {
                saveCacheObject(cacheKey, it)
            }
        } catch (e: Exception) {
            null
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
        } catch (e: Exception) { null }
    }

    private suspend fun <T> saveCacheList(key: String, data: List<T>) {
        try {
            val jsonStr = gson.toJson(data)
            genericCacheDao.saveCache(GenericCacheEntity(cacheKey = key, jsonContent = jsonStr))
        } catch (e: Exception) { }
    }

    private suspend inline fun <reified T> getCacheObject(key: String): T? {
        return try {
            val entity = genericCacheDao.getCache(key)
            if (entity != null && entity.jsonContent.isNotBlank()) {
                gson.fromJson(entity.jsonContent, T::class.java)
            } else null
        } catch (e: Exception) { null }
    }

    private suspend fun <T> saveCacheObject(key: String, data: T) {
        try {
            val jsonStr = gson.toJson(data)
            genericCacheDao.saveCache(GenericCacheEntity(cacheKey = key, jsonContent = jsonStr))
        } catch (e: Exception) { }
    }

    private fun calculateMinutesLeft(etaStr: String?, now: ZonedDateTime): Int? {
        if (etaStr.isNullOrEmpty()) return null
        return try {
            val etaTime = ZonedDateTime.parse(etaStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val diff = ChronoUnit.MINUTES.between(now, etaTime).toInt()
            if (diff < 0) 0 else diff
        } catch (e: Exception) { null }
    }
}
