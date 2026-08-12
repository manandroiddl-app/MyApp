package com.example.lifeapp.di

import com.example.lifeapp.data.api.AppConfigApiService
import com.example.lifeapp.data.api.HkoApiService
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

    // 👈 核心修正：補齊 provideRetrofit，解決 MissingBinding 錯誤
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://data.weather.gov.hk/") // 預設 Base URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideHkoApiService(retrofit: Retrofit): HkoApiService {
        return retrofit.create(HkoApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAppConfigApiService(retrofit: Retrofit): AppConfigApiService {
        return retrofit.create(AppConfigApiService::class.java)
    }
}
