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

            // 2. 解析 XML Data (對齊運輸署 XSD 格式)
            val newsList = parseTrafficNewsXml(xmlString)

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

    /**
     * 使用 Android 內置輕量 XmlPullParser 解析運輸署 specialtrafficnews.xml
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

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("chinText", ignoreCase = true) || tagName.equals("msgText", ignoreCase = true)) {
                            currentChinText = parser.nextText()
                        } else if (tagName.equals("referenceDate", ignoreCase = true)) {
                            currentRefDate = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("message", ignoreCase = true)) {
                            if (currentChinText.isNotBlank()) {
                                items.add(
                                    TrafficNewsItem(
                                        chinText = currentChinText.trim(),
                                        referenceDate = currentRefDate.trim()
                                    )
                                )
                            }
                            currentChinText = ""
                            currentRefDate = ""
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("TrafficRepo", "XML Parse error", e)
        }

        return items
    }
}
