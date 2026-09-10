package com.example.lifeapp.di

import android.content.Context
import androidx.room.Room
import com.example.lifeapp.data.datasource.KmbDataSource
import com.example.lifeapp.data.local.AppDatabase
import com.example.lifeapp.data.local.GenericCacheDao
import com.example.lifeapp.data.local.dao.TransitBookmarkDao
import com.example.lifeapp.data.local.dao.TransitDao
import com.example.lifeapp.data.repository.transit.TransitSyncManager
import com.example.lifeapp.data.repository.transit.fetcher.KmbDataFetcher
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
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lifeapp_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideGenericCacheDao(database: AppDatabase): GenericCacheDao {
        return database.genericCacheDao()
    }

    @Provides
    fun provideTransitBookmarkDao(database: AppDatabase): TransitBookmarkDao {
        return database.transitBookmarkDao()
    }

    @Provides
    fun provideTransitDao(database: AppDatabase): TransitDao {
        return database.transitDao()
    }

    @Provides
    @Singleton
    fun provideKmbDataFetcher(kmbDataSource: KmbDataSource): KmbDataFetcher {
        return KmbDataFetcher(kmbDataSource)
    }

    @Provides
    @Singleton
    fun provideTransitSyncManager(
        database: AppDatabase,
        transitDao: TransitDao,
        kmbDataFetcher: KmbDataFetcher
    ): TransitSyncManager {
        return TransitSyncManager(database, transitDao, kmbDataFetcher)
    }
}
