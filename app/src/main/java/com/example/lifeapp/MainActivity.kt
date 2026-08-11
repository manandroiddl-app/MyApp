package com.example.lifeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import com.example.lifeapp.ui.theme.*
import com.example.lifeapp.ui.traffic.TrafficScreen
import com.example.lifeapp.ui.traffic.TrafficViewModel
import com.example.lifeapp.ui.weather.WeatherScreen
import com.example.lifeapp.ui.weather.WeatherViewModel
import dagger.hilt.android.AndroidEntryPoint

enum class Screen(val title: String) {
    HUB("大目錄"),
    WEATHER("香港天氣"),
    TRAFFIC("交通消息")
}

@AndroidEntryPoint
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
fun MainAppLayout(
    weatherViewModel: WeatherViewModel = hiltViewModel(),
    trafficViewModel: TrafficViewModel = hiltViewModel()
) {
    var currentScreen by remember { mutableStateOf(Screen.HUB) }

    Scaffold(
        bottomBar = {
            BottomNavControl(
                currentScreen = currentScreen,
                onBackToHub = { currentScreen = Screen.HUB },
                onRefresh = {
                    when (currentScreen) {
                        Screen.WEATHER -> weatherViewModel.refresh()
                        Screen.TRAFFIC -> trafficViewModel.refresh()
                        Screen.HUB -> { /* 目錄頁無需刷新 */ }
                    }
                }
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
                    Screen.WEATHER -> WeatherScreen(viewModel = weatherViewModel)
                    Screen.TRAFFIC -> TrafficScreen(viewModel = trafficViewModel)
                }
            }
        }
    }
}

@Composable
fun HubScreen(onNavigate: (Screen) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Text("生活大目錄", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
        Text("請選擇你想要查看的即時資訊", fontSize = 14.sp, color = TextGray, modifier = Modifier.padding(bottom = 20.dp))

        MenuCard(title = "香港天氣", subtitle = "警告、分區氣溫、今日及九日預報", iconText = "☀️", onClick = { onNavigate(Screen.WEATHER) })
        Spacer(modifier = Modifier.height(12.dp))
        MenuCard(title = "交通消息", subtitle = "特別交通預告及即時路況", iconText = "🚗", onClick = { onNavigate(Screen.TRAFFIC) })
    }
}

@Composable
fun MenuCard(title: String, subtitle: String, iconText: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).background(PrimaryLightBlue, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = iconText, fontSize = 26.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(subtitle, fontSize = 13.sp, color = TextGray)
            }
        }
    }
}

@Composable
fun BottomNavControl(
    currentScreen: Screen,
    onBackToHub: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onRefresh,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("重新整理", fontSize = 13.sp)
            }

            Text(
                text = "📍 ${currentScreen.title}",
                color = PrimaryDarkBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onBackToHub,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkBlue),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("🏠 返大目錄", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}
