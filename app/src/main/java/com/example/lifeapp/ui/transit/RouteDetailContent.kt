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
import androidx.compose.material3.Divider
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
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = uiState.routeStops,
                    key = { stop -> stop.stopId }
                ) { stop ->
                    val etas = uiState.selectedStopEtaMap[stop.stopId] ?: emptyList()

                    // 生成與 ViewModel 一致的 Bookmark ID 進行比對
                    val expectedBookmarkId = "KMB_${route.routeName}_${route.bound ?: "O"}_${route.serviceType ?: "1"}_${stop.stopId}"
                    val isBookmarked = uiState.bookmarkedStopIds.contains(expectedBookmarkId)

                    StopDetailItem(
                        stop = stop,
                        etas = etas,
                        isBookmarked = isBookmarked,
                        trackedVehicle = uiState.trackedVehicle,
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
    onBookmarkClick: () -> Unit,
    onTrackVehicleClick: (TransitEta) -> Unit
) {
    // 1) 車站卡片改用藍色系背景
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD) // 柔和淺藍色背景
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
                        color = Color(0xFF0D47A1)
                    )
                    if (!stop.nameEn.isNullOrEmpty()) {
                        Text(
                            text = stop.nameEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1565C0)
                        )
                    }
                }

                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "書籤",
                        tint = if (isBookmarked) Color(0xFF1976D2) else Color(0xFF5C6BC0)
                    )
                }
            }

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFBBDEFB)
            )

            // 2) ETA 資訊 Layout 調整 (左: 倒數分鐘 + 🏁 到達時刻 | 右: 追蹤 Icon)
            if (etas.isEmpty()) {
                Text(
                    text = "暫無到站時間數據",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5C6BC0)
                )
            } else {
                etas.forEach { eta ->
                    val etaMinutes = getEtaMinutes(eta.etaTimestamp)
                    val clockTime = formatEtaTimeClock(eta.etaTimestamp)
                    val isTracked = trackedVehicle?.stopId == stop.stopId &&
                            trackedVehicle.etaTimestamp == eta.etaTimestamp

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左側區塊：倒數分鐘 + 格仔旗與到達時刻
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // 倒數分鐘 (例如 "5 分鐘" / "即將到站")
                            Text(
                                text = formatEtaDisplay(etaMinutes, eta.remarkZh),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (etaMinutes != null && etaMinutes <= 3) Color(0xFFD32F2F) else Color(0xFF0D47A1)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // 精確到達時刻與格仔旗 Icon
                            Surface(
                                color = Color(0xFFBBDEFB),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SportsScore,
                                        contentDescription = "到達時刻",
                                        tint = Color(0xFF1565C0),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = clockTime,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1565C0)
                                    )
                                }
                            }
                        }

                        // 右側：追蹤車輛 Icon
                        IconButton(
                            onClick = { onTrackVehicleClick(eta) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isTracked) Icons.Filled.MyLocation else Icons.Outlined.MyLocation,
                                contentDescription = "追蹤車輛",
                                tint = if (isTracked) Color(0xFF1976D2) else Color(0xFF78909C),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
