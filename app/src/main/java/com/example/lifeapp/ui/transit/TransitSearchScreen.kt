package com.example.lifeapp.ui.transit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifeapp.data.local.entity.TransitBookmarkEntity
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 格式化 ETA 時間顯示：例如 "2 分鐘 (05:02)" 或 "即將到達 (05:00)"
 */
fun formatEtaDisplay(eta: TransitEta): String {
    val mins = eta.minutesLeft

    // 嘗試解析 API 回傳的 ISO 時間戳記，若無法解析則以當前時間 + 剩餘分鐘數計算
    val formattedTime = if (!eta.etaTimestamp.isNullOrEmpty()) {
        try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val date = sdfInput.parse(eta.etaTimestamp)
            if (date != null) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            } else null
        } catch (_: Exception) { null }
    } else null

    val clockString = formattedTime ?: run {
        if (mins != null) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MINUTE, mins)
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)
        } else ""
    }

    val etaText = when {
        mins == null -> eta.remarkZh.ifEmpty { "暫無班次" }
        mins <= 0 -> "即將到達"
        else -> "${mins} 分鐘"
    }

    return if (clockString.isNotEmpty()) {
        "$etaText ($clockString)"
    } else {
        etaText
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitSearchScreen(viewModel: TransitSearchViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.selectedRoute != null) {
                        Text(
                            text = "${uiState.selectedRoute?.routeName} 往 ${uiState.selectedRoute?.destinationZh}",
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(text = "公共交通查詢", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    if (uiState.selectedRoute != null) {
                        IconButton(onClick = { viewModel.clearSelectedRoute() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.selectedRoute == null) {
                TabRow(selectedTabIndex = uiState.currentTab.ordinal) {
                    Tab(
                        selected = uiState.currentTab == TransitTab.SEARCH,
                        onClick = { viewModel.selectTab(TransitTab.SEARCH) },
                        text = { Text("搜尋路線") },
                        icon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    Tab(
                        selected = uiState.currentTab == TransitTab.BOOKMARK,
                        onClick = { viewModel.selectTab(TransitTab.BOOKMARK) },
                        text = { Text("已收藏") },
                        icon = { Icon(Icons.Default.Bookmark, contentDescription = null) }
                    )
                }

                when (uiState.currentTab) {
                    TransitTab.SEARCH -> SearchTabContent(uiState = uiState, viewModel = viewModel)
                    TransitTab.BOOKMARK -> BookmarkTabContent(uiState = uiState, viewModel = viewModel)
                }
            } else {
                RouteDetailContent(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SearchTabContent(
    uiState: TransitUiState,
    viewModel: TransitSearchViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 搜尋欄
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.searchQuery.ifEmpty { "請輸入路線號碼..." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (uiState.searchQuery.isEmpty()) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onBackspaceClicked() }) {
                        Icon(Icons.Default.Backspace, contentDescription = "Clear")
                    }
                }
            }
        }

        // 動態 Chip 鍵盤 (數字列與字母列)
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(uiState.numericChips) { num ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.onChipClicked(num) },
                        label = { Text(num.toString()) }
                    )
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(uiState.letterChips) { letter ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.onChipClicked(letter) },
                        label = { Text(letter.toString()) }
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // 路線列表
        if (uiState.isLoadingRoutes) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.filteredRoutes) { route ->
                    ListItem(
                        headlineContent = { Text(route.routeName, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("${route.originZh} ➔ ${route.destinationZh}") },
                        modifier = Modifier.clickable { viewModel.selectRoute(route) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun RouteDetailContent(
    uiState: TransitUiState,
    viewModel: TransitSearchViewModel
) {
    if (uiState.isLoadingStops) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(uiState.routeStops) { stop ->
                val etaList = uiState.selectedStopEtaMap[stop.stopId] ?: emptyList()
                val route = uiState.selectedRoute
                val bookmarkId = "KMB_${route?.routeName}_${route?.bound}_${route?.serviceType}_${stop.stopId}"
                val isBookmarked = uiState.bookmarkedStopIds.contains(bookmarkId)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // 站序與名稱 (無折疊功能，無重複的方向資訊)
                            Text(
                                text = "${stop.sequence}. ${stop.nameZh}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // 顯示 ETA 分鐘 + 實時點 (HH:mm)
                            if (etaList.isEmpty()) {
                                Text(
                                    text = "載入中或沒有預計班次",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            } else {
                                etaList.take(3).forEach { eta ->
                                    Text(
                                        text = formatEtaDisplay(eta),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
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

@Composable
fun BookmarkTabContent(
    uiState: TransitUiState,
    viewModel: TransitSearchViewModel
) {
    if (uiState.bookmarks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("尚未新增任何收藏車站", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(uiState.bookmarks) { bookmark ->
                val etaList = uiState.selectedStopEtaMap[bookmark.stopId] ?: emptyList()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { viewModel.selectBookmarkRoute(bookmark) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // 路線號碼與目的地
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = bookmark.routeName,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "往 ${bookmark.destinationZh}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // 車站名稱
                            Text(
                                text = bookmark.stopNameZh,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // 顯示與詳細頁面一致的倒數與實時到達時間
                            if (etaList.isEmpty()) {
                                Text(
                                    text = "載入中或沒有預計班次",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            } else {
                                etaList.take(3).forEach { eta ->
                                    Text(
                                        text = formatEtaDisplay(eta),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { viewModel.removeBookmark(bookmark.bookmarkId) }) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Remove Bookmark",
                                tint = Color(0xFFFFC107)
                            )
                        }
                    }
                }
            }
        }
    }
}
