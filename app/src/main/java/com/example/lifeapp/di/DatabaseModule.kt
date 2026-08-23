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
}
