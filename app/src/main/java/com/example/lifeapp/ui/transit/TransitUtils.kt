package com.example.lifeapp.ui.transit

import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitStop
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 解析 ISO 時間字串為 Epoch 毫秒值
 */
private fun parseEtaMillis(etaTimestamp: String?): Long? {
    if (etaTimestamp.isNullOrEmpty()) return null
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        sdf.parse(etaTimestamp)?.time
    } catch (_: Exception) { null }
}

/**
 * 判斷該 ETA 是否為可追蹤的有效班次
 */
fun isValidTrackableEta(eta: TransitEta): Boolean {
    if (eta.minutesLeft == null) return false
    if (eta.etaTimestamp.isNullOrEmpty()) return false
    return true
}

/**
 * 格式化 ETA 顯示文字 (例: "5 分鐘 (14:30)")
 */
fun formatEtaDisplay(eta: TransitEta): String {
    val mins = eta.minutesLeft

    val formattedTime = if (!eta.etaTimestamp.isNullOrEmpty()) {
        try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val date = sdfInput.parse(eta.etaTimestamp)
            if (date != null) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            } else null
        } catch (_: Exception) { null }
    } else null

    val clockString = formattedTime ?: run {
        if (mins != null) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MINUTE, mins)
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)
        } else ""
    }

    val etaText = when {
        mins == null -> eta.remarkZh.orEmpty().ifEmpty { "暫無班次" }
        mins <= 0 -> "即將到達"
        else -> "${mins} 分鐘"
    }

    return if (clockString.isNotEmpty()) {
        "$etaText ($clockString)"
    } else {
        etaText
    }
}

/**
 * 轉換巴士公司顯示名稱
 */
fun formatCompanyDisplayName(company: Any?): String {
    val companyStr = when (company) {
        is Enum<*> -> company.name
        else -> company?.toString()
    }

    return when (companyStr?.uppercase()) {
        "KMB" -> "九巴"
        "CTB" -> "城巴"
        "NWFB" -> "新巴"
        "GMB" -> "綠色小巴"
        "NLB" -> "嶼巴"
        "MTR" -> "港鐵巴士"
        else -> companyStr.orEmpty().ifEmpty { "巴士" }
    }
}

/**
 * 計算被追蹤車輛在下游各站對應的 ETA 鏈結
 */
fun calculateTrackedChain(
    routeStops: List<TransitStop>,
    stopEtaMap: Map<String, List<TransitEta>>,
    tracked: TrackedVehicleInfo?
): Map<String, TransitEta> {
    if (tracked == null) return emptyMap()

    val resultMap = mutableMapOf<String, TransitEta>()
    val trackedMillis = parseEtaMillis(tracked.etaTimestamp) ?: return emptyMap()

    val downstreamStops = routeStops
        .filter { it.sequence >= tracked.stopSequence }
        .sortedBy { it.sequence }

    var currentBaseMillis = trackedMillis

    for (stop in downstreamStops) {
        if (stop.stopId == tracked.stopId) {
            val targetEta = stopEtaMap[stop.stopId]?.find { it.etaTimestamp == tracked.etaTimestamp }
            if (targetEta != null) {
                resultMap[stop.stopId] = targetEta
            }
            continue
        }

        val etaList = stopEtaMap[stop.stopId] ?: emptyList()
        val matchedEta = etaList
            .mapNotNull { eta ->
                val etaTime = parseEtaMillis(eta.etaTimestamp) ?: return@mapNotNull null
                val diff = etaTime - currentBaseMillis
                if (diff >= 0) Pair(eta, etaTime) else null
            }
            .minByOrNull { it.second - currentBaseMillis }

        if (matchedEta != null) {
            resultMap[stop.stopId] = matchedEta.first
            currentBaseMillis = matchedEta.second
        } else {
            break
        }
    }

    return resultMap
}
