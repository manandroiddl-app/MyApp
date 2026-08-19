package com.example.lifeapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lifeapp.data.model.BusBookmarkEntity
import com.example.lifeapp.data.model.DistrictHierarchyEntity
import com.example.lifeapp.data.model.LocationEntity

@Database(
    entities = [
        BusBookmarkEntity::class,
        LocationEntity::class,
        DistrictHierarchyEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun busBookmarkDao(): BusBookmarkDao
    abstract fun locationDao(): LocationDao
}
