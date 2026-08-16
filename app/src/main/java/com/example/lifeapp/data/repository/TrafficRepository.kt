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

            Log.d("TrafficRepo", "Received XML Length: ${xmlString.length}")

            // 2. 解析 XML Data (增強對齊與容錯)
            val newsList = parseTrafficNewsXml(xmlString)

            val nowStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            TrafficUiState(
                isLoading = false,
                trafficNews = newsList,
                updateTime = nowStr,
                errorMessage = if (newsList.isEmpty()) "目前全港交通暢順，沒有特別交通消息" else null
            )
        }.getOrElse { e ->
            Log.e("TrafficRepo", "Fetch traffic error", e)
            TrafficUiState(
                isLoading = false,
                errorMessage = "無法獲取特別交通消息：${e.localizedMessage}"
            )
        }
    }

    /**
     * 強化版 XmlPullParser：支援大小寫不敏感匹配與多種標籤名 (chinText / ChinText / msgText)
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
                                currentTagName.equals("msgText", ignoreCase = true) ||
                                currentTagName.equals("chin_text", ignoreCase = true) -> {
                                    currentChinText = text
                                }
                                currentTagName.equals("referenceDate", ignoreCase = true) ||
                                currentTagName.equals("refDate", ignoreCase = true) -> {
                                    currentRefDate = text
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val endTag = parser.name
                        // 當遇到每條消息的結尾標籤（message 或 item 或 Row）時組合物件
                        if (endTag.equals("message", ignoreCase = true) || 
                            endTag.equals("item", ignoreCase = true) ||
                            endTag.equals("trafficnews", ignoreCase = true)) {
                            
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
            Log.e("TrafficRepo", "XML Parse error", e)
        }

        Log.d("TrafficRepo", "Parsed news count: ${items.size}")
        return items
    }
}
