package com.example.lifeapp.ui.transit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailContent(
    uiState: TransitUiState,
    onBackClick: () -> Unit,
    onToggleBookmark: (TransitStop) -> Unit,
    onToggleTrackVehicle: (String, Int, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val route = uiState.selectedRoute ?: return

    // 在 UI 層獨立管理預設展開的第一個車站與使用者手動展開的車站 ID
    var expandedStopId by remember(uiState.routeStops) {
        mutableStateOf(uiState.routeStops.firstOrNull()?.stopId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = route.routeName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "往 ${route.destinationZh ?: ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                        if (!route.originZh.isNullOrEmpty()) {
                            Text(
                                text = "由 ${route.originZh} 開出",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                    itemsIndexed(
                        items = uiState.routeStops,
                        key = { _, stop -> stop.stopId }
                    ) { index, stop ->
                        val isFirst = index == 0
                        val isLast = index == uiState.routeStops.size - 1
                        val etas = uiState.selectedStopEtaMap[stop.stopId] ?: emptyList()

                        // 生成與 ViewModel 一致的 Bookmark ID 進行比對
                        val expectedBookmarkId = "KMB_${route.routeName}_${route.bound ?: "O"}_${route.serviceType ?: "1"}_${stop.stopId}"
                        val isBookmarked = uiState.bookmarkedStopIds.contains(expectedBookmarkId)

                        val isExpanded = expandedStopId == stop.stopId

                        StopDetailItem(
                            stop = stop,
                            etas = etas,
                            isFirst = isFirst,
                            isLast = isLast,
                            isExpanded = isExpanded,
                            isBookmarked = isBookmarked,
                            trackedVehicle = uiState.trackedVehicle,
                            onItemClick = {
                                expandedStopId = if (isExpanded) null else stop.stopId
                            },
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
}

@Composable
private fun StopDetailItem(
    stop: TransitStop,
    etas: List<TransitEta>,
    isFirst: Boolean,
    isLast: Boolean,
    isExpanded: Boolean,
    isBookmarked: Boolean,
    trackedVehicle: TrackedVehicleInfo?,
    onItemClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onTrackVehicleClick: (TransitEta) -> Unit
) {
    val firstEta = etas.firstOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 左側：路線圖連線與站號球
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(16.dp)
                    .background(if (isFirst) Color.Transparent else MaterialTheme.colorScheme.primary)
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${stop.sequence}",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxSize()
                    .weight(1f, fill = false)
                    .background(if (isLast) Color.Transparent else MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 右側：車站資訊與到站時間卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stop.nameZh ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!stop.nameEn.isNullOrEmpty()) {
                            Text(
                                text = stop.nameEn,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBookmarkClick) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "書籤",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "展開/收起",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 未展開時顯示第一班 ETA 簡要資訊
                if (!isExpanded && firstEta != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val etaMinutes = getEtaMinutes(firstEta.etaTimestamp)
                        Text(
                            text = formatEtaDisplay(etaMinutes, firstEta.rmkZh),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 展開時顯示詳細 ETA 清單
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        if (etas.isEmpty()) {
                            Text(
                                text = "暫無到站時間數據",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            etas.forEachIndexed { etaIndex, eta ->
                                val etaMinutes = getEtaMinutes(eta.etaTimestamp)
                                val isTracked = trackedVehicle?.stopId == stop.stopId &&
                                        trackedVehicle.etaTimestamp == eta.etaTimestamp

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsBus,
                                            contentDescription = "巴士",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "班次 ${eta.etaSeq ?: (etaIndex + 1)}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatEtaDisplay(etaMinutes, eta.rmkZh),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (etaMinutes != null && etaMinutes <= 3) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // 追蹤車輛按鈕
                                        IconButton(
                                            onClick = { onTrackVehicleClick(eta) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isTracked) Icons.Filled.MyLocation else Icons.Outlined.MyLocation,
                                                contentDescription = "追蹤車輛",
                                                tint = if (isTracked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
