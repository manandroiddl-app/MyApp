package com.example.lifeapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifeapp.data.model.LocationEntity

@Dao
interface LocationDao {

    @Query("SELECT * FROM locations")
    suspend fun getAllLocations(): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE id = :id LIMIT 1")
    suspend fun getLocationById(id: String): LocationEntity?

    @Query("""
        SELECT * FROM locations 
        WHERE nameTc LIKE '%' || :query || '%' 
           OR nameEn LIKE '%' || :query || '%' 
           OR subDistrict LIKE '%' || :query || '%'
           OR district LIKE '%' || :query || '%'
           OR searchKeywords LIKE '%' || :query || '%'
           OR routes LIKE '%' || :query || '%'
        LIMIT 50
    """)
    suspend fun searchLocations(query: String): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE region = :region")
    suspend fun getLocationsByRegion(region: String): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE district = :district")
    suspend fun getLocationsByDistrict(district: String): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE subDistrict = :subDistrict")
    suspend fun getLocationsBySubDistrict(subDistrict: String): List<LocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocations(locations: List<LocationEntity>)

    @Query("DELETE FROM locations")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM locations")
    suspend fun getLocationCount(): Int
}
