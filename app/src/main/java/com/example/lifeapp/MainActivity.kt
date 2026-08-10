package com.example.lifeapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 1. 定義導航頁面列舉
enum class Screen(val title: String) {
    HUB("大目錄"),
    WEATHER("香港天氣"),
    TRAFFIC("交通消息")
}

// 2. 定義藍色系 Color Palette
val PrimaryBlue = Color(0xFF1E88E5)
val PrimaryDarkBlue = Color(0xFF1565C0)
val PrimaryLightBlue = Color(0xFFE3F2FD)
val BackgroundLight = Color(0xFFF4F7FA)
val TextDark = Color(0xFF2C3E50)
val TextGray = Color(0xFF7F8C8D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LifeAppTheme {
                MainAppLayout()
            }
        }
    }
}

@Composable
fun LifeAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PrimaryBlue,
            background = BackgroundLight,
            surface = Color.White
        ),
        content = content
    )
}

@Composable
fun MainAppLayout() {
    var currentScreen by remember { mutableStateOf(Screen.HUB) }

    Scaffold(
        bottomBar = {
            BottomNavControl(
                currentScreen = currentScreen,
                onBackToHub = { currentScreen = Screen.HUB }
            )
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    Screen.HUB -> HubScreen(onNavigate = { target -> currentScreen = target })
                    Screen.WEATHER -> WeatherScreen()
                    Screen.TRAFFIC -> TrafficScreen()
                }
            }
        }
    }
}

// --- 第一層：大目錄頁面 ---
@Composable
fun HubScreen(onNavigate: (Screen) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "生活大目錄",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryDarkBlue
        )
        Text(
            text = "請選擇你想要查看的即時資訊",
            fontSize = 14.sp,
            color = TextGray,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        MenuCard(
            title = "香港天氣",
            subtitle = "即時氣溫、濕度及天氣警告",
            iconText = "☀️",
            onClick = { onNavigate(Screen.WEATHER) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        MenuCard(
            title = "交通消息",
            subtitle = "特別交通預告及即時路況",
            iconText = "🚗",
            onClick = { onNavigate(Screen.TRAFFIC) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 預留未來功能卡片
        MenuCard(
            title = "其他功能",
            subtitle = "後續開發新增項目...",
            iconText = "➕",
            enabled = false,
            onClick = {}
        )
    }
}

@Composable
fun MenuCard(
    title: String,
    subtitle: String,
    iconText: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(PrimaryLightBlue, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = iconText, fontSize = 26.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) TextDark else TextGray
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextGray
                )
            }
        }
    }
}

// --- 第二層：專屬頁面 - 天氣 (無 Top Title Header) ---
@Composable
fun WeatherScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 右上方重新整理按鈕
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = { Toast.makeText(context, "已更新天氣數據", Toast.LENGTH_SHORT).show() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryLightBlue)
            ) {
                Text("🔄 重新整理", color = PrimaryDarkBlue, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 天氣重點資訊卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("天文台即時天氣", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                Text("26°C", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Text("大致天晴 | 濕度 78%", color = Color.White, fontSize = 15.sp)

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text("最高: 29°C", color = Color.White, fontSize = 13.sp)
                    Text("最低: 23°C", color = Color.White, fontSize = 13.sp)
                    Text("紫外線: 中等", color = Color.White, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📌 今日提示：", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("吹和緩偏東風，未來一兩日部分時間有陽光，天氣乾燥。", color = TextDark, fontSize = 13.sp)
            }
        }
    }
}

// --- 第二層：專屬頁面 - 交通消息 (無 Top Title Header) ---
@Composable
fun TrafficScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 右上方重新整理按鈕
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = { Toast.makeText(context, "已更新交通消息", Toast.LENGTH_SHORT).show() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryLightBlue)
            ) {
                Text("🔄 重新整理", color = PrimaryDarkBlue, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TrafficItemCard("10 分鐘前 • 運輸署通知", "🔴 觀塘道工程：往旺角方向近創紀之城第 5 期慢線暫時封閉，交通繁忙。")
        TrafficItemCard("35 分鐘前 • 港鐵消息", "🟢 港鐵服務：全線列車服務目前維持正常班次。")
        TrafficItemCard("1 小時前 • 特別交通", "🟡 屯門公路：往九龍方向近深井車流較多，請駕駛人士小心駕駛。")
    }
}

@Composable
fun TrafficItemCard(timeText: String, contentText: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(timeText, fontSize = 12.sp, color = TextGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(contentText, fontSize = 14.sp, color = TextDark, lineHeight = 20.sp)
        }
    }
}

// --- 底部分頁欄 (Bottom Navigation Bar) ---
@Composable
fun BottomNavControl(
    currentScreen: Screen,
    onBackToHub: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左下角：現正觀看動態標籤
            Box(
                modifier = Modifier
                    .background(PrimaryLightBlue, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "👀 現正觀看：${currentScreen.title}",
                    color = PrimaryDarkBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 右下角：🏠 返大目錄按鈕
            Button(
                onClick = onBackToHub,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🏠 返大目錄", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}
