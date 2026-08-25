package com.example.lifeapp.ui.transit

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 計算 ISO Timestamp 距離現在還有多少分鐘
 */
fun getEtaMinutes(etaTimestamp: String?): Long? {
    if (etaTimestamp.isNullOrEmpty()) return null
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("Asia/Hong_Kong")
        val etaDate = sdf.parse(etaTimestamp) ?: return null
        val diffMs = etaDate.time - System.currentTimeMillis()
        val minutes = diffMs / (1000 * 60)
        if (minutes < 0) 0 else minutes
    } catch (_: Exception) {
        null
    }
}

/**
 * 格式化 ETA 顯示文字（處理「即將到站」、備註如「最後班次」等）
 */
fun formatEtaDisplay(etaMinutes: Long?, rmkZh: String?): String {
    if (!rmkZh.isNullOrEmpty() && rmkZh != "原定班次" && rmkZh != "預算班次") {
        return rmkZh
    }
    return when (etaMinutes) {
        null -> "暫無數據"
        0L -> "即將到站"
        else -> "${etaMinutes} 分鐘"
    }
}
