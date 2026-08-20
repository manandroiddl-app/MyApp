package com.example.lifeapp.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "app_json_cache")
data class GenericCacheEntity(
    @PrimaryKey val cacheKey: String,
    val jsonContent: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Dao
interface GenericCacheDao {
    @Query("SELECT * FROM app_json_cache WHERE cacheKey = :key")
    suspend fun getCache(key: String): GenericCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCache(cache: GenericCacheEntity)
}
