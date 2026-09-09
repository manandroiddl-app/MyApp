package com.example.lifeapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lifeapp.data.local.dao.TransitBookmarkDao
import com.example.lifeapp.data.local.dao.TransitDao
import com.example.lifeapp.data.local.entity.TransitBookmarkEntity
import com.example.lifeapp.data.local.entity.TransitLastUpdateEntity
import com.example.lifeapp.data.local.entity.TransitRouteEntity
import com.example.lifeapp.data.local.entity.TransitRouteStopEntity
import com.example.lifeapp.data.local.entity.TransitStopEntity

@Database(
    entities = [
        GenericCacheEntity::class,
        TransitBookmarkEntity::class,
        TransitRouteEntity::class,
        TransitStopEntity::class,
        TransitRouteStopEntity::class,
        TransitLastUpdateEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun genericCacheDao(): GenericCacheDao
    abstract fun transitBookmarkDao(): TransitBookmarkDao
    abstract fun transitDao(): TransitDao
}
