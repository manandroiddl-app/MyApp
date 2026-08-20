package com.example.lifeapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    // 巴士清空後，暫無 DAO，留待地點搜尋 DAO 注入
}
