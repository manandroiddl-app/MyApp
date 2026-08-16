package com.example.lifeapp.ui.traffic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.lifeapp.ui.theme.*

@Composable
fun TrafficScreen(viewModel: TrafficViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // 🛡️ 核心修復：ON_RESUME 生命週期監聽與 1 分鐘輪詢 Timer 綁定
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // 切回前景或解鎖時：背景靜默更新並啟動每分鐘 Timer
                    viewModel.loadTrafficData(isSilent = true)
                    viewModel.startAutoRefresh()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // 切到背景或離頁時：自動關閉 Timer 省電
                    viewModel.stopAutoRefresh()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopAutoRefresh()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = uiState.errorMessage ?: "", color = WarningRed, fontSize = 14.sp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // 1. 頁面頂部標題與更新時間 (格式: yyyyMMdd HH:mm:ss)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚗 即時特別交通消息",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDarkBlue
                    )
                    if (uiState.updateTime.isNotEmpty()) {
                        Text(
                            text = "更新時間: ${uiState.updateTime}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                // 2. 特別交通消息列表渲染
                if (uiState.trafficNews.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "目前全港交通大致暢順，沒有特別交通消息",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uiState.trafficNews) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    item.referenceDate?.let { date ->
                                        if (date.isNotBlank()) {
                                            Text(
                                                text = "⏱️ $date",
                                                fontSize = 12.sp,
                                                color = PrimaryDarkBlue,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                    }
                                    Text(
                                        text = item.chinText ?: "",
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = TextDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
