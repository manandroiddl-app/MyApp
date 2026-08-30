package com.example.lifeapp.data.repository

import com.example.lifeapp.data.dao.BookmarkDao
import com.example.lifeapp.data.entity.BookmarkEntity
import com.example.lifeapp.data.model.OperatorCompany
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusRepository @Inject constructor(
    private val kmbDataSource: KmbDataSource,
    private val bookmarkDao: BookmarkDao
) {
    // ==========================================
    // 1. 通用 / 多機構數據 API (Facade Interface)
    // ==========================================

    /**
     * 獲取所有交通機構的路線列表 (目前僅返回 KMB)
     */
    suspend fun getAllRoutes(): List<TransitRoute> {
        return kmbDataSource.getRoutes()
    }

    /**
     * 根據路線的公司類型分發獲取車站列表
     */
    suspend fun getRouteStops(route: TransitRoute): List<TransitStop> {
        return when (route.company) {
            OperatorCompany.KMB -> kmbDataSource.getRouteStops(
                routeName = route.routeName,
                bound = route.bound ?: "O",
                serviceType = route.serviceType ?: "1"
            )
            else -> emptyList()
        }
    }

    /**
     * 根據指定交通公司獲取即時到站時間 (ETA)
     */
    suspend fun getEta(
        company: OperatorCompany,
        stopId: String,
        routeName: String,
        serviceType: String
    ): List<TransitEta> {
        return when (company) {
            OperatorCompany.KMB -> kmbDataSource.getEta(
                stopId = stopId,
                routeName = routeName,
                serviceType = serviceType
            )
            else -> emptyList()
        }
    }

    // ==========================================
    // 2. 向下相容的方法 (相容舊有叫法)
    // ==========================================

    suspend fun getKmbRoutes(): List<TransitRoute> {
        return kmbDataSource.getRoutes()
    }

    suspend fun getKmbRouteStops(
        routeName: String,
        bound: String,
        serviceType: String
    ): List<TransitStop> {
        return kmbDataSource.getRouteStops(routeName, bound, serviceType)
    }

    suspend fun getKmbEta(
        stopId: String,
        routeName: String,
        serviceType: String
    ): List<TransitEta> {
        return kmbDataSource.getEta(stopId, routeName, serviceType)
    }

    // ==========================================
    // 3. 本地資料庫 (Room / Bookmark) 100% 原樣保留
    // ==========================================

    fun getAllBookmarks(): Flow<List<BookmarkEntity>> {
        return bookmarkDao.getAllBookmarks()
    }

    suspend fun isBookmarked(id: String): Boolean {
        return bookmarkDao.isBookmarked(id)
    }

    suspend fun insertBookmark(bookmark: BookmarkEntity) {
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) {
        bookmarkDao.deleteBookmark(bookmark)
    }

    suspend fun deleteBookmarkById(id: String) {
        bookmarkDao.deleteBookmarkById(id)
    }
}
