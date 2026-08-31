package com.example.lifeapp.data.repository

import com.example.lifeapp.data.datasource.KmbDataSource
import com.example.lifeapp.data.local.dao.TransitBookmarkDao
import com.example.lifeapp.data.local.entity.TransitBookmarkEntity
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusRepository @Inject constructor(
    private val kmbDataSource: KmbDataSource,
    private val bookmarkDao: TransitBookmarkDao
) {

    /**
     * 獲取所有營運商路線（Phase 1 目前整合 KMB， Phase 2 可合併 CTB）
     */
    suspend fun getRoutes(): List<TransitRoute> {
        return kmbDataSource.getRoutes()
    }

    /**
     * 獲取指定路線的車站清單
     */
    suspend fun getRouteStops(route: String, bound: String, serviceType: String): List<TransitStop> {
        return kmbDataSource.getRouteStops(route, bound, serviceType)
    }

    /**
     * 獲取指定車站與路線的實時 ETA 到站時間
     */
    suspend fun getEta(stopId: String, route: String, serviceType: String): List<TransitEta> {
        return kmbDataSource.getEta(stopId, route, serviceType)
    }

    // =========================================================================
    // 向下相容既有方法 (向後相容 ViewModel/既有呼叫點，防止動到未修改代碼)
    // =========================================================================

    suspend fun getKmbRoutes(): List<TransitRoute> {
        return getRoutes()
    }

    suspend fun getKmbRouteStops(route: String, bound: String, serviceType: String): List<TransitStop> {
        return getRouteStops(route, bound, serviceType)
    }

    suspend fun getKmbEta(stopId: String, route: String, serviceType: String): List<TransitEta> {
        return getEta(stopId, route, serviceType)
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
