package com.example.lifeapp.data.local

import androidx.room.*
import com.example.lifeapp.data.model.BusBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusBookmarkDao {
    @Query("SELECT * FROM bus_bookmarks ORDER BY createTime DESC")
    fun getAllBookmarks(): Flow<List<BusBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BusBookmarkEntity)

    @Query("DELETE FROM bus_bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bus_bookmarks WHERE id = :id)")
    suspend fun isBookmarked(id: String): Boolean
}
