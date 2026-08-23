package com.example.lifeapp.di

import android.content.Context
import androidx.room.Room
import com.example.lifeapp.data.local.AppDatabase
import com.example.lifeapp.data.local.GenericCacheDao
import com.example.lifeapp.data.local.dao.TransitBookmarkDao
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()[cite: 1]

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lifeapp_database"
        ).fallbackToDestructiveMigration().build()[cite: 1]
    }

    @Provides
    fun provideGenericCacheDao(database: AppDatabase): GenericCacheDao {
        return database.genericCacheDao()[cite: 1]
    }

    // 新增提供 TransitBookmarkDao
    @Provides
    fun provideTransitBookmarkDao(database: AppDatabase): TransitBookmarkDao {
        return database.transitBookmarkDao()
    }
}
