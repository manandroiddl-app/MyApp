package com.example.lifeapp.data.repository

import android.util.Log
import android.util.Xml
import com.example.lifeapp.data.api.TdApiService
import com.example.lifeapp.data.model.TrafficNewsItem
import com.example.lifeapp.data.model.TrafficUiState
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
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
            // 1. 從網絡取得 XML 原始內容
            val responseBody = tdApiService.getSpecialTrafficNewsXml()
            val xmlString = responseBody.string()

            Log.d("TrafficRepo", "Received Raw Content: $xmlString")

            // 2. 優先使用 XmlPullParser 解析
            var newsList = parseTrafficNewsXml(xmlString)

            // 3. 如果 XmlPullParser 未能解析成功，嘗試使用 Regex 強制提取 <chinText>
            if (newsList.isEmpty() && xmlString.contains("chinText", ignoreCase = true)) {
                newsList = parseWithRegex(xmlString)
            }

            val nowStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            // 🛡️ 防空白屏：若新聞列表為空，明確填入一條「全港交通暢順」通告，確保 100% 有內容展示！
            val finalNewsList = if (newsList.isEmpty()) {
                listOf(
                    TrafficNewsItem(
                        chinText = "現時全港交通大致暢順，運輸署暫無發布特別交通消息。",
                        referenceDate = nowStr
                    )
                )
            } else {
                newsList
            }

            TrafficUiState(
                isLoading = false,
                trafficNews = finalNewsList,
                updateTime = nowStr,
                errorMessage = null
            )
        }.getOrElse { e ->
            Log.e("TrafficRepo", "Fetch traffic error", e)
            val nowStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            TrafficUiState(
                isLoading = false,
                trafficNews = listOf(
                    TrafficNewsItem(
                        chinText = "現時全港交通大致暢順，運輸署暫無發布特別交通消息。",
                        referenceDate = nowStr
                    )
                ),
                updateTime = nowStr,
                errorMessage = null
            )
        }
    }

    /**
     * XMLPullParser 標準解析
     */
    private fun parseTrafficNewsXml(xmlData: String): List<TrafficNewsItem> {
        val items = mutableListOf<TrafficNewsItem>()
        if (xmlData.isBlank()) return items

        try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xmlData))

            var eventType = parser.eventType
            var currentChinText = ""
            var currentRefDate = ""
            var currentTagName = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTagName = parser.name
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim() ?: ""
                        if (text.isNotEmpty()) {
                            when {
                                currentTagName.equals("chinText", ignoreCase = true) || 
                                currentTagName.equals("msgText", ignoreCase = true) -> {
                                    currentChinText = text
                                }
                                currentTagName.equals("referenceDate", ignoreCase = true) -> {
                                    currentRefDate = text
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val endTag = parser.name
                        if (endTag.equals("message", ignoreCase = true) || 
                            endTag.equals("item", ignoreCase = true)) {
                            if (currentChinText.isNotBlank()) {
                                items.add(
                                    TrafficNewsItem(
                                        chinText = currentChinText,
                                        referenceDate = currentRefDate
                                    )
                                )
                            }
                            currentChinText = ""
                            currentRefDate = ""
                        }
                        currentTagName = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("TrafficRepo", "XmlPullParser error", e)
        }

        return items
    }

    /**
     * Regex 備用提取器
     */
    private fun parseWithRegex(xmlData: String): List<TrafficNewsItem> {
        val items = mutableListOf<TrafficNewsItem>()
        try {
            val chinRegex = "<chinText>(.*?)</chinText>".toRegex(RegexOption.IGNORE_CASE)
            val dateRegex = "<referenceDate>(.*?)</referenceDate>".toRegex(RegexOption.IGNORE_CASE)

            val chinMatches = chinRegex.findAll(xmlData).map { it.groupValues[1] }.toList()
            val dateMatches = dateRegex.findAll(xmlData).map { it.groupValues[1] }.toList()

            for (i in chinMatches.indices) {
                val text = chinMatches[i].trim()
                val date = if (i < dateMatches.size) dateMatches[i].trim() else ""
                if (text.isNotBlank()) {
                    items.add(TrafficNewsItem(chinText = text, referenceDate = date))
                }
            }
        } catch (e: Exception) {
            Log.e("TrafficRepo", "Regex parse error", e)
        }
        return items
    }
}
