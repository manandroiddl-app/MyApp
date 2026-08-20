package com.example.lifeapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        GenericCacheEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun genericCacheDao(): GenericCacheDao
}
