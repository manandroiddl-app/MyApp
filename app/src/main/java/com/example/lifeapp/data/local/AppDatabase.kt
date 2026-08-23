app/src/main/java/com/example/lifeapp/data/local/AppDatabase.kt
package com.example.lifeapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lifeapp.data.local.dao.TransitBookmarkDao
import com.example.lifeapp.data.local.entity.TransitBookmarkEntity

@Database(
    entities = [
        GenericCacheEntity::class,
        TransitBookmarkEntity::class // 新增 TransitBookmarkEntity[cite: 3]
    ],
    version = 4,                      // 升級版本號至 4[cite: 3]
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun genericCacheDao(): GenericCacheDao[cite: 3]
    abstract fun transitBookmarkDao(): TransitBookmarkDao // 新增 TransitBookmarkDao Provider
}
