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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop
import com.example.lifeapp.ui.theme.*

@Composable
fun TransitSearchScreen(
    viewModel: TransitSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResumeRefresh()
            else if (event == Lifecycle.Event.ON_PAUSE) viewModel.onPauseStopRefresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            ) {
                if (uiState.selectedRoute != null) {
                    IconButton(
                        onClick = { viewModel.clearSelectedRoute() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = PrimaryDarkBlue)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (uiState.selectedRoute == null) "巴士 / 交通到站" else "路線 ${uiState.selectedRoute?.routeName}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDarkBlue
                )
            }

            Text(
                text = if (uiState.selectedRoute == null) "請輸入路線編號以搜尋即時到站時間" else "${uiState.selectedRoute?.originZh} ➔ ${uiState.selectedRoute?.destinationZh}",
                fontSize = 12.sp,
                color = TextGray,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            if (uiState.selectedRoute == null) {
                TabRow(
                    selectedTabIndex = uiState.currentTab.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = PrimaryDarkBlue,
                    modifier = Modifier.padding(bottom = 6.dp)
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

                if (uiState.currentTab == TransitTab.SEARCH) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        label = { Text("搜尋路線 (例如 1A, E33, 102)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryDarkBlue,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (uiState.isLoadingRoutes) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryDarkBlue)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                            items(uiState.filteredRoutes) { route ->
                                RouteCardItem(route = route, onClick = { viewModel.selectRoute(route) })
                            }
                        }
                    }
                } else {
                    Text("已收藏的車站列表", fontSize = 14.sp, color = TextGray)
                }
            } else {
                if (uiState.isLoadingStops) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryDarkBlue)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                        items(uiState.routeStops) { stop ->
                            val etas = uiState.selectedStopEtaMap[stop.stopId]
                            val isExpanded = uiState.expandedStopIds.contains(stop.stopId)
                            val isBookmarked = uiState.bookmarkedStopIds.contains("KMB_${uiState.selectedRoute?.routeName}_${uiState.selectedRoute?.bound}_${uiState.selectedRoute?.serviceType}_${stop.stopId}")

                            StopCardItem(
                                stop = stop,
                                etas = etas,
                                isExpanded = isExpanded,
                                isBookmarked = isBookmarked,
                                onToggleExpand = { viewModel.toggleStopExpand(stop.stopId) },
                                onToggleBookmark = { viewModel.toggleBookmark(stop) }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.selectedRoute == null && uiState.currentTab == TransitTab.SEARCH) {
            Surface(
                color = Color.White,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp, start = 12.dp, end = 12.dp)
                ) {
                    if (uiState.numericChips.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            items(uiState.numericChips) { char ->
                                SuggestionChip(
                                    onClick = { viewModel.onChipClicked(char) },
                                    label = { Text(char.toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = PrimaryLightBlue,
                                        labelColor = PrimaryDarkBlue
                                    ),
                                    border = null
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            items(uiState.letterChips) { char ->
                                FilterChip(
                                    selected = false,
                                    onClick = { viewModel.onChipClicked(char) },
                                    label = { Text(char.toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0xFFE8EAF6),
                                        labelColor = PrimaryDarkBlue
                                    ),
                                    border = null
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.onBackspaceClicked() },
                            modifier = Modifier.size(36.dp).background(Color(0xFFFFEBEE), RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Default.Backspace, contentDescription = "退格", tint = Color.Red, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RouteCardItem(route: TransitRoute, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(PrimaryLightBlue, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(route.routeName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryDarkBlue)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${route.originZh} ➔ ${route.destinationZh}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text("九巴 KMB ${if (route.bound == "O") "(去程)" else if (route.bound == "I") "(回程)" else ""}", fontSize = 12.sp, color = TextGray)
            }
        }
    }
}

@Composable
fun StopCardItem(
    stop: TransitStop,
    etas: List<TransitEta>?,
    isExpanded: Boolean,
    isBookmarked: Boolean,
    onToggleExpand: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onToggleExpand() },
        colors = CardDefaults.cardColors(containerColor = if (isExpanded) Color(0xFFE3F2FD) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(color = PrimaryDarkBlue, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = "${stop.sequence}", color = Color.White, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                
                Text(
                    text = stop.nameZh.ifEmpty { "車站 ${stop.sequence}" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleBookmark, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "收藏",
                        tint = if (isBookmarked) Color(0xFFFFB300) else TextGray
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(6.dp))

                if (etas == null) {
                    Text("載入到站時間中...", fontSize = 12.sp, color = TextGray)
                } else if (etas.isEmpty()) {
                    Text("暫時無班次資料", fontSize = 13.sp, color = TextGray, fontWeight = FontWeight.Medium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        etas.take(3).forEachIndexed { index, eta ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = Color(0xFFE0E0E0), shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = "班次 ${index + 1}", fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold, color = Color.DarkGray,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    
                                    val destName = eta.destinationZh?.takeIf { it.isNotEmpty() } ?: "終點站"
                                    Text(text = "往 $destName", fontSize = 13.sp, color = TextDark)
                                }
                                
                                val mins = eta.minutesLeft
                                val displayText: String
                                val displayColor: Color

                                if (eta.etaTimestamp.isEmpty() || mins == null) {
                                    displayText = "暫時無班次資料"
                                    displayColor = TextGray
                                } else if (mins <= 1) {
                                    displayText = "即將到站"
                                    displayColor = Color.Red
                                } else {
                                    displayText = "$mins 分鐘"
                                    displayColor = PrimaryDarkBlue
                                }

                                Text(
                                    text = displayText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = displayColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
