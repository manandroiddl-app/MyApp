package com.example.lifeapp.data.repository

import com.example.lifeapp.data.api.BusApiService
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
import java.time.Instant
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusRepository @Inject constructor(
    private val busApiService: BusApiService,
    private val baseCacheRepository: BaseCacheRepository,
    private val bookmarkDao: TransitBookmarkDao,
    private val gson: Gson
) {

    private val kmbRoutesCacheKey = "KMB_ROUTES_ALL_CACHE"

    /**
     * 取得九巴所有路線 (支援 Local First 快取 + API 靜默更新)
     */
    suspend fun getKmbRoutes(forceRefresh: Boolean = false): List<TransitRoute> {
        val cacheDurationMinutes = 24 * 60L // 路線列表快取 24 小時

        if (!forceRefresh) {
            val cachedJson = baseCacheRepository.getValidCache(kmbRoutesCacheKey, cacheDurationMinutes)
            if (cachedJson != null) {
                try {
                    val type = object : TypeToken<List<TransitRoute>>() {}.type
                    return gson.fromJson(cachedJson, type)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 從 API 抓取最新路線
        return try {
            val response = busApiService.getKmbRoutes()
            val dtoList = response.data ?: emptyList()
            val routes = dtoList.map { dto ->
                TransitRoute(
                    routeId = "${dto.route}-${dto.bound ?: "O"}-${dto.serviceType ?: "1"}",
                    routeName = dto.route,
                    transitType = TransitType.BUS,
                    company = OperatorCompany.KMB,
                    originZh = dto.origTc ?: "",
                    originEn = dto.origEn,
                    destinationZh = dto.destTc ?: "",
                    destinationEn = dto.destEn,
                    bound = dto.bound ?: "O",
                    serviceType = dto.serviceType ?: "1"
                )
            }
            // 寫入本地 Room 快取
            val jsonString = gson.toJson(routes)
            baseCacheRepository.saveCache(kmbRoutesCacheKey, jsonString)
            routes
        } catch (e: Exception) {
            e.printStackTrace()
            // 若網路失敗，嘗試合併讀取過期快取防死機
            val fallbackJson = baseCacheRepository.getValidCache(kmbRoutesCacheKey, 30 * 24 * 60L)
            if (fallbackJson != null) {
                val type = object : TypeToken<List<TransitRoute>>() {}.type
                gson.fromJson(fallbackJson, type)
            } else {
                emptyList()
            }
        }
    }

    /**
     * 取得特定路線的車站列表與詳情
     */
    suspend fun getKmbRouteStops(route: String, bound: String, serviceType: String): List<TransitStop> {
        val cacheKey = "KMB_STOPS_${route}_${bound}_${serviceType}"
        val cachedJson = baseCacheRepository.getValidCache(cacheKey, 7 * 24 * 60L) // 車站序列快取 7 天

        if (cachedJson != null) {
            try {
                val type = object : TypeToken<List<TransitStop>>() {}.type
                return gson.fromJson(cachedJson, type)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return try {
            val stopsResponse = busApiService.getKmbRouteStops(route, bound, serviceType)
            val stopDtos = stopsResponse.data ?: emptyList()

            val transitStops = stopDtos.map { stopDto ->
                // 逐一取得車站名稱與座標
                val detailResponse = try {
                    busApiService.getKmbStopDetail(stopDto.stopId).data
                } catch (e: Exception) {
                    null
                }

                TransitStop(
                    stopId = stopDto.stopId,
                    sequence = stopDto.sequence,
                    nameZh = detailResponse?.nameTc ?: "車站 ${stopDto.sequence}",
                    nameEn = detailResponse?.nameEn,
                    latitude = detailResponse?.lat?.toDoubleOrNull() ?: 0.0,
                    longitude = detailResponse?.long?.toDoubleOrNull() ?: 0.0
                )
            }

            val jsonString = gson.toJson(transitStops)
            baseCacheRepository.saveCache(cacheKey, jsonString)
            transitStops
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 取得特定車站 + 路線的即時 ETA (不快取，直接抓取實時資料)
     */
    suspend fun getKmbEta(stopId: String, route: String, serviceType: String): List<TransitEta> {
        return try {
            val response = busApiService.getKmbEta(stopId, route, serviceType)
            val dtoList = response.data ?: emptyList()

            dtoList.map { dto ->
                val minutesLeft = calculateMinutesLeft(dto.etaTimestamp)
                TransitEta(
                    routeName = dto.route ?: route,
                    company = OperatorCompany.KMB,
                    destinationZh = dto.destTc ?: "",
                    etaTimestamp = dto.etaTimestamp,
                    remarkZh = dto.remarkTc,
                    minutesLeft = minutesLeft
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 計算剩餘到站分鐘數
    private fun calculateMinutesLeft(etaTimestamp: String?): Int? {
        if (etaTimestamp.isNull_or_Empty()) return null
        return try {
            val etaTime = Instant.parse(etaTimestamp)
            val now = Instant.now()
            val minutes = Duration.between(now, etaTime).toMinutes().toInt()
            if (minutes < 0) 0 else minutes
        } catch (e: Exception) {
            null
        }
    }

    // ==========================================
    // Bookmark 收藏功能管理
    // ==========================================

    fun getAllBookmarks(): Flow<List<TransitBookmarkEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun addBookmark(bookmark: TransitBookmarkEntity) {
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun removeBookmark(bookmarkId: String) {
        bookmarkDao.deleteBookmarkById(bookmarkId)
    }

    suspend fun isBookmarked(bookmarkId: String): Boolean {
        return bookmarkDao.isBookmarked(bookmarkId)
    }
}
