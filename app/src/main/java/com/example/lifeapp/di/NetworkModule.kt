app/src/main/java/com/example/lifeapp/di/NetworkModule.kt
package com.example.lifeapp.di

import com.example.lifeapp.data.api.AppConfigApiService
import com.example.lifeapp.data.api.BusApiService
import com.example.lifeapp.data.api.HkoApiService
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
            .build()[cite: 2]
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://data.weather.gov.hk/") // 預設 Base URL (適用於天文台 API)[cite: 2]
            .client(okHttpClient)[cite: 2]
            .addConverterFactory(GsonConverterFactory.create())[cite: 2]
            .build()[cite: 2]
    }

    // 1. 香港天文台 API
    @Provides
    @Singleton
    fun provideHkoApiService(retrofit: Retrofit): HkoApiService {
        return retrofit.create(HkoApiService::class.java)[cite: 2]
    }

    // 2. GitHub Remote Config API
    @Provides
    @Singleton
    fun provideAppConfigApiService(retrofit: Retrofit): AppConfigApiService {
        return retrofit.create(AppConfigApiService::class.java)[cite: 2]
    }

    // 3. 運輸署特別交通消息 TD API
    @Provides
    @Singleton
    fun provideTdApiService(retrofit: Retrofit): TdApiService {
        return retrofit.create(TdApiService::class.java)[cite: 2]
    }

    // 4. 九巴 Open Data API (使用專屬的 Base URL)
    @Provides
    @Singleton
    fun provideBusApiService(okHttpClient: OkHttpClient): BusApiService {
        return Retrofit.Builder()
            .baseUrl("https://data.etabus.gov.hk/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BusApiService::class.java)
    }
}
