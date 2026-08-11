package com.example.lifeapp.di

import android.content.Context
import androidx.room.Room
import com.example.lifeapp.data.local.AppDatabase
import com.example.lifeapp.data.local.BusBookmarkDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lifeapp_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideBusBookmarkDao(database: AppDatabase): BusBookmarkDao {
        return database.busBookmarkDao()
    }
}
