package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.TrafficApiService
import com.example.lifeapp.data.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrafficRepository @Inject constructor(
    private val trafficApiService: TrafficApiService
) {
    suspend fun fetchTrafficNews(): TrafficUiState {
        return runCatching {
            val newsList = trafficApiService.getTrafficNews()
            val nowStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            TrafficUiState(
                isLoading = false,
                trafficNews = newsList,
                updateTime = nowStr,
                errorMessage = null
            )
        }.getOrElse { e ->
            Log.e("TrafficRepo", "Fetch traffic error", e)
            TrafficUiState(
                isLoading = false,
                errorMessage = "無法獲取特別交通消息，請再試一遍"
            )
        }
    }
}
