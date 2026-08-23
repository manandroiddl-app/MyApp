package com.example.lifeapp.data.repository

import com.example.lifeapp.data.api.BusApiService
import com.example.lifeapp.data.local.GenericCacheDao
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
    cacheDao: GenericCacheDao,
    gson: Gson
) : BaseCacheRepository(cacheDao, gson) {

    suspend fun getAllRoutes(): List<TransitRoute> {
        val cacheKey = "kmb_all_routes"
        val cached = getCache(cacheKey)
        if (!cached.isNullOrEmpty()) {
            val type = object : TypeToken<List<TransitRoute>>() {}.type
            return gson.fromJson(cached, type)
        }

        return try {
            val response = busApiService.getAllRoutes()
            val routes = response.data.map { dto ->
                TransitRoute(
                    routeId = "${dto.route}-${dto.bound}-${dto.serviceType}",
                    routeName = dto.route,
                    transitType = TransitType.BUS,
                    company = OperatorCompany.KMB,
                    originZh = dto.origTc,
                    originEn = dto.origEn,
                    destinationZh = dto.destTc,
                    destinationEn = dto.destEn,
                    bound = dto.bound,
                    serviceType = dto.serviceType
                )
            }
            val type = object : TypeToken<List<TransitRoute>>() {}.type
            saveCache(cacheKey, gson.toJson(routes, type), ttlMinutes = 1440)
            routes
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRouteStops(routeName: String, direction: String, serviceType: String): List<TransitStop> {
        val dirParam = if (direction.equals("O", ignoreCase = true)) "outbound" else "inbound"
        val cacheKey = "kmb_stops_${routeName}_${dirParam}_$serviceType"
        val cached = getCache(cacheKey)
        
        if (!cached.isNullOrEmpty()) {
            val type = object : TypeToken<List<TransitStop>>() {}.type
            return gson.fromJson(cached, type)
        }

        return try {
            val response = busApiService.getRouteStops(routeName, dirParam, serviceType)
            val stops = response.data.map { dto ->
                TransitStop(
                    stopId = dto.stop,
                    nameZh = "載入中...",
                    nameEn = null,
                    sequence = dto.seq.toIntOrNull() ?: 0,
                    lat = null,
                    long = null
                )
            }
            val type = object : TypeToken<List<TransitStop>>() {}.type
            saveCache(cacheKey, gson.toJson(stops, type), ttlMinutes = 720)
            stops
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getStopEta(stopId: String, routeName: String): List<TransitEta> {
        return try {
            val response = busApiService.getStopEta(stopId, routeName)
            val now = ZonedDateTime.now()
            response.data.map { dto ->
                val minutesLeft = calculateMinutesLeft(dto.eta, now)
                TransitEta(
                    routeName = dto.route,
                    destinationZh = dto.destTc,
                    destinationEn = dto.destEn,
                    etaTime = dto.eta,
                    minutesLeft = minutesLeft,
                    remarkZh = dto.rmkTc,
                    remarkEn = dto.rmkEn
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAllBookmarks(): Flow<List<TransitBookmarkEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun toggleBookmark(bookmark: TransitBookmarkEntity) {
        val existing = bookmarkDao.getBookmark(bookmark.id)
        if (existing != null) {
            bookmarkDao.deleteBookmark(bookmark)
        } else {
            bookmarkDao.insertBookmark(bookmark)
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
