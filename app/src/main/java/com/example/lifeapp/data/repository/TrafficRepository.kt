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
            
            // 簡單解析 XML 內文 (例如標籤 <chinText> 或 <MsgText>)
            val newsList = parseXmlTrafficNews(xmlText)
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
                errorMessage = "無法獲取特別交通消息：${e.localizedMessage}"
            )
        }
    }

    private fun parseXmlTrafficNews(xml: String): List<TrafficNewsItem> {
        val list = mutableListOf<TrafficNewsItem>()
        val regex = Regex("<chinText>(.*?)</chinText>", RegexOption.DOT_MATCHES_ALL)
        regex.findAll(xml).forEach { matchResult ->
            val text = matchResult.groupValues[1].trim()
            if (text.isNotEmpty()) {
                list.add(TrafficNewsItem(chinText = text))
            }
        }
        return list
    }
}
