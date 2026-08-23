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
import androidx.compose.material.icons.filled.DirectionsBus
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
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val routeStops by viewModel.routeStops.collectAsState()
    val selectedStop by viewModel.selectedStop.collectAsState()
    val stopEtas by viewModel.stopEtas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val letters = listOf("A", "B", "C", "E", "K", "M", "N", "P", "R", "S", "T", "X")

    Scaffold(
        containerColor = BackgroundLight,
        bottomBar = {
            // 🎯 底部數字 / 英文 Quick Chip 鍵盤
            if (selectedRoute == null) {
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
                        // 第一行：數字 Chips (橫向滾動)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(digits) { digit ->
                                SuggestionChip(
                                    onClick = { viewModel.onQueryChange(searchQuery + digit) },
                                    label = { Text(digit, fontWeight = FontWeight.Bold) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = PrimaryLightBlue,
                                        labelColor = PrimaryDarkBlue
                                    ),
                                    border = null
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 第二行：字母 Chips (橫向滾動) + 靠右退格鍵
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(letters) { letter ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { viewModel.onQueryChange(searchQuery + letter) },
                                        label = { Text(letter, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = Color(0xFFE8EAF6),
                                            labelColor = PrimaryDarkBlue
                                        ),
                                        border = null
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // 退格 Button (靠右)
                            IconButton(
                                onClick = {
                                    if (searchQuery.isNotEmpty()) {
                                        viewModel.onQueryChange(searchQuery.dropLast(1))
                                    }
                                },
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
            // 🎯 統一頁面標題樣式
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (selectedRoute != null) {
                    IconButton(onClick = { viewModel.clearSelection() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = PrimaryDarkBlue)
                    }
                }
                Text(
                    text = if (selectedRoute == null) "巴士 / 交通到站" else "路線 ${selectedRoute?.routeName}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDarkBlue
                )
            }

            Text(
                text = if (selectedRoute == null) "請輸入路線編號以搜尋即時到站時間" else "${selectedRoute?.originZh} ➔ ${selectedRoute?.destinationZh}",
                fontSize = 14.sp,
                color = TextGray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (selectedRoute == null) {
                // 搜尋框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onQueryChange(it) },
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

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryDarkBlue)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(searchResults) { route ->
                            RouteCardItem(route = route, onClick = { viewModel.selectRoute(route) })
                        }
                    }
                }
            } else {
                // 路線詳情：車站與 ETA 列表
                if (isLoading && routeStops.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryDarkBlue)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(routeStops) { stop ->
                            StopCardItem(
                                stop = stop,
                                isSelected = selectedStop?.stopId == stop.stopId,
                                etas = if (selectedStop?.stopId == stop.stopId) stopEtas else emptyList(),
                                onClick = { viewModel.selectStop(stop) }
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
    isSelected: Boolean,
    etas: List<TransitEta>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    color = TextDark
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                if (etas.isEmpty()) {
                    Text("載入 ETA 到站時間中...", fontSize = 13.sp, color = TextGray)
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
