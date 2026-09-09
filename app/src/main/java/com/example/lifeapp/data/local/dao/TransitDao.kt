package com.example.lifeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifeapp.data.local.entity.TransitLastUpdateEntity
import com.example.lifeapp.data.local.entity.TransitRouteEntity
import com.example.lifeapp.data.local.entity.TransitRouteStopEntity
import com.example.lifeapp.data.local.entity.TransitStopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransitDao {

    // ==========================================
    // Batch Insert Methods (Phase 2 批次更新使用)
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<TransitRouteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<TransitStopEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouteStops(routeStops: List<TransitRouteStopEntity>)

    // ==========================================
    // Version Control Methods (版本紀錄使用)
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLastUpdate(lastUpdate: TransitLastUpdateEntity)

    @Query("SELECT * FROM Transit_Last_Update WHERE id = 1 LIMIT 1")
    suspend fun getLastUpdate(): TransitLastUpdateEntity?

    // ==========================================
    // Clear Table Methods (全量更新或清理快取使用)
    // ==========================================

    @Query("DELETE FROM Transit_Route")
    suspend fun clearRoutes()

    @Query("DELETE FROM Transit_Stop")
    suspend fun clearStops()

    @Query("DELETE FROM Transit_Route_Stop")
    suspend fun clearRouteStops()

    // ==========================================
    // Query Methods (Phase 3 頁面接入與查詢使用)
    // ==========================================

    @Query("SELECT * FROM Transit_Route WHERE co = :co AND route_name = :routeName")
    fun getRoutesByCoAndName(co: String, routeName: String): Flow<List<TransitRouteEntity>>

    @Query("""
        SELECT * FROM Transit_Route_Stop 
        WHERE co = :co AND route_name = :routeName AND bound = :bound AND other_key = :otherKey 
        ORDER BY seq ASC
    """)
    fun getStopsForRoute(co: String, routeName: String, bound: String, otherKey: String = "1"): Flow<List<TransitRouteStopEntity>>

    @Query("SELECT * FROM Transit_Stop WHERE co = :co AND stop_id = :stopId LIMIT 1")
    suspend fun getStopById(co: String, stopId: String): TransitStopEntity?
}
