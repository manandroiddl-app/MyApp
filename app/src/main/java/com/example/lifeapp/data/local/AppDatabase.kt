package com.example.lifeapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lifeapp.data.model.BusBookmarkEntity

@Database(entities = [BusBookmarkEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun busBookmarkDao(): BusBookmarkDao
}
