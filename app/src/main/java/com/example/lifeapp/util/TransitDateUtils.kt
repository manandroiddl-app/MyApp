package com.example.lifeapp.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TransitDateUtils {

    private const val FIVE_HOURS_IN_MILLIS = 5 * 60 * 60 * 1000L

    /**
     * 計算當前時間 -5 小時後的 Data Version 字串 (格式: yyyyMMdd)
     */
    fun calculateVersion(currentTimeMillis: Long = System.currentTimeMillis()): String {
        val targetTime = currentTimeMillis - FIVE_HOURS_IN_MILLIS
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        return sdf.format(Date(targetTime))
    }

    /**
     * 將資料庫最後更新時間 (Millis) 格式化為 UI 顯示字串 (格式: yyyy-MM-dd HH:mm)
     */
    fun formatLastUpdateTime(lastUpdateTimeMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        return sdf.format(Date(lastUpdateTimeMillis))
    }
}
