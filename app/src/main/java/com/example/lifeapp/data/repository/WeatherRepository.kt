package com.example.lifeapp.data.repository

import com.example.lifeapp.data.api.HkoApiService
import com.example.lifeapp.data.model.HkoFndResponse
import com.example.lifeapp.data.model.HkoRhrreadResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class CompleteWeatherData(
    val rhrread: HkoRhrreadResponse,
    val fnd: HkoFndResponse?
)

@Singleton
class WeatherRepository @Inject constructor(
    private val hkoApiService: HkoApiService
) {
    suspend fun getFullWeatherData(): Result<CompleteWeatherData> {
        return withContext(Dispatchers.IO) {
            runCatching {
                // 並行併發抓取即時天氣與 9 天預報
                val rhrreadDeferred = async { hkoApiService.getRhrread() }
                val fndDeferred = async { runCatching { hkoApiService.getFnd() }.getOrNull() }

                val rhrread = rhrreadDeferred.await()
                val fnd = fndDeferred.await()

                CompleteWeatherData(rhrread = rhrread, fnd = fnd)
            }
        }
    }
}
