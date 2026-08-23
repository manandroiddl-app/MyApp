app/src/main/java/com/example/lifeapp/data/local/dao/TransitBookmarkDao.kt
package com.example.lifeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifeapp.data.local.entity.TransitBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransitBookmarkDao {

    /**
     * 監聽所有收藏的交通車站
     */
    @Query("SELECT * FROM transit_bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<TransitBookmarkEntity>>

    /**
     * 新增或置換收藏
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: TransitBookmarkEntity)

    /**
     * 移除特定收藏
     */
    @Query("DELETE FROM transit_bookmarks WHERE bookmarkId = :bookmarkId")
    suspend fun deleteBookmarkById(bookmarkId: String)

    /**
     * 檢查特定車站路線是否已收藏
     */
    @Query("SELECT EXISTS(SELECT 1 FROM transit_bookmarks WHERE bookmarkId = :bookmarkId)")
    suspend fun isBookmarked(bookmarkId: String): Boolean
}
