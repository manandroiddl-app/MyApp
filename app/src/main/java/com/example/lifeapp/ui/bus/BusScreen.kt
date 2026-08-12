package com.example.lifeapp.ui.bus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifeapp.data.model.BusBookmarkEntity
import com.example.lifeapp.data.model.KmbRoute
import com.example.lifeapp.data.model.KmbStopDetail
import com.example.lifeapp.ui.theme.*

@Composable
fun BusScreen(viewModel: BusViewModel = hiltViewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = "Bookmarks") },
                    label = { Text("⭐ 已收藏到站時間", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("🔍 搜尋交通工具", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> BookmarkTabContent(viewModel)
                1 -> SearchTabContent(viewModel)
            }
        }
    }
}

// === Sub-Tab 1: 已收藏頁面 ===
@Composable
fun BookmarkTabContent(viewModel: BusViewModel) {
    val state by viewModel.bookmarkUiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("已收藏路線 ETA", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
            if (state.lastUpdatedText.isNotEmpty()) {
                Text(state.lastUpdatedText, fontSize = 11.sp, color = TextGray)
            }
        }

        if (state.isLoading && state.bookmarks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (state.bookmarks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("未有收藏車站。請切換至底部「搜尋交通工具」加入收藏！", fontSize = 14.sp, color = TextGray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.bookmarks, key = { it.id }) { bookmark ->
                    val etas = state.etaMap[bookmark.id] ?: emptyList()
                    BookmarkEtaCard(bookmark = bookmark, etas = etas, onDelete = {
                        viewModel.toggleBookmark(
                            KmbRoute(bookmark.route, bookmark.bound, bookmark.serviceType, "", bookmark.destTc),
                            KmbStopDetail(bookmark.stopId, bookmark.stopNameTc, null, null)
                        )
                    })
                }
            }
        }
    }
}

@Composable
fun BookmarkEtaCard(bookmark: BusBookmarkEntity, etas: List<com.example.lifeapp.data.model.BusEtaUiItem>, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = PrimaryLightBlue,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("九巴", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(bookmark.route, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("往 ${bookmark.destTc}", fontSize = 14.sp, color = TextDark)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Unbookmark", tint = WarningRed)
                }
            }

            Text("📍 車站：${bookmark.stopNameTc}", fontSize = 13.sp, color = TextGray)
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.Black.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(10.dp))

            if (etas.isEmpty()) {
                Text("載入中 / 暫無班次...", fontSize = 13.sp, color = TextGray)
            } else {
                for (eta in etas.take(3)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• ${eta.remark.ifBlank { "班次正常" }}", fontSize = 13.sp, color = TextDark)
                        Text(eta.etaText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    }
                }
            }
        }
    }
}

// === Sub-Tab 2: 搜尋頁面 ===
@Composable
fun SearchTabContent(viewModel: BusViewModel) {
    val state by viewModel.searchUiState.collectAsState()
    val nextChars by viewModel.nextAvailableChars.collectAsState()
    val routeStopsEtaMap by viewModel.routeStopsEtaMap.collectAsState()
    var searchModeTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (state.selectedRoute == null) {
            // 🌟 4a & 4b) 上方專注顯示搜尋結果路線列表，輸入欄與控制項移至下方
            Column(modifier = Modifier.weight(1f)) {
                if (state.isLoading && state.routeList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                } else if (searchModeTab == 0) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                        items(state.filteredRoutes) { route ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.selectRoute(route) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = PrimaryLightBlue,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "九巴",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryBlue
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(route.route, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                                    }
                                    Text("${route.origTc} ➔ ${route.destTc}", fontSize = 13.sp, color = TextDark)
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📍 地點搜尋功能", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("此功能正在開發中，敬請期待！", fontSize = 13.sp, color = TextGray)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 🌟 4b) 移至下方的：輸入搜尋欄
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("輸入路線 (例如: 1A, 290, 960)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // 🌟 4b) 移至下方的：動態字元 Chip 按鈕 (在搜尋 Tag 上方)
            if (nextChars.isNotEmpty() && searchModeTab == 0) {
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(nextChars) { charStr ->
                        SuggestionChip(
                            onClick = { viewModel.appendSearchChar(charStr) },
                            label = { Text(charStr, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = PrimaryLightBlue,
                                labelColor = PrimaryBlue
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = PrimaryBlue.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 🌟 4a) 移至最下方的：路線搜尋 Tag 與 地點搜尋 Tag
            TabRow(selectedTabIndex = searchModeTab, containerColor = Color.Transparent) {
                Tab(
                    selected = searchModeTab == 0,
                    onClick = { searchModeTab = 0 },
                    text = { Text("路線搜尋", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = searchModeTab == 1,
                    onClick = { searchModeTab = 1 },
                    text = { Text("地點搜尋 (待開發)", fontWeight = FontWeight.Normal, color = TextGray) }
                )
            }
        } else {
            // 已選擇路線：詳細車站列表 (包含 2: 車費標籤 & 3: 1分鐘自動刷新)
            val route = state.selectedRoute!!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = PrimaryLightBlue,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("九巴", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(route.route, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                    }
                    Text("${route.origTc} ➔ ${route.destTc}", fontSize = 13.sp, color = TextGray)
                }
                Button(
                    onClick = { viewModel.clearSelectedRoute() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("返回搜尋")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("正在獲取路線車站資料...", fontSize = 13.sp, color = TextGray)
                    }
                }
            } else if (state.stopList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("未能取得該路線的車站清單，請重試。", fontSize = 14.sp, color = TextGray)
                }
            } else {
                val bookmarks by viewModel.bookmarkUiState.collectAsState()
                val bookmarkedIds = remember(bookmarks) { bookmarks.bookmarks.map { it.id }.toSet() }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.stopList) { (stop, detail) ->
                        val bookmarkId = "${route.route}_${detail.stopId}_${route.bound}"
                        val isBookmarked = bookmarkedIds.contains(bookmarkId)
                        val stopEtas = routeStopsEtaMap[detail.stopId] ?: emptyList()

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Text("${stop.seq}. ${detail.nameTc}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                        
                                        // 🌟 2) 車站車費標籤 (Fare Badge)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = Color(0xFFE8F5E9),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "💰 車費資訊",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }

                                    IconButton(onClick = { viewModel.toggleBookmark(route, detail) }) {
                                        Icon(
                                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Bookmark",
                                            tint = if (isBookmarked) WarningRed else PrimaryBlue
                                        )
                                    }
                                }

                                // 🌟 3) 車站即時到站時間 (1分鐘自動刷新中)
                                if (stopEtas.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("⏱️ 到站時間：", fontSize = 12.sp, color = TextGray)
                                        stopEtas.forEach { etaText ->
                                            Surface(
                                                color = PrimaryLightBlue.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = etaText,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryBlue
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text("⏱️ 正在獲取到站時間...", fontSize = 12.sp, color = TextGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
