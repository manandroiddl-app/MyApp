package com.example.lifeapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifeapp.data.model.DistrictHierarchyEntity
import com.example.lifeapp.data.model.LocationEntity

@Dao
interface LocationDao {

    // --- Locations (實體地點/站點/街道) ---

    @Query("SELECT * FROM locations")
    suspend fun getAllLocations(): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE id = :id LIMIT 1")
    suspend fun getLocationById(id: String): LocationEntity?

    /**
     * 🛡️ 核心修復：確保 SQL 欄位名稱 (regionName, districtName, subDistrictName) 
     * 與 LocationEntity 中的屬性定義 100% 一致！
     */
    @Query("""
        SELECT * FROM locations 
        WHERE nameTc LIKE '%' || :query || '%' 
           OR nameEn LIKE '%' || :query || '%' 
           OR subDistrictName LIKE '%' || :query || '%'
           OR districtName LIKE '%' || :query || '%'
           OR searchKeywords LIKE '%' || :query || '%'
           OR routes LIKE '%' || :query || '%'
        LIMIT 50
    """)
    suspend fun searchLocations(query: String): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE type = 'STREET'")
    suspend fun getAllStreets(): List<LocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocations(locations: List<LocationEntity>)

    @Query("DELETE FROM locations")
    suspend fun clearAllLocations()

    @Query("SELECT COUNT(*) FROM locations")
    suspend fun getLocationCount(): Int

    // --- District Hierarchy (區域與分區表) ---

    @Query("SELECT * FROM district_hierarchy")
    suspend fun getAllDistrictHierarchies(): List<DistrictHierarchyEntity>

    @Query("SELECT * FROM district_hierarchy WHERE districtName LIKE '%' || :query || '%' OR subDistrictName LIKE '%' || :query || '%'")
    suspend fun searchHierarchies(query: String): List<DistrictHierarchyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDistrictHierarchies(hierarchies: List<DistrictHierarchyEntity>)

    @Query("DELETE FROM district_hierarchy")
    suspend fun clearAllHierarchies()
}
