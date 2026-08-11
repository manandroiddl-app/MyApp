package com.example.lifeapp.ui.bus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 已收藏, 1: 搜尋交通工具

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab, containerColor = Color.White) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("⭐ 已收藏到站時間", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("🔍 搜尋交通工具", fontWeight = FontWeight.Bold) }
            )
        }

        when (selectedTab) {
            0 -> BookmarkTabContent(viewModel)
            1 -> SearchTabContent(viewModel)
        }
    }
}

// === Tab 1: 已收藏頁面 (1分鐘自動更新) ===
@Composable
fun BookmarkTabContent(viewModel: BusViewModel) {
    val state by viewModel.bookmarkUiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("已收藏路線 ETA", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
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
                Text("未有收藏車站。請到「搜尋交通工具」搜尋並加入收藏！", fontSize = 14.sp, color = TextGray)
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
                    Text("🚌 ${bookmark.route}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
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

// === Tab 2: 搜尋頁面 ===
@Composable
fun SearchTabContent(viewModel: BusViewModel) {
    val state by viewModel.searchUiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (state.selectedRoute == null) {
            // 路線搜尋列表
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜尋路線編號 (例: 1A, 290, 960)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                Text("🚌 ${route.route}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                                Text("${route.origTc} ➔ ${route.destTc}", fontSize = 13.sp, color = TextDark)
                            }
                        }
                    }
                }
            }
        } else {
            // 已選擇路線：顯示車站列表
            val route = state.selectedRoute!!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("九巴 ${route.route}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                    Text("往 ${route.destTc}", fontSize = 13.sp, color = TextGray)
                }
                Button(onClick = { viewModel.clearSelectedRoute() }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                    Text("返回搜尋")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.stopList) { (stop, detail) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${stop.seq}. ${detail.nameTc}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                }
                                IconButton(onClick = { viewModel.toggleBookmark(route, detail) }) {
                                    Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmark", tint = PrimaryBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
