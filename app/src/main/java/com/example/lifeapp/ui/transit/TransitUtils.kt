package com.example.lifeapp.ui.transit

import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitStop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCompanyDisplayName(company: Any?): String {
    val compStr = when (company) {
        null -> ""
        is String -> company
        else -> company.toString()
    }

    return when (compStr.uppercase()) {
        "KMB" -> "九巴"
        "CTB" -> "城巴"
        "NWFB" -> "新巴"
        "NLB" -> "嶼巴"
        "MTR" -> "港鐵"
        "GMB" -> "專線小巴"
        else -> compStr
    }
}

fun formatEtaDisplay(eta: TransitEta): String {
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

data class TrackedChainResult(
    val chainMap: Map<String, Pair<TransitEta, Boolean>> = emptyMap(),
    val effectiveHeadStopId: String? = null
)

fun calculateTrackedChain(
    routeStops: List<TransitStop>,
    selectedStopEtaMap: Map<String, List<TransitEta>>,
    tracked: TrackedVehicleInfo?
): TrackedChainResult {
    if (tracked == null || routeStops.isEmpty()) {
        return TrackedChainResult()
    }

    val sortedStops = routeStops.sortedBy { it.sequence }
    val targetTimestampMillis = parseIsoTimestampToMillis(tracked.etaTimestamp)

    val targetIndex = sortedStops.indexOfFirst { it.stopId == tracked.stopId }
    val startIndex = if (targetIndex != -1) targetIndex else 0

    val chainMap = mutableMapOf<String, Pair<TransitEta, Boolean>>()
    var effectiveHeadStopId: String? = null

    var lastMatchedTimeMillis = targetTimestampMillis

    for (i in startIndex until sortedStops.size) {
        val stop = sortedStops[i]
        val etas: List<TransitEta> = selectedStopEtaMap[stop.stopId] ?: emptyList()

        val matchedEta = etas.firstOrNull { eta: TransitEta ->
            val etaMillis = parseIsoTimestampToMillis(eta.etaTimestamp)
            val sameSeq = eta.etaSeq == tracked.etaSeq

            if (sameSeq) {
                true
            } else if (lastMatchedTimeMillis != null && etaMillis != null) {
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
