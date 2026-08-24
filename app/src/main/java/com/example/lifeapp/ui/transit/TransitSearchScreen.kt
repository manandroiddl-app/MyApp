package com.example.lifeapp.ui.transit

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.ui.theme.PrimaryDarkBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val BluePrimary = Color(0xFF1976D2)
private val BlueOnPrimary = Color(0xFFFFFFFF)
private val BlueContainer = Color(0xFFE3F2FD)
private val BlueOnContainer = Color(0xFF0D47A1)
private val HighlightYellow = Color(0xFFFFF59D)

private fun parseEtaMillis(etaTimestamp: String?): Long? {
    if (etaTimestamp.isNullOrEmpty()) return null
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        sdf.parse(etaTimestamp)?.time
    } catch (_: Exception) { null }
}

fun formatEtaDisplay(eta: TransitEta): String {
    val mins = eta.minutesLeft

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
        mins == null -> eta.remarkZh.orEmpty().ifEmpty { "暫無班次" }
        mins <= 0 -> "即將到達"
        else -> "${mins} 分鐘"
    }

    return if (clockString.isNotEmpty()) {
        "$etaText ($clockString)"
    } else {
        etaText
    }
}

fun formatCompanyDisplayName(company: Any?): String {
    val companyStr = when (company) {
        is Enum<*> -> company.name
        else -> company?.toString()
    }

    return when (companyStr?.uppercase()) {
        "KMB" -> "九巴"
        "CTB" -> "城巴"
        "NWFB" -> "新巴"
        "GMB" -> "綠色小巴"
        "NLB" -> "嶼巴"
        "MTR" -> "港鐵巴士"
        else -> companyStr.orEmpty().ifEmpty { "巴士" }
    }
}

@Composable
fun TransitSearchScreen(
    viewModel: TransitSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val customColorScheme = lightColorScheme(
        primary = BluePrimary,
        onPrimary = BlueOnPrimary,
        primaryContainer = BlueContainer,
        onPrimaryContainer = BlueOnContainer
    )

    MaterialTheme(colorScheme = customColorScheme) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val titleText = if (uiState.selectedRoute != null) {
                        "${uiState.selectedRoute?.routeName} 往 ${uiState.selectedRoute?.destinationZh}"
                    } else {
                        "公共交通查詢"
                    }
                    Text(
                        text = titleText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = PrimaryDarkBlue
                    )
                }

                if (uiState.selectedRoute == null) {
                    TabRow(
                        selectedTabIndex = uiState.currentTab.ordinal,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = uiState.currentTab == TransitTab.SEARCH,
                            onClick = { viewModel.selectTab(TransitTab.SEARCH) },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("搜尋路線", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                        Tab(
                            selected = uiState.currentTab == TransitTab.BOOKMARK,
                            onClick = { viewModel.selectTab(TransitTab.BOOKMARK) },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("已收藏", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (uiState.currentTab) {
                            TransitTab.SEARCH -> SearchTabContent(uiState = uiState, viewModel = viewModel)
                            TransitTab.BOOKMARK -> BookmarkTabContent(uiState = uiState, viewModel = viewModel)
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f)) {
                        RouteDetailContent(uiState = uiState, viewModel = viewModel)
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = { viewModel.clearSelectedRoute() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("返回搜尋結果", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
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
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.isLoadingRoutes) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.filteredRoutes) { route ->
                        val serviceTypeInt = route.serviceType?.toIntOrNull() ?: 1
                        val isSpecialService = serviceTypeInt > 1

                        ListItem(
                            headlineContent = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = BlueContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = formatCompanyDisplayName(route.company),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BlueOnContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = route.routeName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = PrimaryDarkBlue
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = "${route.originZh} ➔ ${route.destinationZh}",
                                            fontSize = 14.sp,
                                            color = Color.DarkGray,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (isSpecialService) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            color = Color(0xFFFFE0B2),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "特別班次",
                                                color = Color(0xFFE65100),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.clickable { viewModel.selectRoute(route) }
                        )
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            color = BlueContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 0.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.searchQuery.ifEmpty { "請點擊下方按鈕輸入路線..." },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (uiState.searchQuery.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                            color = if (uiState.searchQuery.isEmpty()) Color.Gray else PrimaryDarkBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 0.dp)
                ) {
                    items(uiState.numericChips) { num ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.onChipClicked(num) },
                            label = {
                                Text(
                                    text = num.toString(),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            },
                            modifier = Modifier.height(34.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = PrimaryDarkBlue
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = BluePrimary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(vertical = 0.dp)
                    ) {
                        items(uiState.letterChips) { letter ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.onChipClicked(letter) },
                                label = {
                                    Text(
                                        text = letter.toString(),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                },
                                modifier = Modifier.height(34.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White,
                                    labelColor = PrimaryDarkBlue
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = false,
                                    borderColor = BluePrimary.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { viewModel.onBackspaceClicked() },
                        enabled = uiState.searchQuery.isNotEmpty(),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Backspace",
                            tint = if (uiState.searchQuery.isNotEmpty()) PrimaryDarkBlue else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        val tracked = uiState.trackedVehicle
        val trackedMillis = parseEtaMillis(tracked?.etaTimestamp)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(uiState.routeStops) { stop ->
                val etaList = uiState.selectedStopEtaMap[stop.stopId] ?: emptyList()
                val route = uiState.selectedRoute
                val bookmarkId = "KMB_${route?.routeName}_${route?.bound}_${route?.serviceType}_${stop.stopId}"
                val isBookmarked = uiState.bookmarkedStopIds.contains(bookmarkId)

                val isTargetStop = tracked != null && tracked.stopId == stop.stopId
                val isDownstream = tracked != null && stop.sequence > tracked.stopSequence

                // 純時序推移演算法：下游抵達時間必須大於或等於上游時間 (diff >= 0)
                val matchedDownstreamEta = if (isDownstream && trackedMillis != null) {
                    etaList
                        .mapNotNull { eta ->
                            val etaTime = parseEtaMillis(eta.etaTimestamp) ?: return@mapNotNull null
                            val diff = etaTime - trackedMillis
                            if (diff >= 0) Pair(eta, diff) else null
                        }
                        .minByOrNull { it.second }
                        ?.first
                } else null

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
                                    val isHighlight = when {
                                        isTargetStop -> eta.etaTimestamp == tracked?.etaTimestamp
                                        isDownstream -> eta == matchedDownstreamEta
                                        else -> false
                                    }

                                    val showTrackButton = when {
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
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = BlueContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${formatCompanyDisplayName(bookmark.company)} ${bookmark.routeName}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = BlueOnContainer
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

                            Text(
                                text = "車站：${bookmark.stopNameZh}",
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
                                    Text(
                                        text = formatEtaDisplay(eta),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BluePrimary,
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
