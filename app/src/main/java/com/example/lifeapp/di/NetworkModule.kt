package com.example.lifeapp.di

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

    private const val HKO_BASE_URL = "https://data.weather.gov.hk/"
    private const val TD_BASE_URL = "https://resource.data.one.gov.hk/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/109.0 Firefox/115.0")
                    .header("Accept", "*/*")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideHkoApiService(okHttpClient: OkHttpClient): HkoApiService {
        return Retrofit.Builder()
            .baseUrl(HKO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HkoApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTdApiService(okHttpClient: OkHttpClient): TdApiService {
        return Retrofit.Builder()
            .baseUrl(TD_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TdApiService::class.java)
    }
}
