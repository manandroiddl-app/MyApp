package com.example.lifeapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Database
import androidx.room.RoomDatabase

// 暫時 Dummy Entity 避免 Room/kapt 因空 entities 報錯
@Entity(tableName = "dummy_table")
data class DummyEntity(
    @PrimaryKey val id: Int = 1
)

@Database(entities = [DummyEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    // 待下一階段加入 LocationDao
}
