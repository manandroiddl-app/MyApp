package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.KmbApiService
import com.example.lifeapp.data.local.BusBookmarkDao
import com.example.lifeapp.data.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KmbRepository @Inject constructor(
    private val kmbApiService: KmbApiService,
    private val bookmarkDao: BusBookmarkDao
) {
    fun getAllBookmarks(): Flow<List<BusBookmarkEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun toggleBookmark(bookmark: BusBookmarkEntity): Boolean {
        return if (bookmarkDao.isBookmarked(bookmark.id)) {
            bookmarkDao.deleteBookmarkById(bookmark.id)
            false
        } else {
            bookmarkDao.insertBookmark(bookmark)
            true
        }
    }

    suspend fun isBookmarked(id: String): Boolean = bookmarkDao.isBookmarked(id)

    suspend fun fetchAllRoutes(): List<KmbRoute> {
        return runCatching {
            val res = kmbApiService.getAllRoutes()
            res.data ?: emptyList()
        }.getOrElse {
            Log.e("KmbRepo", "Fetch routes error", it)
            emptyList()
        }
    }

    suspend fun fetchRouteStopsWithDetail(route: String, bound: String, serviceType: String): List<Pair<KmbRouteStop, KmbStopDetail>> = coroutineScope {
        runCatching {
            val stopsRes = kmbApiService.getRouteStops(route, bound, serviceType)
            val stops = stopsRes.data ?: return@coroutineScope emptyList()

            // 批量非同步拉取各站詳細中文名稱，單站失敗時自動容錯
            val deferreds = stops.map { stop ->
                async {
                    val detail = runCatching {
                        val detailRes = kmbApiService.getStopDetail(stop.stopId)
                        detailRes.data
                    }.getOrNull() ?: KmbStopDetail(stop.stopId, "車站 ${stop.stopId}", null, null)

                    Pair(stop, detail)
                }
            }
            deferreds.awaitAll()
        }.getOrElse { e ->
            Log.e("KmbRepo", "Fetch route stops error", e)
            emptyList()
        }
    }

    suspend fun fetchEtaForBookmark(bookmark: BusBookmarkEntity): List<BusEtaUiItem> {
        return runCatching {
            val res = kmbApiService.getStopEta(bookmark.stopId, bookmark.route, bookmark.serviceType)
            val etaList = res.data ?: emptyList()

            etaList.map { eta ->
                BusEtaUiItem(
                    bookmarkId = bookmark.id,
                    route = bookmark.route,
                    stopName = bookmark.stopNameTc,
                    destName = eta.destTc ?: bookmark.destTc,
                    etaText = formatEtaTime(eta.eta),
                    remark = eta.rmkTc ?: ""
                )
            }
        }.getOrElse {
            emptyList()
        }
    }

    private fun formatEtaTime(rawEta: String?): String {
        if (rawEta.isNullOrBlank()) return "暫無班次資料"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(rawEta.substring(0, 19))
            if (date != null) {
                val diffMs = date.time - System.currentTimeMillis()
                val diffMinutes = (diffMs / (1000 * 60)).toInt()

                when {
                    diffMinutes <= 0 -> "即將到達"
                    diffMinutes < 60 -> "${diffMinutes} 分鐘"
                    else -> {
                        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        outputFormat.format(date)
                    }
                }
            } else {
                rawEta
            }
        } catch (e: Exception) {
            rawEta
        }
    }
}
