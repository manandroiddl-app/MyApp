package com.example.lifeapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lifeapp.data.model.WeatherCacheEntity

@Database(
    entities = [
        DummyEntity::class,
        WeatherCacheEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
}
