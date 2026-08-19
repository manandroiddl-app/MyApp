package com.example.lifeapp.di

import com.example.lifeapp.data.api.BusApiService
import com.example.lifeapp.data.api.LocationApiService
import com.example.lifeapp.data.api.TdApiService
import com.example.lifeapp.data.api.WeatherApiService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient() // 🛡️ 關鍵：允許較鬆散/不完美的 JSON 解析
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
            .baseUrl("https://api.csdi.gov.hk/") // CSDI Base URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(LocationApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideWeatherApiService(okHttpClient: OkHttpClient, gson: Gson): WeatherApiService {
        return Retrofit.Builder()
            .baseUrl("https://data.weather.gov.hk/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(WeatherApiService::class.java)
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
    fun provideBusApiService(okHttpClient: OkHttpClient, gson: Gson): BusApiService {
        return Retrofit.Builder()
            .baseUrl("https://data.etabus.gov.hk/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(BusApiService::class.java)
    }
}
