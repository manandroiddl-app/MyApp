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
            val responseBody = tdApiService.getSpecialTrafficNewsRaw()
            val xmlText = responseBody.string()
            
            val newsList = parseXmlTrafficNews(xmlText)
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
                errorMessage = "無法獲取特別交通消息：${e.localizedMessage}"
            )
        }
    }

    private fun parseXmlTrafficNews(xml: String): List<TrafficNewsItem> {
        val list = mutableListOf<TrafficNewsItem>()
        // 兼容匹配 <chinText> ... </chinText> 或 <MsgText> ... </MsgText>
        val regex = Regex("<(?:chinText|MsgText|content)>(.*?)</(?:chinText|MsgText|content)>", RegexOption.DOT_MATCHES_ALL)
        
        regex.findAll(xml).forEach { matchResult ->
            val rawText = matchResult.groupValues[1]
                .replace("<![CDATA[", "")
                .replace("]]>", "")
                .trim()
            if (rawText.isNotEmpty()) {
                list.add(TrafficNewsItem(chinText = rawText))
            }
        }
        return list
    }
}
