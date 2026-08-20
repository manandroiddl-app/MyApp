package com.example.lifeapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifeapp.data.model.WeatherCacheEntity

@Dao
interface WeatherDao {

    @Query("SELECT * FROM weather_ui_cache WHERE id = 1")
    suspend fun getWeatherCache(): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWeatherCache(cache: WeatherCacheEntity)
}
