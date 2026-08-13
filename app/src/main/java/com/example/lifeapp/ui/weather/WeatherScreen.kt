package com.example.lifeapp.ui.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifeapp.data.model.PlaceRainfallData
import com.example.lifeapp.ui.theme.PrimaryDarkBlue
import com.example.lifeapp.ui.theme.PrimaryLightBlue
import com.example.lifeapp.ui.theme.TextDark
import com.example.lifeapp.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel
) {
    val state by viewModel.uiState.collectAsState()
    var showRainfallSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (state.error != null) {
            Text(
                text = state.error ?: "載入失敗",
                color = Color.Red,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            val rhrread = state.weatherData?.rhrread
            val fnd = state.weatherData?.fnd

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("香港即時天氣", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                Spacer(modifier = Modifier.height(12.dp))

                // 1. 生效中天氣警告/提示卡片
                rhrread?.warningMessage?.filter { it.isNotBlank() }?.let { warnings ->
                    if (warnings.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("⚠️ 天氣警告與提示", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                warnings.forEach { warning ->
                                    Text("• $warning", color = Color(0xFFC62828), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // 2. UV 紫外線指數卡片 (若有資料)
                rhrread?.uvindex?.data?.firstOrNull()?.let { uv ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("☀️ 紫外線指數", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                            Text("${uv.value} (${uv.desc})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFF57F17))
                        }
                    }
                }

                // 3. 分區氣溫 (右側附帶 🌧️ 雨量按鈕)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("分區氣溫", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            
                            // 🌧️ 雨量按鈕 (On Top 新增)
                            Button(
                                onClick = { showRainfallSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryLightBlue),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("🌧️ 查看分區雨量", color = PrimaryDarkBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        rhrread?.temperature?.data?.forEach { temp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(temp.place, fontSize = 14.sp, color = TextDark)
                                Text("${temp.value}°C", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                            }
                        }
                    }
                }

                // 4. 九天天氣預報區塊
                fnd?.weatherForecast?.let { forecasts ->
                    if (forecasts.isNotEmpty()) {
                        Text("📅 九天天氣預報", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                        Spacer(modifier = Modifier.height(8.dp))

                        fnd.generalSituation?.let { situation ->
                            Text(situation, fontSize = 12.sp, color = TextGray, modifier = Modifier.padding(bottom = 12.dp))
                        }

                        forecasts.forEach { day ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${day.forecastDate} (${day.week})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                        Text(day.forecastWeather, fontSize = 12.sp, color = TextGray)
                                        if (day.PSR.isNotBlank()) {
                                            Text("顯著降雨概率: ${day.PSR}", fontSize = 11.sp, color = PrimaryDarkBlue)
                                        }
                                    }
                                    Text(
                                        "${day.forecastMintemp?.value ?: "-"} - ${day.forecastMaxtemp?.value ?: "-"}°C",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = PrimaryDarkBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. 分區雨量 ModalBottomSheet (On Top 新增)
        if (showRainfallSheet) {
            ModalBottomSheet(
                onDismissRequest = { showRainfallSheet = false },
                containerColor = Color.White
            ) {
                RainfallSheetContent(
                    rainfallData = state.weatherData?.rhrread?.rainfall?.data ?: emptyList(),
                    onClose = { showRainfallSheet = false }
                )
            }
        }
    }
}

@Composable
fun RainfallSheetContent(
    rainfallData: List<PlaceRainfallData>,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🌧️ 全港分區即時雨量", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
            TextButton(onClick = onClose) {
                Text("關閉", color = TextGray)
            }
        }
        Text("過去一小時錄得之雨量 (毫米)", fontSize = 12.sp, color = TextGray)
        
        Spacer(modifier = Modifier.height(12.dp))

        if (rainfallData.isEmpty()) {
            Text("暫無分區雨量數據", color = TextGray, modifier = Modifier.padding(vertical = 20.dp))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(rainfallData) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.place, fontSize = 15.sp, color = TextDark)
                        
                        val rainText = if (item.max == 0) "0 mm" else "${item.min} - ${item.max} mm"
                        val badgeColor = if (item.max > 0) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
                        val textColor = if (item.max > 0) PrimaryDarkBlue else TextGray

                        Surface(
                            color = badgeColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = rainText,
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}
