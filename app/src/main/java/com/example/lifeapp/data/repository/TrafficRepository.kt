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
            val responseBody = apiService.getSpecialTrafficNewsRaw()
            val xmlText = responseBody.string()

            val items = parseXmlWithRegex(xmlText)

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

    private fun parseXmlWithRegex(xml: String): List<TrafficItem> {
        val itemList = mutableListOf<TrafficItem>()

        // 匹配每一個 <message>...</message> 區塊
        val messageRegex = Regex("<message>(.*?)</message>", RegexOption.DOT_MATCHES_ALL)
        val matches = messageRegex.findAll(xml)

        val options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)

        for (match in matches) {
            val block = match.groupValues[1]

            // 提取 msgID
            val msgIdMatch = Regex("<msgID>(.*?)</msgID>", RegexOption.DOT_MATCHES_ALL).find(block)
            val msgId = msgIdMatch?.groupValues?.get(1)?.trim() ?: ""

            // 提取 中文內容 (包含 CDATA 防禦)
            val chinMatch = Regex("<ChinText>(.*?)</ChinText>", options).find(block)
            var chinText = chinMatch?.groupValues?.get(1)?.trim() ?: ""

            // 提取 英文內容 (備用)
            val engMatch = Regex("<EngText>(.*?)</EngText>", options).find(block)
            var engText = engMatch?.groupValues?.get(1)?.trim() ?: ""

            // 提取 時間
            val dateMatch = Regex("<ReferenceDate>(.*?)</ReferenceDate>", options).find(block)
            val rawDate = dateMatch?.groupValues?.get(1)?.trim() ?: ""

            // 清理 CDATA 與 HTML 標籤
            chinText = cleanXmlText(chinText)
            engText = cleanXmlText(engText)

            val content = if (chinText.isNotBlank()) chinText else engText
            if (content.isNotBlank()) {
                itemList.add(
                    TrafficItem(
                        id = msgId,
                        title = content,
                        timeText = parseTime(rawDate)
                    )
                )
            }
        }

        return itemList
    }

    private fun cleanXmlText(text: String): String {
        return text
            .replace("<![CDATA[", "")
            .replace("]]>", "")
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }

    private fun parseTime(rawTime: String): String {
        if (rawTime.isBlank()) return "特別交通通告"
        return try {
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
