package com.example.lifeapp.ui.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import coil.compose.AsyncImage // 👈 導入 Coil 圖片加載元件
import com.example.lifeapp.data.model.DistrictRainfall
import com.example.lifeapp.data.model.DistrictTemperature
import com.example.lifeapp.data.model.WeatherWarningItem
import com.example.lifeapp.ui.theme.PrimaryDarkBlue
import com.example.lifeapp.ui.theme.PrimaryLightBlue
import com.example.lifeapp.ui.theme.TextDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedWarning by remember { mutableStateOf<WeatherWarningItem?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var userChosenDistrict by remember { mutableStateOf<DistrictTemperature?>(null) }
    var showRainfallSheet by remember { mutableStateOf(false) }

    val activeDistrict = userChosenDistrict
        ?: uiState.districtTemperatures.firstOrNull { it.placeTc == "香港天文台" }
        ?: uiState.districtTemperatures.firstOrNull()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryDarkBlue)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 頂部最後更新時間
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "香港天氣概況", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PrimaryDarkBlue)
                    Text(
                        text = "更新時間: ${uiState.updateTime}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // 2. 生效中警告 (點擊彈窗)
            if (uiState.warningSummary.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE3F2FD), shape = RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "⚠️ 生效中的天氣警告 (點擊查看詳情)",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkBlue,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.warningSummary.forEach { warning ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedWarning = warning },
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🚨 ${warning.name}", fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                                }
                            }
                        }
                    }
                }
            }

            // 3. 分區天氣 Dropdown 選單與雨量按鈕
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📍 分區氣象觀察", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryDarkBlue)
                            
                            Button(
                                onClick = { showRainfallSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryLightBlue),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("🌧️ 查看雨量", color = PrimaryDarkBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = activeDistrict?.let { "${it.placeTc} (${it.placeEn})" } ?: "載入地點中...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                uiState.districtTemperatures.forEach { district ->
                                    DropdownMenuItem(
                                        text = { Text("${district.placeTc} (${district.placeEn})") },
                                        onClick = {
                                            userChosenDistrict = district
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        activeDistrict?.let { dist ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🌡️ 氣溫", fontSize = 13.sp, color = Color.Gray)
                                    Text("${dist.tempValue}${dist.unit}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("💧 相對濕度", fontSize = 13.sp, color = Color.Gray)
                                    Text("${dist.humidityValue ?: "--"}%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                                }
                            }
                        }

                        uiState.uvIndexInfo?.let { uv ->
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("☀️ 全港紫外線指數", fontSize = 14.sp, color = Color.Gray)
                                Text("${uv.value} (${uv.desc})", fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                            }
                        }
                    }
                }
            }

            // 4. 今日天氣預報
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PrimaryLightBlue)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "📝 今日天氣預報", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryDarkBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = uiState.todayForecast, fontSize = 14.sp, lineHeight = 20.sp, color = Color.DarkGray)
                    }
                }
            }

            // 5. 九天天氣預報
            if (uiState.nineDayForecast.isNotEmpty()) {
                item {
                    Text(text = "📅 九天天氣預報", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryDarkBlue)
                }
                items(uiState.nineDayForecast) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // 標題列：Icon、日期、星期與氣溫範圍
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // 🖼️ 1. 使用 Coil AsyncImage 渲染天氣圖示
                                    if (item.iconCode > 0) {
                                        AsyncImage(
                                            model = "https://www.hko.gov.hk/images/HKOWxIconOutline/pic${item.iconCode}.png",
                                            contentDescription = "Weather Icon",
                                            modifier = Modifier
                                                .size(32.dp)
                                                .padding(end = 8.dp)
                                        )
                                    }

                                    val formattedDate = if (item.forecastDate.length == 8) {
                                        "${item.forecastDate.substring(4, 6)}月${item.forecastDate.substring(6, 8)}日"
                                    } else item.forecastDate

                                    Text(
                                        text = "$formattedDate (${item.week})",
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryDarkBlue,
                                        fontSize = 15.sp
                                    )
                                }

                                Text(
                                    text = "${item.forecastMintemp.value}°C - ${item.forecastMaxtemp.value}°C",
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDarkBlue,
                                    fontSize = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 詳細天氣描述
                            Text(
                                text = item.forecastWeather,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = Color.DarkGray
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 🚩 風向風力
                            if (item.wind.isNotBlank()) {
                                Text(
                                    text = "🚩 風力: ${item.wind}",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            // 💧 濕度範圍 & 🌧️ 降雨概率
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.forecastRh.minrh > 0) {
                                    Text(
                                        text = "💧 濕度: ${item.forecastRh.minrh}% - ${item.forecastRh.maxrh}%",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                if (item.psr.isNotBlank()) {
                                    Text(
                                        text = "🌧️ 降雨概率: ${item.psr}",
                                        fontSize = 12.sp,
                                        color = PrimaryDarkBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 生效中警告詳細彈窗 (帶 Scroller)
    selectedWarning?.let { warning ->
        val scrollState = rememberScrollState()
        AlertDialog(
            onDismissRequest = { selectedWarning = null },
            title = { Text(text = "⚠️ ${warning.name} 詳情", fontWeight = FontWeight.Bold, color = PrimaryDarkBlue) },
            text = {
                Box(
                    modifier = Modifier
                        .heightIn(max = 350.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = warning.details,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = Color.DarkGray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedWarning = null }) {
                    Text("確定", color = PrimaryDarkBlue)
                }
            }
        )
    }

    // 分區雨量 ModalBottomSheet
    if (showRainfallSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRainfallSheet = false },
            containerColor = Color.White
        ) {
            RainfallSheetContent(
                rainfallList = uiState.districtRainfall,
                onClose = { showRainfallSheet = false }
            )
        }
    }
}

@Composable
fun RainfallSheetContent(
    rainfallList: List<DistrictRainfall>,
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
                Text("關閉", color = Color.Gray)
            }
        }
        Text("過去一小時錄得之雨量 (毫米)", fontSize = 12.sp, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(12.dp))

        if (rainfallList.isEmpty()) {
            Text("暫無分區雨量數據", color = Color.Gray, modifier = Modifier.padding(vertical = 20.dp))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(rainfallList) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${item.placeTc} (${item.placeEn})", fontSize = 14.sp, color = TextDark)
                        
                        val rainText = if (item.max == 0) "0 mm" else "${item.min} - ${item.max} mm"
                        val badgeColor = if (item.max > 0) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
                        val textColor = if (item.max > 0) PrimaryDarkBlue else Color.Gray

                        Surface(
                            color = badgeColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = rainText,
                                color = textColor,
                                fontSize = 12.sp,
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
