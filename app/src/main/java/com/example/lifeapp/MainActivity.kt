package com.example.lifeapp

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifeapp.data.local.AppDatabase
import com.example.lifeapp.data.repository.AppConfigRepository
import com.example.lifeapp.ui.theme.*
import com.example.lifeapp.ui.traffic.TrafficScreen
import com.example.lifeapp.ui.traffic.TrafficViewModel
import com.example.lifeapp.ui.transit.TransitSearchScreen
import com.example.lifeapp.ui.weather.WeatherScreen
import com.example.lifeapp.ui.weather.WeatherViewModel
import com.example.lifeapp.util.DbExportHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Screen(val title: String) {
    HUB("大目錄"),
    WEATHER("香港天氣"),
    TRAFFIC("特別交通消息"),
    BUS_SEARCH("巴士/交通到站")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appConfigRepository: AppConfigRepository

    @Inject
    lateinit var appDatabase: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        setContent {
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                }
            }

            LifeAppTheme {
                MainAppLayout(
                    appConfigRepository = appConfigRepository,
                    appDatabase = appDatabase
                )
            }
        }
    }
}

@Composable
fun MainAppLayout(
    appConfigRepository: AppConfigRepository,
    appDatabase: AppDatabase,
    weatherViewModel: WeatherViewModel = hiltViewModel(),
    trafficViewModel: TrafficViewModel = hiltViewModel()
) {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.HUB) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isConfigRefreshing by appConfigRepository.isRefreshing.collectAsState()

    LaunchedEffect(Unit) {
        appConfigRepository.loadRemoteConfig()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavControl(
                currentScreen = currentScreen,
                isRefreshing = isConfigRefreshing,
                onBackToHub = { currentScreen = Screen.HUB },
                onRefresh = {
                    scope.launch {
                        when (currentScreen) {
                            Screen.HUB -> {
                                appConfigRepository.loadRemoteConfig()
                                Toast.makeText(context, "已從 GitHub 重新載入設定", Toast.LENGTH_SHORT).show()
                            }
                            Screen.WEATHER -> {
                                weatherViewModel.refresh()
                                Toast.makeText(context, "已更新天氣數據", Toast.LENGTH_SHORT).show()
                            }
                            Screen.TRAFFIC -> {
                                trafficViewModel.refresh()
                                Toast.makeText(context, "已更新交通消息", Toast.LENGTH_SHORT).show()
                            }
                            Screen.BUS_SEARCH -> {
                                Toast.makeText(context, "可以在搜尋頁輸入路線重新搜尋", Toast.LENGTH_SHORT).show()
                            }
                        }
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
                    Screen.HUB -> HubScreen(
                        onNavigate = { target -> currentScreen = target },
                        appConfigRepository = appConfigRepository,
                        appDatabase = appDatabase
                    )
                    Screen.WEATHER -> WeatherScreen(viewModel = weatherViewModel)
                    Screen.TRAFFIC -> TrafficScreen(viewModel = trafficViewModel)
                    Screen.BUS_SEARCH -> TransitSearchScreen()
                }
            }
        }
    }
}

@Composable
fun HubScreen(
    onNavigate: (Screen) -> Unit,
    appConfigRepository: AppConfigRepository,
    appDatabase: AppDatabase
) {
    val config by appConfigRepository.appConfig.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        config.globalAnnouncement?.let { announcement ->
            if (announcement.enabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (announcement.level == "warning") Color(0xFFFFEBEE) else Color(0xFFE3F2FD)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "📢 ${announcement.title}",
                            fontWeight = FontWeight.Bold,
                            color = if (announcement.level == "warning") Color.Red else PrimaryDarkBlue,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = announcement.message, fontSize = 13.sp, color = Color.DarkGray)
                    }
                }
            }
        }

        Text("生活大目錄", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
        Text("請選擇你想要查看的即時資訊", fontSize = 14.sp, color = TextGray, modifier = Modifier.padding(bottom = 20.dp))

        config.hubScreen.cards.filter { it.enabled }.forEach { card ->
            MenuCard(
                title = card.title,
                subtitle = card.subtitle,
                iconText = card.icon,
                badge = card.badge,
                onClick = {
                    when (card.id.lowercase()) {
                        "weather" -> onNavigate(Screen.WEATHER)
                        "traffic" -> onNavigate(Screen.TRAFFIC)
                        "bus", "bus_search", "transit", "eta" -> onNavigate(Screen.BUS_SEARCH)
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ------------------ 🛠️ 臨時偵錯按鈕 ------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🛠️ 開發者偵錯工具",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFE65100)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        isExporting = true
                        scope.launch {
                            val result = DbExportHelper.exportDatabaseToExternalStorage(context, appDatabase)
                            Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                            isExporting = false
                        }
                    },
                    enabled = !isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isExporting) "匯出中..." else "匯出 Room DB 到 Android/data/", fontSize = 13.sp)
                }
            }
        }
        // --------------------------------------------------------
    }
}

@Composable
fun MenuCard(
    title: String,
    subtitle: String,
    iconText: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    badge?.let { b ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = PrimaryDarkBlue,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = b,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(subtitle, fontSize = 13.sp, color = TextGray)
            }
        }
    }
}

@Composable
fun BottomNavControl(
    currentScreen: Screen,
    isRefreshing: Boolean = false,
    onBackToHub: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onRefresh,
                enabled = !isRefreshing,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isRefreshing) "更新中..." else "重新整理", fontSize = 13.sp)
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
