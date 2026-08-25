package com.example.lifeapp.ui.transit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifeapp.ui.common.FullPageLoading
import com.example.lifeapp.ui.theme.PrimaryDarkBlue

private val BluePrimary = Color(0xFF1976D2)
private val HighlightYellow = Color(0xFFFFF59D)

@Composable
fun RouteDetailContent(
    uiState: TransitUiState,
    viewModel: TransitSearchViewModel
) {
    if (uiState.isLoadingStops) {
        FullPageLoading()
    } else {
        val tracked = uiState.trackedVehicle

        val highlightMap = remember(uiState.routeStops, uiState.selectedStopEtaMap, tracked) {
            calculateTrackedChain(
                routeStops = uiState.routeStops,
                stopEtaMap = uiState.selectedStopEtaMap,
                tracked = tracked
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(uiState.routeStops) { stop ->
                val etaList = uiState.selectedStopEtaMap[stop.stopId] ?: emptyList()
                val route = uiState.selectedRoute
                val bookmarkId = "KMB_${route?.routeName}_${route?.bound}_${route?.serviceType}_${stop.stopId}"
                val isBookmarked = uiState.bookmarkedStopIds.contains(bookmarkId)

                val chainMatchedEta = highlightMap[stop.stopId]
                val isTargetStop = tracked != null && tracked.stopId == stop.stopId

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${stop.sequence}. ${stop.nameZh}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDarkBlue
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            if (etaList.isEmpty()) {
                                Text(
                                    text = "載入中或沒有預計班次",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            } else {
                                etaList.take(3).forEach { eta ->
                                    val isHighlight = (eta == chainMatchedEta)

                                    val isTrackable = isValidTrackableEta(eta)
                                    val showTrackButton = isTrackable && when {
                                        tracked == null -> true
                                        isTargetStop && isHighlight -> true
                                        else -> false
                                    }

                                    Surface(
                                        color = if (isHighlight) HighlightYellow else Color.Transparent,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = formatEtaDisplay(eta),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isHighlight) Color.Black else BluePrimary,
                                                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold
                                            )

                                            if (showTrackButton) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.toggleTrackVehicle(
                                                            stopId = stop.stopId,
                                                            stopSequence = stop.sequence,
                                                            etaTimestamp = eta.etaTimestamp
                                                        )
                                                    },
                                                    modifier = Modifier.height(26.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = if (isHighlight) Color.White else Color.Transparent
                                                    )
                                                ) {
                                                    Text(
                                                        text = if (isHighlight) "取消追蹤" else "追蹤",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isHighlight) Color.Red else BluePrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        IconButton(onClick = { viewModel.toggleBookmark(stop) }) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) Color(0xFFFFC107) else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
