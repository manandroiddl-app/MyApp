package com.example.lifeapp.ui.weather

import androidx.compose.foundation.background
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
import com.example.lifeapp.data.model.DistrictTemperature
import com.example.lifeapp.data.model.NineDayForecast
import com.example.lifeapp.ui.theme.*

@Composable
fun WeatherScreen(viewModel: WeatherViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.errorMessage!!, color = WarningRed, fontSize = 14.sp)
            }
        } else {
            // 🌟 加上 statusBarsPadding，讓天氣卡片向上滾動時能透入 Status Bar 後方，同時保護標題不被卡到
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // 天氣警告標籤
                if (uiState.warningSummaries.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("⚠️ 現正生效警告：", fontWeight = FontWeight.Bold, color = Color(0xFFE65100), fontSize = 14.sp)
                            uiState.warningSummaries.forEach { warning ->
                                Text("• $warning", color = Color(0xFFE65100), fontSize = 13.sp)
                            }
                        }
                    }
                }

                // 本港地區天氣概要
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("☀️ 本港地區天氣概要", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.generalSituation.ifBlank { "暫無概要資料" }, fontSize = 14.sp, color = TextDark, lineHeight = 20.sp)
                        if (uiState.updateTime.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("更新時間：${uiState.updateTime}", fontSize = 11.sp, color = TextGray)
                        }
                    }
                }

                // 分區氣溫
                Text("🌡️ 各區即時氣溫", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue, modifier = Modifier.padding(bottom = 8.dp))
                DistrictTempSection(uiState.districtTemperatures)

                Spacer(modifier = Modifier.height(16.dp))

                // 九天天氣預報
                Text("📅 九天天氣預報", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue, modifier = Modifier.padding(bottom = 8.dp))
                NineDayForecastSection(uiState.nineDayForecasts)

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DistrictTempSection(list: List<DistrictTemperature>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val chunked = list.chunked(2)
            chunked.forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    rowItems.forEach { item ->
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.place, fontSize = 13.sp, color = TextDark)
                            Text("${item.value}°C", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun NineDayForecastSection(list: List<NineDayForecast>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        list.forEach { forecast ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${forecast.forecastDate} (${forecast.week})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                        Text("${forecast.forecastWeather} | 濕度 ${forecast.forecastRH.min}-${forecast.forecastRH.max}%", fontSize = 12.sp, color = TextGray)
                    }
                    Text("${forecast.forecastMaxtemp.value}° / ${forecast.forecastMintemp.value}°C", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
            }
        }
    }
}
