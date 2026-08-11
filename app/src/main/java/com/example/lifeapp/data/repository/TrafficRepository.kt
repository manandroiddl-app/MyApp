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
                val rawChin = msg.chinText?.trim() ?: ""
                val rawEng = msg.engText?.trim() ?: ""
                
                // 優先使用中文，若無則使用英文
                val rawContent = if (rawChin.isNotBlank()) rawChin else rawEng
                if (rawContent.isBlank()) return@mapNotNull null

                // 清洗可能殘留的 HTML 標籤 (例如 <br/>)
                val cleanContent = rawContent
                    .replace(Regex("<[^>]*>"), "")
                    .replace("&nbsp;", " ")
                    .trim()

                val id = msg.msgID ?: ""
                val rawDate = msg.referenceDate ?: ""
                val formattedTime = parseTime(rawDate)

                TrafficItem(
                    id = id,
                    title = cleanContent,
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
            // 解析運輸署時間格式（例如：2026-08-11 23:00:00）
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
