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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifeapp.ui.common.FullPageLoading
import com.example.lifeapp.ui.common.OnLifecycleResume
import com.example.lifeapp.ui.theme.*

@Composable
fun TrafficScreen(viewModel: TrafficViewModel = hiltViewModel()) {
    val uiState by viewModel.enhancedUiState.collectAsState()

    OnLifecycleResume {
        viewModel.loadTrafficData(isSilent = true)
        viewModel.startAutoRefresh()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAutoRefresh()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        if (uiState.isLoading) {
            FullPageLoading(color = PrimaryBlue)
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

                if (uiState.taggedTrafficNews.isEmpty()) {
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
                        items(uiState.taggedTrafficNews) { taggedItem ->
                            val item = taggedItem.rawItem
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {

                                    // 🏷️ 18 區 Tag 晶片標籤 (新增)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        taggedItem.districtTags.forEach { tag ->
                                            Surface(
                                                color = if (tag == "全港") Color(0xFFECEFF1) else PrimaryLightBlue,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "📍 $tag",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (tag == "全港") Color.DarkGray else PrimaryDarkBlue,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

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
