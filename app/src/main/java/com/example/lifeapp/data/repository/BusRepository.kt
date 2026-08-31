package com.example.lifeapp.data.repository

import com.example.lifeapp.data.datasource.CtbDataSource
import com.example.lifeapp.data.datasource.KmbDataSource
import com.example.lifeapp.data.local.dao.TransitBookmarkDao
import com.example.lifeapp.data.local.entity.TransitBookmarkEntity
import com.example.lifeapp.data.model.OperatorCompany
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusRepository @Inject constructor(
    private val kmbDataSource: KmbDataSource,
    private val ctbDataSource: CtbDataSource,
    private val bookmarkDao: TransitBookmarkDao
) {

    /**
     * 獲取所有營運商路線（Phase 2: 併發合併 KMB 與 CTB 路線數據）
     */
    suspend fun getRoutes(): List<TransitRoute> = coroutineScope {
        val kmbDeferred = async { runCatching { kmbDataSource.getRoutes() }.getOrDefault(emptyList()) }
        val ctbDeferred = async { runCatching { ctbDataSource.getRoutes() }.getOrDefault(emptyList()) }

        val kmbRoutes = kmbDeferred.await()
        val ctbRoutes = ctbDeferred.await()

        kmbRoutes + ctbRoutes
    }

    /**
     * 獲取指定路線的車站清單（帶入營運商分流）
     */
    suspend fun getRouteStops(company: OperatorCompany, route: String, bound: String, serviceType: String): List<TransitStop> {
        return when (company) {
            OperatorCompany.CTB -> ctbDataSource.getRouteStops(route, bound, serviceType)
            else -> kmbDataSource.getRouteStops(route, bound, serviceType)
        }
    }

    /**
     * 獲取指定路線的車站清單（向下相容分流）
     */
    suspend fun getRouteStops(route: String, bound: String, serviceType: String): List<TransitStop> {
        val kmbStops = runCatching { kmbDataSource.getRouteStops(route, bound, serviceType) }.getOrDefault(emptyList())
        if (kmbStops.isNotEmpty()) return kmbStops

        return runCatching { ctbDataSource.getRouteStops(route, bound, serviceType) }.getOrDefault(emptyList())
    }

    /**
     * 獲取指定車站與路線的實時 ETA 到站時間（帶入營運商分流）
     */
    suspend fun getEta(company: OperatorCompany, stopId: String, route: String, serviceType: String): List<TransitEta> {
        return when (company) {
            OperatorCompany.CTB -> ctbDataSource.getEta(stopId, route, serviceType)
            else -> kmbDataSource.getEta(stopId, route, serviceType)
        }
    }

    /**
     * 獲取指定車站與路線的實時 ETA 到站時間（向下相容）
     */
    suspend fun getEta(stopId: String, route: String, serviceType: String): List<TransitEta> = coroutineScope {
        val kmbEtaDeferred = async { runCatching { kmbDataSource.getEta(stopId, route, serviceType) }.getOrDefault(emptyList()) }
        val ctbEtaDeferred = async { runCatching { ctbDataSource.getEta(stopId, route, serviceType) }.getOrDefault(emptyList()) }

        val kmbList = kmbEtaDeferred.await()
        val ctbList = ctbEtaDeferred.await()

        (kmbList + ctbList).sortedBy { it.minutesLeft ?: Int.MAX_VALUE }
    }

    // =========================================================================
    // 書籤管理 (Local Database operations)
    // =========================================================================

    fun getAllBookmarks(): Flow<List<TransitBookmarkEntity>> {
        return bookmarkDao.getAllBookmarks()
    }

    suspend fun addBookmark(bookmark: TransitBookmarkEntity) {
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun removeBookmark(bookmarkId: String) {
        bookmarkDao.deleteBookmarkById(bookmarkId)
    }
}
