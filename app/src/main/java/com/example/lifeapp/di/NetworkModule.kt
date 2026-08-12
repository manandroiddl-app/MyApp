package com.example.lifeapp.di

import com.example.lifeapp.data.api.HkoApiService
import com.example.lifeapp.data.api.KmbApiService
import com.example.lifeapp.data.api.TdApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideHkoApiService(okHttpClient: OkHttpClient): HkoApiService {
        return Retrofit.Builder()
            .baseUrl("https://data.weather.gov.hk/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HkoApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTdApiService(okHttpClient: OkHttpClient): TdApiService {
        return Retrofit.Builder()
            .baseUrl("https://data.gov.hk/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TdApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideKmbApiService(okHttpClient: OkHttpClient): KmbApiService {
        return Retrofit.Builder()
            .baseUrl("https://data.etabus.gov.hk/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KmbApiService::class.java)
    }
}
