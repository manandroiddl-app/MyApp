package com.example.lifeapp.di

import android.content.Context
import androidx.room.Room
import com.example.lifeapp.data.datasource.CtbDataSource
import com.example.lifeapp.data.datasource.KmbDataSource
import com.example.lifeapp.data.local.AppDatabase
import com.example.lifeapp.data.local.GenericCacheDao
import com.example.lifeapp.data.local.dao.TransitBookmarkDao
import com.example.lifeapp.data.local.dao.TransitDao
import com.example.lifeapp.data.repository.transit.TransitSyncManager
import com.example.lifeapp.data.repository.transit.fetcher.CtbDataFetcher
import com.example.lifeapp.data.repository.transit.fetcher.KmbDataFetcher
import com.example.lifeapp.util.FileLogger
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.InstallIn
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
    fun provideFileLogger(@ApplicationContext context: Context): FileLogger {
        return FileLogger(context)
    }

    @Provides
    @Singleton
    fun provideKmbDataFetcher(kmbDataSource: KmbDataSource): KmbDataFetcher {
        return KmbDataFetcher(kmbDataSource)
    }

    @Provides
    @Singleton
    fun provideCtbDataFetcher(ctbDataSource: CtbDataSource): CtbDataFetcher {
        return CtbDataFetcher(ctbDataSource)
    }

    @Provides
    @Singleton
    fun provideTransitSyncManager(
        @ApplicationContext context: Context,
        database: AppDatabase,
        transitDao: TransitDao,
        kmbDataFetcher: KmbDataFetcher,
        ctbDataFetcher: CtbDataFetcher,
        fileLogger: FileLogger
    ): TransitSyncManager {
        return TransitSyncManager(context, database, transitDao, kmbDataFetcher, ctbDataFetcher, fileLogger)
    }
}
