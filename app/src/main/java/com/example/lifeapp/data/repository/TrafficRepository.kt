package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.TdApiService
import com.example.lifeapp.data.model.TrafficNewsItem
import com.example.lifeapp.data.model.TrafficUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrafficRepository @Inject constructor(
    private val tdApiService: TdApiService
) {
    suspend fun fetchTrafficNews(): TrafficUiState {
        return runCatching {
            val jsonElement = tdApiService.getSpecialTrafficNewsRaw()
            val newsList = mutableListOf<TrafficNewsItem>()

            if (jsonElement.isJsonArray) {
                jsonElement.asJsonArray.forEach { elem ->
                    if (elem.isJsonObject) {
                        val obj = elem.asJsonObject
                        val text = when {
                            obj.has("chinText") -> obj.get("chinText").asString
                            obj.has("msgText") -> obj.get("msgText").asString
                            else -> ""
                        }
                        val date = if (obj.has("referenceDate")) obj.get("referenceDate").asString else ""
                        
                        if (text.isNotBlank()) {
                            newsList.add(TrafficNewsItem(chinText = text, referenceDate = date))
                        }
                    }
                }
            }

            val nowStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            TrafficUiState(
                isLoading = false,
                trafficNews = newsList,
                updateTime = nowStr,
                errorMessage = if (newsList.isEmpty()) "目前沒有特別交通消息" else null
            )
        }.getOrElse { e ->
            Log.e("TrafficRepo", "Fetch traffic error", e)
            TrafficUiState(
                isLoading = false,
                errorMessage = "無法獲取特別交通消息"
            )
        }
    }
}
