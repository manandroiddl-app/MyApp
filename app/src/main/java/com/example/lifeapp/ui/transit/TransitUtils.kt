package com.example.lifeapp.ui.transit

import com.example.lifeapp.data.model.EtaInfo
import com.example.lifeapp.data.model.RouteStop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCompanyDisplayName(company: String): String {
    return when (company.uppercase()) {
        "KMB" -> "九巴"
        "CTB" -> "城巴"
        "NWFB" -> "新巴"
        "NLB" -> "嶼巴"
        "MTR" -> "港鐵"
        "GMB" -> "專線小巴"
        else -> company
    }
}

fun formatEtaDisplay(eta: EtaInfo): String {
    val mins = eta.getEtaMinutes()
    val remarkStr = if (!eta.rmkZh.isNullAsStringEmpty()) " (${eta.rmkZh})" else ""

    if (mins != null) {
        val minText = when {
            mins <= 0 -> "即將到達"
            else -> "${mins} 分鐘"
        }
        return "$minText$remarkStr"
    }

    val timestampStr = eta.etaTimestamp
    if (!timestampStr.isNullAsStringEmpty()) {
        val formattedTime = formatTimestampToTimeOnly(timestampStr)
        if (formattedTime.isNotEmpty()) {
            return "$formattedTime$remarkStr"
        }
    }

    return if (remarkStr.isNotEmpty()) remarkStr.trim() else "暫無預計時間"
}

private fun String?.isNullAsStringEmpty(): Boolean {
    if (this == null) return true
    val trimmed = this.trim()
    return trimmed.isEmpty() || trimmed.equals("null", ignoreCase = true)
}

private fun formatTimestampToTimeOnly(timestampStr: String?): String {
    if (timestampStr.isNullAsStringEmpty()) return ""
    return try {
        val cleanStr = timestampStr!!.trim()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = isoFormat.parse(cleanStr)
        if (date != null) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeFormat.format(date)
        } else {
            ""
        }
    } catch (e: Exception) {
        if (timestampStr!!.length >= 16 && timestampStr.contains("T")) {
            val timePart = timestampStr.substringAfter("T").take(5)
            if (timePart.matches(Regex("\\d{2}:\\d{2}"))) {
                return timePart
            }
        }
        ""
    }
}

private fun parseIsoTimestampToMillis(timestampStr: String?): Long? {
    if (timestampStr.isNullAsStringEmpty()) return null
    return try {
        val cleanStr = timestampStr!!.trim()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        isoFormat.parse(cleanStr)?.time
    } catch (e: Exception) {
        null
    }
}

/**
 * 追蹤鏈計算結果結構體
 * @param chainMap 包含所有符合追蹤條件的 (stopId -> Pair(EtaInfo, isEffectiveHead))
 * @param effectiveHeadStopId 當前追蹤鏈中第一個有有效 ETA 的車站 StopId（即目前顯示「取消追蹤」按鈕的車站）
 */
data class TrackedChainResult(
    val chainMap: Map<String, Pair<EtaInfo, Boolean>> = emptyMap(),
    val effectiveHeadStopId: String? = null
)

/**
 * 計算車輛追蹤鏈：
 * 若原追蹤車站 (T0) 的 ETA 已到站過期/消失，自動向下尋找下一個仍包含該班次 ETA 的車站 (T1) 作為 Effective Head。
 */
fun calculateTrackedChain(
    routeStops: List<RouteStop>,
    selectedStopEtaMap: Map<String, List<EtaInfo>>,
    tracked: TrackedVehicleInfo?
): TrackedChainResult {
    if (tracked == null || routeStops.isEmpty()) {
        return TrackedChainResult()
    }

    val sortedStops = routeStops.sortedBy { it.sequence }
    val targetTimestampMillis = parseIsoTimestampToMillis(tracked.etaTimestamp)

    // 1. 尋找追蹤鏈的起點車站索引
    val targetIndex = sortedStops.indexOfFirst { it.stopId == tracked.stopId }
    val startIndex = if (targetIndex != -1) targetIndex else 0

    val chainMap = mutableMapOf<String, Pair<EtaInfo, Boolean>>()
    var effectiveHeadStopId: String? = null

    // 2. 沿著路線車站往下尋找匹配的 ETA 班次
    var lastMatchedTimeMillis = targetTimestampMillis

    for (i in startIndex until sortedStops.size) {
        val stop = sortedStops[i]
        val etas = selectedStopEtaMap[stop.stopId] ?: emptyList()

        val matchedEta = etas.firstOrNull { eta ->
            val etaMillis = parseIsoTimestampToMillis(eta.etaTimestamp)
            val sameSeq = eta.etaSeq == tracked.etaSeq

            if (sameSeq) {
                true
            } else if (lastMatchedTimeMillis != null && etaMillis != null) {
                // 允許 3 分鐘內的合理時間差漂移比對
                val diffMins = (etaMillis - lastMatchedTimeMillis!!) / (1000 * 60)
                diffMins in -3..15
            } else {
                false
            }
        }

        if (matchedEta != null) {
            val matchedMillis = parseIsoTimestampToMillis(matchedEta.etaTimestamp)
            if (matchedMillis != null) {
                lastMatchedTimeMillis = matchedMillis
            }

            // 第一個找到有效 ETA 的車站，即為當前的 Effective Head（最新前端車站）
            val isHead = (effectiveHeadStopId == null)
            if (isHead) {
                effectiveHeadStopId = stop.stopId
            }

            chainMap[stop.stopId] = Pair(matchedEta, isHead)
        }
    }

    return TrackedChainResult(
        chainMap = chainMap,
        effectiveHeadStopId = effectiveHeadStopId
    )
}
