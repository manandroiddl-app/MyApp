package com.example.lifeapp.di

import com.example.lifeapp.data.api.AppConfigApiService
import com.example.lifeapp.data.api.HkoApiService
import com.example.lifeapp.data.api.KmbApiService
import com.example.lifeapp.data.api.LocationApiService
import com.example.lifeapp.data.api.TdApiService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient() // 🛡️ 鬆散解析：防止 API 回傳非標準 JSON 或 HTML 導致 crash
            .create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideLocationApiService(okHttpClient: OkHttpClient, gson: Gson): LocationApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.csdi.gov.hk/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(LocationApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideHkoApiService(okHttpClient: OkHttpClient, gson: Gson): HkoApiService {
        return Retrofit.Builder()
            .baseUrl("https://data.weather.gov.hk/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(HkoApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTdApiService(okHttpClient: OkHttpClient, gson: Gson): TdApiService {
        return Retrofit.Builder()
            .baseUrl("https://rt.data.gov.hk/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(TdApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideKmbApiService(okHttpClient: OkHttpClient, gson: Gson): KmbApiService {
        return Retrofit.Builder()
            .baseUrl("https://data.etabus.gov.hk/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(KmbApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAppConfigApiService(okHttpClient: OkHttpClient, gson: Gson): AppConfigApiService {
        return Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(AppConfigApiService::class.java)
    }
}
