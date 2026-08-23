app/src/main/java/com/example/lifeapp/ui/transit/TransitSearchScreen.kt
package com.example.lifeapp.ui.transit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifeapp.data.local.entity.TransitBookmarkEntity
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop
import com.example.lifeapp.ui.common.OnLifecycleResume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitSearchScreen(
    viewModel: TransitSearchViewModel = hiltViewModel(),
    onBackClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    // 監聽 App 解鎖/切回前景，自動背景靜默刷新 ETA
    OnLifecycleResume {
        viewModel.onResumeRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.selectedRoute != null) "路線 ${state.selectedRoute?.routeName}" else "交通到站資訊",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (state.selectedRoute != null) {
                        IconButton(onClick = { viewModel.clearSelectedRoute() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回搜尋")
                        }
                    } else if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.selectedRoute != null) {
                // Level 2: 車站與即時 ETA 詳情子頁面
                RouteDetailSubScreen(
                    route = state.selectedRoute!!,
                    stops = state.routeStops,
                    isLoading = state.isLoadingStops,
                    etaMap = state.selectedStopEtaMap,
                    bookmarkedIds = state.bookmarkedStopIds,
                    onStopClick = { viewModel.fetchStopEta(it.stopId) },
                    onBookmarkToggle = { viewModel.toggleBookmark(it) }
                )
            } else {
                // Level 1: 路線搜尋與 Bookmark 分頁
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(selectedTabIndex = state.currentTab.ordinal) {
                        Tab(
                            selected = state.currentTab == TransitTab.SEARCH,
                            onClick = { viewModel.selectTab(TransitTab.SEARCH) },
                            text = { Text("路線搜尋") },
                            icon = { Icon(Icons.Filled.Search, contentDescription = null) }
                        )
                        Tab(
                            selected = state.currentTab == TransitTab.BOOKMARK,
                            onClick = { viewModel.selectTab(TransitTab.BOOKMARK) },
                            text = { Text("已收藏 (${state.bookmarks.size})") },
                            icon = { Icon(Icons.Filled.Bookmark, contentDescription = null) }
                        )
                    }

                    when (state.currentTab) {
                        TransitTab.SEARCH -> SearchTabContent(
                            state = state,
                            onQueryChange = viewModel::onSearchQueryChanged,
                            onClear = viewModel::onClearSearch,
                            onChipClick = viewModel::onChipClicked,
                            onBackspace = viewModel::onBackspaceClicked,
                            onRouteSelect = viewModel::selectRoute
                        )
                        TransitTab.BOOKMARK -> BookmarkTabContent(
                            bookmarks = state.bookmarks,
                            onSelectBookmark = { bookmark ->
                                val route = state.allRoutes.find { 
                                    it.routeName == bookmark.routeName && it.bound == bookmark.bound 
                                } ?: TransitRoute(
                                    routeId = "${bookmark.routeName}-${bookmark.bound}-${bookmark.serviceType}",
                                    routeName = bookmark.routeName,
                                    transitType = com.example.lifeapp.data.model.TransitType.BUS,
                                    company = com.example.lifeapp.data.model.OperatorCompany.KMB,
                                    originZh = bookmark.originZh,
                                    originEn = null,
                                    destinationZh = bookmark.destinationZh,
                                    destinationEn = null,
                                    bound = bookmark.bound,
                                    serviceType = bookmark.serviceType
                                )
                                viewModel.selectRoute(route)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchTabContent(
    state: TransitUiState,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onChipClick: (Char) -> Unit,
    onBackspace: () -> Unit,
    onRouteSelect: (TransitRoute) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 搜尋文字輸入框
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("輸入或點擊 Chip 搜尋路線 (例: 1A, 271)") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Clear, contentDescription = "清除")
                    }
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 動態 Chip 鍵盤區
        Text("快速搜尋鍵盤", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(6.dp))
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            state.availableChips.forEach { char ->
                AssistChip(
                    onClick = { onChipClick(char) },
                    label = { Text(char.toString(), fontWeight = FontWeight.Bold) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
            if (state.searchQuery.isNotEmpty()) {
                AssistChip(
                    onClick = onBackspace,
                    label = { Text("⌫ 退格", color = Color.Red, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 路線列表結果
        if (state.isLoadingRoutes) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.filteredRoutes) { route ->
                    RouteItemCard(route = route, onClick = { onRouteSelect(route) })
                }
            }
        }
    }
}

@Composable
private fun RouteItemCard(route: TransitRoute, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = route.routeName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "${route.originZh} ➔ ${route.destinationZh}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${route.company.name} 九巴",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun RouteDetailSubScreen(
    route: TransitRoute,
    stops: List<TransitStop>,
    isLoading: Boolean,
    etaMap: Map<String, List<TransitEta>>,
    bookmarkedIds: Set<String>,
    onStopClick: (TransitStop) -> Unit,
    onBookmarkToggle: (TransitStop) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 路線標頭資訊卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "路線 ${route.routeName}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "往 ${route.destinationZh}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stops) { stop ->
                    val bookmarkId = "KMB_${route.routeName}_${route.bound}_${route.serviceType}_${stop.stopId}"
                    val isBookmarked = bookmarkedIds.contains(bookmarkId)
                    val etas = etaMap[stop.stopId]

                    StopItemCard(
                        stop = stop,
                        etas = etas,
                        isBookmarked = isBookmarked,
                        onClick = { onStopClick(stop) },
                        onBookmarkClick = { onBookmarkToggle(stop) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StopItemCard(
    stop: TransitStop,
    etas: List<TransitEta>?,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${stop.sequence}.",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(28.dp)
                    )
                    Text(
                        text = stop.nameZh,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "收藏車站",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }

            // ETA 預計到站時間列表
            if (etas != null) {
                Spacer(modifier = Modifier.height(8.dp))
                if (etas.isEmpty()) {
                    Text("暫無即時班次資料", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        etas.take(3).forEach { eta ->
                            val minutesText = when (val mins = eta.minutesLeft) {
                                null -> "--"
                                0 -> "即將到達"
                                else -> "${mins} 分鐘"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = minutesText,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (eta.minutesLeft == 0) Color.Red else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkTabContent(
    bookmarks: List<TransitBookmarkEntity>,
    onSelectBookmark: (TransitBookmarkEntity) -> Unit
) {
    if (bookmarks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("尚未收藏任何車站，請在路線詳情頁點擊 🔖 收藏", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(bookmarks) { bookmark ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectBookmark(bookmark) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = bookmark.routeName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "往 ${bookmark.destinationZh}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "車站：${bookmark.stopNameZh}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(Icons.Filled.DirectionsBus, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
