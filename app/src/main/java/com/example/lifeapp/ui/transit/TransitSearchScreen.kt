package com.example.lifeapp.ui.transit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop
import com.example.lifeapp.ui.theme.*

@Composable
fun TransitSearchScreen(
    viewModel: TransitSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundLight,
        bottomBar = {
            // 🎯 底部鍵盤（只限搜尋頁且未選中特定路線時顯示）
            if (uiState.selectedRoute == null && uiState.currentTab == TransitTab.SEARCH) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(vertical = 10.dp, horizontal = 12.dp)
                    ) {
                        // 第一行：數字 Chips (向橫 Scroll)
                        if (uiState.numericChips.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(uiState.numericChips) { char ->
                                    SuggestionChip(
                                        onClick = { viewModel.onChipClicked(char) },
                                        label = { Text(char.toString(), fontWeight = FontWeight.Bold) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = PrimaryLightBlue,
                                            labelColor = PrimaryDarkBlue
                                        ),
                                        border = null
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // 第二行：字母 Chips (向橫 Scroll) + 靠右退格按鈕
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(uiState.letterChips) { char ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { viewModel.onChipClicked(char) },
                                        label = { Text(char.toString(), fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = Color(0xFFE8EAF6),
                                            labelColor = PrimaryDarkBlue
                                        ),
                                        border = null
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // 🎯 退格 Button (靠右邊放置)
                            IconButton(
                                onClick = { viewModel.onBackspaceClicked() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFFFEBEE), RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "退格",
                                    tint = Color.Red,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // 🎯 統一頁面標題（藍色系與字級統一）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.selectedRoute != null) {
                    IconButton(onClick = { viewModel.clearSelectedRoute() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = PrimaryDarkBlue)
                    }
                }
                Text(
                    text = if (uiState.selectedRoute == null) "巴士 / 交通到站" else "路線 ${uiState.selectedRoute?.routeName}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDarkBlue
                )
            }

            Text(
                text = if (uiState.selectedRoute == null) "請輸入路線編號以搜尋即時到站時間" else "${uiState.selectedRoute?.originZh} ➔ ${uiState.selectedRoute?.destinationZh}",
                fontSize = 14.sp,
                color = TextGray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Tab 選擇列 (搜尋 / 書籤)
            if (uiState.selectedRoute == null) {
                TabRow(
                    selectedTabIndex = uiState.currentTab.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = PrimaryDarkBlue,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Tab(
                        selected = uiState.currentTab == TransitTab.SEARCH,
                        onClick = { viewModel.selectTab(TransitTab.SEARCH) },
                        text = { Text("路線搜尋", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = uiState.currentTab == TransitTab.BOOKMARK,
                        onClick = { viewModel.selectTab(TransitTab.BOOKMARK) },
                        text = { Text("我的收藏 (${uiState.bookmarks.size})", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (uiState.selectedRoute == null) {
                if (uiState.currentTab == TransitTab.SEARCH) {
                    // 搜尋輸入框
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        label = { Text("搜尋路線 (例如 1A, 102, 607X)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryDarkBlue,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.isLoadingRoutes) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryDarkBlue)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.filteredRoutes) { route ->
                                RouteCardItem(route = route, onClick = { viewModel.selectRoute(route) })
                            }
                        }
                    }
                } else {
                    // 書籤分頁內容...
                    Text("已收藏的車站列表", fontSize = 14.sp, color = TextGray)
                }
            } else {
                // Level 2 路線詳情 (車站與到站 ETA)
                if (uiState.isLoadingStops) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryDarkBlue)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.routeStops) { stop ->
                            val etas = uiState.selectedStopEtaMap[stop.stopId]
                            val isBookmarked = uiState.bookmarkedStopIds.contains("KMB_${uiState.selectedRoute?.routeName}_${uiState.selectedRoute?.bound}_${uiState.selectedRoute?.serviceType}_${stop.stopId}")

                            StopCardItem(
                                stop = stop,
                                etas = etas,
                                isBookmarked = isBookmarked,
                                onFetchEta = { viewModel.fetchStopEta(stop.stopId) },
                                onToggleBookmark = { viewModel.toggleBookmark(stop) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RouteCardItem(
    route: TransitRoute,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(PrimaryLightBlue, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = route.routeName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = PrimaryDarkBlue
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${route.originZh} ➔ ${route.destinationZh}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(text = "九巴 KMB", fontSize = 12.sp, color = TextGray)
            }
        }
    }
}

@Composable
fun StopCardItem(
    stop: TransitStop,
    etas: List<TransitEta>?,
    isBookmarked: Boolean,
    onFetchEta: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    var expanded by remember { mutableStateOf(etas != null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                expanded = !expanded
                if (expanded && etas == null) {
                    onFetchEta()
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) Color(0xFFE3F2FD) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = PrimaryDarkBlue,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${stop.sequence}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stop.nameZh,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "收藏",
                        tint = if (isBookmarked) Color(0xFFFFB300) else TextGray
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                if (etas == null) {
                    Text("載入 ETA 到站時間中...", fontSize = 13.sp, color = TextGray)
                } else if (etas.isEmpty()) {
                    Text("暫無即時到站班次", fontSize = 13.sp, color = TextGray)
                } else {
                    etas.forEach { eta ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "往 ${eta.destinationZh}",
                                fontSize = 13.sp,
                                color = TextDark
                            )
                            Text(
                                text = when {
                                    eta.minutesLeft == null -> "無即時班次"
                                    eta.minutesLeft <= 0 -> "即將到站"
                                    else -> "${eta.minutesLeft} 分鐘"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if ((eta.minutesLeft ?: 99) <= 3) Color.Red else PrimaryDarkBlue
                            )
                        }
                    }
                }
            }
        }
    }
}
