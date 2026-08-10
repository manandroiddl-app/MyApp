package com.example.lifeapp.ui.weather

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifeapp.ui.theme.*

@Composable
fun WeatherScreen(viewModel: WeatherViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = PrimaryBlue)
                Spacer(modifier = Modifier.height(12.dp))
                Text("正在獲取最新天氣資訊...", color = TextGray, fontSize = 14.sp)
            }
        }
    } else if (uiState.errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⚠️ 載入失敗", fontWeight = FontWeight.Bold, color = WarningRed)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(uiState.errorMessage!!, color = TextDark)
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
            // 1. 警告
            Text("1. 生效中的天氣警告", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.warnings.isNotEmpty()) Color(0xFFFFF3E0) else Color.White
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (uiState.warnings.isNotEmpty()) {
                        uiState.warnings.forEach { warning ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("⚠️ ", fontSize = 16.sp)
                                Text(text = warning, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WarningRed)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✅ ", fontSize = 16.sp)
                            Text("現時無生效天氣警告", fontSize = 14.sp, color = TextDark)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. 分區天氣 (英文 A-Z 排序)
            Text("2. 分區天氣資訊", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
            Spacer(modifier = Modifier.height(8.dp))
            var expandedDropdown by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        OutlinedButton(
                            onClick = { expandedDropdown = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = uiState.selectedLocation?.let { "${it.nameTc} (${it.nameEn})" } ?: "選擇地點",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }

                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            uiState.locations.forEach { loc ->
                                DropdownMenuItem(
                                    text = { Text("${loc.nameTc} (${loc.nameEn})") },
                                    onClick = {
                                        viewModel.selectLocation(loc)
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.selectedLocation?.temp?.let { "$it°C" } ?: "--°C",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Text("💧 相對濕度: ${uiState.currentHumidity}", color = Color.White, fontSize = 13.sp)
                        Text("☀️ 紫外線: ${uiState.currentUv}", color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. 今日預報
            Text("3. 今日天氣預報", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = uiState.todayForecastDesc, fontSize = 14.sp, color = TextDark, lineHeight = 22.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. 九日預報
            Text("4. 九日天氣預報", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.nineDayForecasts.forEach { day ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${formatDate(day.forecastDate)} (${day.week})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextDark
                                )
                                Text(text = day.forecastWeather, fontSize = 12.sp, color = TextGray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${day.forecastMintemp?.value}°C - ${day.forecastMaxtemp?.value}°C",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PrimaryDarkBlue
                                )
                                Text(
                                    text = "濕度 ${day.forecastMinrh?.value}% - ${day.forecastMaxrh?.value}%",
                                    fontSize = 12.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

fun formatDate(rawDate: String): String {
    if (rawDate.length == 8) {
        val month = rawDate.substring(4, 6).toIntOrNull()
        val day = rawDate.substring(6, 8).toIntOrNull()
        if (month != null && day != null) {
            return "${month}月${day}日"
        }
    }
    return rawDate
}
