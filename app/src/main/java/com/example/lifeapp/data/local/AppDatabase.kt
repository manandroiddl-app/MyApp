package com.example.lifeapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lifeapp.data.model.WeatherCacheEntity

@Database(
    entities = [
        WeatherCacheEntity::class // 🎯 已移除未定義的 DummyEntity
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
}
