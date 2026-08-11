package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.TdApiService
import com.example.lifeapp.data.model.TrafficItem
import com.example.lifeapp.data.model.TrafficUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrafficRepository @Inject constructor(
    private val apiService: TdApiService
) {
    suspend fun fetchTrafficNews(): TrafficUiState {
        val outputDateFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault())
        val nowStr = outputDateFormat.format(Date())

        return runCatching {
            val response = apiService.getSpecialTrafficNews()
            val list = response.messageList ?: emptyList()

            val items = list.mapNotNull { msg ->
                val content = msg.chinText?.ifBlank { msg.engText } ?: return@mapNotNull null
                val id = msg.msgID ?: ""
                val rawDate = msg.referenceDate ?: ""
                
                // 格式化新聞時間
                val formattedTime = parseTime(rawDate)

                TrafficItem(
                    id = id,
                    title = content,
                    timeText = formattedTime
                )
            }

            TrafficUiState(
                isLoading = false,
                items = items,
                updateTimeText = "最後更新時間：$nowStr",
                errorMessage = null
            )
        }.getOrElse { e ->
            Log.e("TrafficRepo", "Fetch traffic error", e)
            TrafficUiState(
                isLoading = false,
                items = emptyList(),
                updateTimeText = "最後更新時間：$nowStr",
                errorMessage = "無法連線至運輸署交通消息伺服器。"
            )
        }
    }

    private fun parseTime(rawTime: String): String {
        if (rawTime.isBlank()) return "特別交通通告"
        return try {
            // 常見格式：yyyy-MM-dd HH:mm:ss 或 2026-08-11 23:00:00.0
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(rawTime)
            if (date != null) {
                val outputFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                outputFormat.format(date)
            } else {
                rawTime
            }
        } catch (e: Exception) {
            rawTime
        }
    }
}
