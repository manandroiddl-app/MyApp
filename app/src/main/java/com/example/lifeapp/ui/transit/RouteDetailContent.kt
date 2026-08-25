package com.example.lifeapp.ui.transit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifeapp.ui.common.FullPageLoading
import com.example.lifeapp.ui.theme.PrimaryDarkBlue

private val BluePrimary = Color(0xFF1976D2)
private val BlueContainer = Color(0xFFE3F2FD)

@Composable
fun RouteDetailContent(
    uiState: TransitUiState,
    viewModel: TransitSearchViewModel
) {
    if (uiState.isLoadingStops) {
        FullPageLoading()
    } else {
        val tracked = uiState.trackedVehicle

        // 計算追蹤鏈與 Effective Head Stop
        val trackedChainResult = remember(uiState.routeStops, uiState.selectedStopEtaMap, tracked) {
            calculateTrackedChain(uiState.routeStops, uiState.selectedStopEtaMap, tracked)
        }

        val chainMap = trackedChainResult.chainMap
        val effectiveHeadStopId = trackedChainResult.effectiveHeadStopId

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(
                items = uiState.routeStops,
                key = { _, stop -> stop.stopId }
            ) { index, stop ->
                val isExpanded = uiState.expandedStopIds.contains(stop.stopId)
                val isBookmarked = uiState.bookmarks.any { it.stopId == stop.stopId && it.routeId == uiState.selectedRoute?.routeId }
                val etaList = uiState.selectedStopEtaMap[stop.stopId]

                // 車站是否在追蹤鏈中 (黃色 Highlight)
                val trackedPair = chainMap[stop.stopId]
                val isTrackedInChain = trackedPair != null
                val trackedEtaInfo = trackedPair?.first

                // 🎯 核心修復：當該站是當前有效追蹤鏈的第一個車站 (Effective Head) 時顯示「取消追蹤」按鈕
                val showCancelTrackButton = (tracked != null && stop.stopId == effectiveHeadStopId)

                val cardBgColor = if (isTrackedInChain) {
                    Color(0xFFFFF9C4) // 黃色 Highlight
                } else {
                    Color.White
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleStopExpanded(stop.stopId) }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(BlueContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDarkBlue
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = stop.nameZh,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDarkBlue,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = { viewModel.toggleBookmark(stop) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (isBookmarked) Color(0xFFFFC107) else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 若此站屬於追蹤鏈，預覽顯示受追蹤班次 ETA
                        if (isTrackedInChain && trackedEtaInfo != null && !isExpanded) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsBus,
                                    contentDescription = null,
                                    tint = Color(0xFFF57F17),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "追蹤班次：${formatEtaDisplay(trackedEtaInfo)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF57F17)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                HorizontalDivider(color = Color(0xFFEEEEEE))
                                Spacer(modifier = Modifier.height(8.dp))

                                if (etaList == null) {
                                    Text(
                                        text = "載入班次中...",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                } else if (etaList.isEmpty()) {
                                    Text(
                                        text = "暫無預計班次",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                } else {
                                    etaList.forEach { eta ->
                                        val isThisEtaTracked = isTrackedInChain &&
                                                trackedEtaInfo?.etaTimestamp == eta.etaTimestamp &&
                                                trackedEtaInfo?.etaSeq == eta.etaSeq

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = formatEtaDisplay(eta),
                                                fontSize = 14.sp,
                                                fontWeight = if (isThisEtaTracked) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isThisEtaTracked) Color(0xFFE65100) else BluePrimary,
                                                modifier = Modifier.weight(1f)
                                            )

                                            // 🎯 按鈕邏輯：若為 Effective Head 車站且正在追蹤此班次，顯示「取消追蹤」
                                            if (isThisEtaTracked && showCancelTrackButton) {
                                                Button(
                                                    onClick = { viewModel.toggleTrackVehicle(stop, eta) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.GpsOff,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("取消追蹤", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else if (tracked == null) {
                                                // 尚未追蹤任何車輛時，顯示「追蹤此班」
                                                OutlinedButton(
                                                    onClick = { viewModel.toggleTrackVehicle(stop, eta) },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.GpsFixed,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("追蹤此班", fontSize = 11.sp)
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
    }
}
