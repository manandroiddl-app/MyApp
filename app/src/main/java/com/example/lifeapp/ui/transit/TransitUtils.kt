package com.example.lifeapp.ui.transit

import com.example.lifeapp.data.model.OperatorCompany
import com.example.lifeapp.data.model.TransitEta
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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

// 新增：支援直接傳入 TransitEta 物件
fun formatEtaDisplay(eta: TransitEta?): String {
    if (eta == null) return "暫無數據"
    val minutes = getEtaMinutes(eta.etaTimestamp)
    return formatEtaDisplay(minutes, eta.rmkZh)
}

// 新增：公開且同時支援 Enum 與 String 傳入的名稱轉換
fun formatCompanyDisplayName(company: Any?): String {
    val name = when (company) {
        is OperatorCompany -> company.name
        else -> company?.toString() ?: ""
    }
    return when (name.uppercase()) {
        "KMB" -> "九巴"
        "CTB" -> "城巴"
        "NLB" -> "嶼巴"
        "LRTFE", "MTR" -> "港鐵巴士"
        else -> "巴士"
    }
}
