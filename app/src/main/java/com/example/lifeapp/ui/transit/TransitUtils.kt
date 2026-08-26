package com.example.lifeapp.ui.transit

import com.example.lifeapp.data.model.OperatorCompany
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * 計算 ISO 8601 時間字串與當前時間的相差分鐘數
 */
fun getEtaMinutes(etaTimestamp: String?): Int? {
    if (etaTimestamp.isNullOrEmpty()) return null
    return try {
        val etaTime = ZonedDateTime.parse(etaTimestamp)
        val now = ZonedDateTime.now()
        val duration = Duration.between(now, etaTime)
        val minutes = duration.toMinutes().toInt()
        if (minutes < 0) 0 else minutes
    } catch (e: Exception) {
        null
    }
}

/**
 * 格式化倒數時間顯示 (例: "即將到站", "5 分鐘")
 */
fun formatEtaDisplay(etaMinutes: Int?, remark: String?): String {
    if (etaMinutes == null) {
        return remark ?: "暫無資料"
    }
    return when {
        etaMinutes <= 0 -> "即將到站"
        else -> "$etaMinutes 分鐘"
    }
}

/**
 * 將 ISO 8601 時間字串格式化為精確時刻 [HH:mm] (例: "23:53", "00:03")
 */
fun formatEtaTimeClock(etaTimestamp: String?): String {
    if (etaTimestamp.isNullOrEmpty()) return "--:--"
    return try {
        val etaTime = ZonedDateTime.parse(etaTimestamp)
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        etaTime.format(formatter)
    } catch (e: Exception) {
        "--:--"
    }
}

/**
 * 轉換公司名稱顯示
 */
fun formatCompanyDisplayName(company: OperatorCompany): String {
    return when (company) {
        OperatorCompany.KMB -> "九巴"
        OperatorCompany.CTB -> "城巴"
        OperatorCompany.NLB -> "嶼巴"
        OperatorCompany.GMB -> "專線小巴"
        OperatorCompany.MTR -> "港鐵"
        OperatorCompany.FERRY -> "渡輪"
    }
}
