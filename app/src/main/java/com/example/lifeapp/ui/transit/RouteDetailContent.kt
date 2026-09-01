package com.example.lifeapp.ui.transit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitStop
import com.example.lifeapp.ui.theme.PrimaryDarkBlue
import java.time.ZonedDateTime

/**
 * 封裝追蹤運算後的結果
 */
private data class TrackingCalculationResult(
    val trackedEtaMap: Map<String, String>,      // StopId -> Highlighted EtaTimestamp
    val activeBaseStopId: String?,               // 當前最新有效的起點車站 ID
    val activeBaseEtaTimestamp: String?          // 當前最新有效的起點 ETA Timestamp
)

@Composable
fun RouteDetailContent(
    uiState: TransitUiState,
    onBackClick: () -> Unit,
    onToggleBookmark: (TransitStop) -> Unit,
    onToggleTrackVehicle: (String, Int, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val route = uiState.selectedRoute ?: return

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (uiState.isLoadingStops) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.routeStops.isEmpty()) {
            Text(
                text = "暫無路線車站資料",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // 計算跨車站鏈式追蹤地圖（支援車輛到站後自動順移起點）
            val trackingResult = calculateTrackedEtaMap(
                routeStops = uiState.routeStops,
                stopEtaMap = uiState.selectedStopEtaMap,
                trackedVehicle = uiState.trackedVehicle
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = uiState.routeStops,
                    key = { stop -> stop.stopId }
                ) { stop ->
                    val etas = uiState.selectedStopEtaMap[stop.stopId] ?: emptyList()

                    // 生成與 ViewModel 一致的 Bookmark ID 進行比對（支援動態營運商前綴如 CTB / KMB）
                    val expectedBookmarkId = "${route.company.name}_${route.routeName}_${route.bound ?: "O"}_${route.serviceType ?: "1"}_${stop.stopId}"
                    val isBookmarked = uiState.bookmarkedStopIds.contains(expectedBookmarkId)

                    StopDetailItem(
                        stop = stop,
                        etas = etas,
                        isBookmarked = isBookmarked,
                        trackedVehicle = uiState.trackedVehicle,
                        trackedEtaTimestamp = trackingResult.trackedEtaMap[stop.stopId],
                        activeBaseStopId = trackingResult.activeBaseStopId,
                        activeBaseEtaTimestamp = trackingResult.activeBaseEtaTimestamp,
                        onBookmarkClick = { onToggleBookmark(stop) },
                        onTrackVehicleClick = { eta ->
                            onToggleTrackVehicle(stop.stopId, stop.sequence, eta.etaTimestamp)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StopDetailItem(
    stop: TransitStop,
    etas: List<TransitEta>,
    isBookmarked: Boolean,
    trackedVehicle: TrackedVehicleInfo?,
    trackedEtaTimestamp: String?,
    activeBaseStopId: String?,
    activeBaseEtaTimestamp: String?,
    onBookmarkClick: () -> Unit,
    onTrackVehicleClick: (TransitEta) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 車站名稱列與書籤按鈕
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stop.nameZh ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDarkBlue
                    )
                    if (!stop.nameEn.isNullOrEmpty()) {
                        Text(
                            text = stop.nameEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "書籤",
                        tint = PrimaryDarkBlue
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFEEEEEE)
            )

            // 檢查是否有任何有效的時間戳記或 API 回傳的 Remark (例如 "尾班車已開出")
            val hasAnyContent = etas.any { !it.etaTimestamp.isNullOrEmpty() || !it.remarkZh.isNullOrEmpty() }

            // ETA 資訊 Layout
            if (!hasAnyContent) {
                // 完全沒有任何時間與 Remark 時，顯示預設空狀態提示（不顯示追蹤 Icon）
                Text(
                    text = "暫無到站時間數據",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5C6BC0)
                )
            } else {
                etas.forEach { eta ->
                    val hasValidEtaTime = !eta.etaTimestamp.isNullOrEmpty()
                    val etaMinutes = getEtaMinutes(eta.etaTimestamp)
                    val clockTime = formatEtaTimeClock(eta.etaTimestamp)

                    // 判斷是否為當前最前方的「有效追蹤起點」 Icon 點
                    val isActiveBaseTrackedIcon = activeBaseStopId == stop.stopId &&
                            activeBaseEtaTimestamp == eta.etaTimestamp

                    // 判斷是否屬於「追蹤目標班次」（包含起點站與鏈式比對到的下游車站）
                    val isHighlighted = trackedEtaTimestamp != null && trackedEtaTimestamp == eta.etaTimestamp

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左側區塊：倒數分鐘/API 備註 + 格仔旗與到達時刻
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // 倒數分鐘 (例如 "5 分鐘" / "即將到站" / API 官方備註如 "尾班車已開出")
                            Text(
                                text = formatEtaDisplay(etaMinutes, eta.remarkZh),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isHighlighted) {
                                    Color(0xFF0D47A1)
                                } else if (etaMinutes != null && etaMinutes <= 3) {
                                    Color(0xFFD32F2F)
                                } else {
                                    Color(0xFF0D47A1)
                                }
                            )

                            // 只有在存在有效 ETA 時間時才顯示時刻 Card (若僅有 "尾班車已開出" 則不顯示時刻)
                            if (hasValidEtaTime) {
                                Spacer(modifier = Modifier.width(8.dp))

                                // 精確到達時刻與格仔旗 Icon (底色改為白色，被追蹤時維持深藍底)
                                Surface(
                                    color = if (isHighlighted) Color(0xFF0D47A1) else Color.White,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SportsScore,
                                            contentDescription = "到達時刻",
                                            tint = if (isHighlighted) Color.White else Color(0xFF1565C0),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = clockTime,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isHighlighted) Color.White else Color(0xFF1565C0)
                                        )
                                    }
                                }
                            }
                        }

                        // 右側：追蹤車輛 Icon
                        // 條件：必須存在有效 ETA 時間才允許顯示追蹤按鈕 (例如 "尾班車已開出" 無時間時不顯示 Icon)
                        if (hasValidEtaTime) {
                            // 未追蹤時全顯示；已追蹤時，僅在「當前最新有效起點」顯示 Icon 供取消控制，其餘 Hide
                            if (trackedVehicle == null || isActiveBaseTrackedIcon) {
                                IconButton(
                                    onClick = { onTrackVehicleClick(eta) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isActiveBaseTrackedIcon) Icons.Filled.MyLocation else Icons.Outlined.MyLocation,
                                        contentDescription = "追蹤車輛",
                                        tint = if (isActiveBaseTrackedIcon) Color(0xFF1976D2) else Color(0xFF78909C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                // 當其他 Icon 隱藏時保持空位防止 Layout 跳動
                                Spacer(modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 計算全路線被追蹤車輛的跨車站鏈式 (Chain Reaction) ETA 時間表
 * 支援舊起點站到站離去後，自動順移起點至下一個下游相關車站
 */
private fun calculateTrackedEtaMap(
    routeStops: List<TransitStop>,
    stopEtaMap: Map<String, List<TransitEta>>,
    trackedVehicle: TrackedVehicleInfo?
): TrackingCalculationResult {
    if (trackedVehicle == null || trackedVehicle.etaTimestamp.isNullOrEmpty()) {
        return TrackingCalculationResult(emptyMap(), null, null)
    }

    val resultMap = mutableMapOf<String, String>()
    
    // 檢查原本記錄的起點車站與 ETA 時間是否依然有效（未到站完結）
    val originalStopEtas = stopEtaMap[trackedVehicle.stopId] ?: emptyList()
    val isOriginalStillValid = originalStopEtas.any { it.etaTimestamp == trackedVehicle.etaTimestamp }

    var activeBaseStopId: String? = null
    var activeBaseEtaTimestamp: String? = null
    var lastValidTime: ZonedDateTime? = null

    if (isOriginalStillValid) {
        // 情況 1: 原始起點站班車尚未離站，以原始點作為有效起點
        activeBaseStopId = trackedVehicle.stopId
        activeBaseEtaTimestamp = trackedVehicle.etaTimestamp
        resultMap[trackedVehicle.stopId] = trackedVehicle.etaTimestamp
        lastValidTime = parseZonedDateTime(trackedVehicle.etaTimestamp)
    }

    // 依序掃描車站列表（尋找新起點或進行連鎖推進）
    routeStops.forEach { stop ->
        if (stop.sequence > trackedVehicle.stopSequence) {
            val etas = stopEtaMap[stop.stopId] ?: emptyList()

            if (lastValidTime == null) {
                // 情況 2: 原始起點站已離站，順移尋找下一個下游的第一班符合條件車輛作為「新起點」
                val initialBaseTime = parseZonedDateTime(trackedVehicle.etaTimestamp)
                if (initialBaseTime != null) {
                    val matchedNewBase = etas.mapNotNull { eta ->
                        val timestamp = eta.etaTimestamp
                        if (timestamp != null) {
                            val time = parseZonedDateTime(timestamp)
                            if (time != null) Triple(eta, timestamp, time) else null
                        } else null
                    }.filter { (_, _, time) ->
                        !time.isBefore(initialBaseTime)
                    }.minByOrNull { (_, _, time) ->
                        time.toInstant().toEpochMilli()
                    }

                    if (matchedNewBase != null) {
                        activeBaseStopId = stop.stopId
                        activeBaseEtaTimestamp = matchedNewBase.second
                        resultMap[stop.stopId] = matchedNewBase.second
                        lastValidTime = matchedNewBase.third
                    }
                }
            } else {
                // 進行鏈式連鎖遞推 (Chain Reaction)
                val currentBaseTime = lastValidTime ?: return@forEach
                val matchedEta = etas.mapNotNull { eta ->
                    val timestamp = eta.etaTimestamp
                    if (timestamp != null) {
                        val time = parseZonedDateTime(timestamp)
                        if (time != null) Triple(eta, timestamp, time) else null
                    } else null
                }.filter { (_, _, time) ->
                    !time.isBefore(currentBaseTime)
                }.minByOrNull { (_, _, time) ->
                    time.toInstant().toEpochMilli()
                }

                if (matchedEta != null) {
                    resultMap[stop.stopId] = matchedEta.second
                    lastValidTime = matchedEta.third
                }
            }
        }
    }

    return TrackingCalculationResult(
        trackedEtaMap = resultMap,
        activeBaseStopId = activeBaseStopId,
        activeBaseEtaTimestamp = activeBaseEtaTimestamp
    )
}

private fun parseZonedDateTime(timeStr: String?): ZonedDateTime? {
    if (timeStr.isNullOrEmpty()) return null
    return try {
        ZonedDateTime.parse(timeStr)
    } catch (_: Exception) {
        null
    }
}
