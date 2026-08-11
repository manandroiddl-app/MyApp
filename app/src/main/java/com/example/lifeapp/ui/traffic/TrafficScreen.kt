package com.example.lifeapp.ui.traffic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifeapp.ui.theme.*

@Composable
fun TrafficScreen(viewModel: TrafficViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = PrimaryBlue)
                Spacer(modifier = Modifier.height(12.dp))
                Text("正在獲取最新交通消息...", color = TextGray, fontSize = 14.sp)
            }
        }
    } else if (uiState.errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⚠️ 載入失敗", fontWeight = FontWeight.Bold, color = WarningRed)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(uiState.errorMessage!!, color = TextDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.refresh() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("重試")
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 頂部顯示最後更新時間 (年月日時分秒)
            if (uiState.updateTimeText.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(text = "🕒 ${uiState.updateTimeText}", fontSize = 12.sp, color = TextGray)
                }
            }

            Text("即時特別交通通告", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.items.isNotEmpty()) {
                for (item in uiState.items) {
                    TrafficItemCard(timeText = item.timeText, contentText = item.title)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("✅ 現時無特別交通通告，道路交通大致正常。", fontSize = 14.sp, color = TextDark)
                    }
                }
            }
        }
    }
}

@Composable
fun TrafficItemCard(timeText: String, contentText: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔴 ", fontSize = 14.sp)
                Text(timeText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGray)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(contentText, fontSize = 14.sp, color = TextDark, lineHeight = 22.sp)
        }
    }
}
