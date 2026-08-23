package com.example.lifeapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lifeapp.data.local.dao.TransitBookmarkDao
import com.example.lifeapp.data.local.entity.TransitBookmarkEntity

@Database(
    entities = [
        GenericCacheEntity::class,
        TransitBookmarkEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun genericCacheDao(): GenericCacheDao
    abstract fun transitBookmarkDao(): TransitBookmarkDao
}
