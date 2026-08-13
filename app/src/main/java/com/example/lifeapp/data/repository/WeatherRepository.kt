package com.example.lifeapp.data.repository

import com.example.lifeapp.data.api.HkoApiService
import com.example.lifeapp.data.model.HkoRhrreadResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val hkoApiService: HkoApiService
) {
    suspend fun getWeatherRealtimeData(): Result<HkoRhrreadResponse> {
        return withContext(Dispatchers.IO) {
            runCatching {
                hkoApiService.getRhrread()
            }
        }
    }
}
