package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.local.GenericCacheDao
import com.example.lifeapp.data.local.GenericCacheEntity
import com.google.gson.Gson

abstract class BaseCacheRepository<T>(
    private val genericCacheDao: GenericCacheDao,
    private val gson: Gson,
    private val cacheKey: String,
    private val clazz: Class<T>
) {
    suspend fun loadFromCache(): T? {
        return try {
            val entity = genericCacheDao.getCache(cacheKey)
            if (entity != null && entity.jsonContent.isNotBlank()) {
                gson.fromJson(entity.jsonContent, clazz)
            } else null
        } catch (e: Exception) {
            Log.e("BaseCacheRepo", "Error reading cache for key: $cacheKey", e)
            null
        }
    }

    suspend fun saveToCache(data: T) {
        try {
            val jsonStr = gson.toJson(data)
            genericCacheDao.saveCache(
                GenericCacheEntity(cacheKey = cacheKey, jsonContent = jsonStr)
            )
        } catch (e: Exception) {
            Log.e("BaseCacheRepo", "Error saving cache for key: $cacheKey", e)
        }
    }
}
