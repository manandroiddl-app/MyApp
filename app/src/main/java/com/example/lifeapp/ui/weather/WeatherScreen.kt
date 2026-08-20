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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.lifeapp.data.model.DistrictRainfall
import com.example.lifeapp.data.model.DistrictTemperature
import com.example.lifeapp.data.model.WeatherWarningItem
import com.example.lifeapp.ui.common.AutoRefreshLifecycleHandler
import com.example.lifeapp.ui.common.FullPageLoading
import com.example.lifeapp.ui.theme.PrimaryDarkBlue
import com.example.lifeapp.ui.theme.PrimaryLightBlue
import com.example.lifeapp.ui.theme.TextDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedWarning by remember { mutableStateOf<WeatherWarningItem?>(null) }
    var showRainfallSheet by remember { mutableStateOf(false) }
    var showTempSheet by remember { mutableStateOf(false) }

    // 🎯 套用統一的 AutoRefreshLifecycleHandler
    AutoRefreshLifecycleHandler(
        onStartRefresh = { viewModel.startAutoRefresh() },
        onStopRefresh = { viewModel.stopAutoRefresh() },
        onResumeFetch = { viewModel.loadWeatherData(isSilent = true) }
    )

    val isInitialLoading = uiState.isLoading && uiState.updateTime.isEmpty()

    if (isInitialLoading) {
        FullPageLoading(color = PrimaryDarkBlue)
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

            val activeWarnings = uiState.warningSummary.filter { warningItem -> warningItem.code != "CANCEL" }
            if (activeWarnings.isNotEmpty()) {
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
                        activeWarnings.forEach { warning ->
                            val isPre8 = warning.isTcPre8()
                            val cardBgColor = if (isPre8) Color(0xFFFFF3E0) else Color.White

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedWarning = warning },
                                colors = CardDefaults.cardColors(containerColor = cardBgColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val iconUrl = warning.getIconUrl()
                                    if (iconUrl != null) {
                                        AsyncImage(
                                            model = iconUrl,
                                            contentDescription = warning.name,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    } else {
                                        Text(
                                            text = if (warning.code == "SWT") "💡 " else "🚨 ",
                                            fontSize = 18.sp
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val displayName = if (warning.code == "SWT" && warning.detailsList.size > 1) {
                                                "${warning.name} (共 ${warning.detailsList.size} 條)"
                                            } else {
                                                warning.name
                                            }

                                            Text(
                                                text = displayName,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPre8) Color(0xFFE65100) else PrimaryDarkBlue,
                                                fontSize = 15.sp
                                            )

                                            if (isPre8) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    color = Color(0xFFFFE0B2),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "8號預警",
                                                        color = Color(0xFFE65100),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📍 本港氣象觀察",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = PrimaryDarkBlue
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showTempSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryLightBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text(
                                    text = if (uiState.isApparentTempMode) "🌡️ 查看分區體感" else "🌡️ 查看分區氣溫",
                                    color = PrimaryDarkBlue,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { showRainfallSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryLightBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text(
                                    text = "🌧️ 查看分區雨量",
                                    color = PrimaryDarkBlue,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val validHumidities = uiState.districtTemperatures.mapNotNull { districtTemp -> districtTemp.humidityValue }
                            val avgHumidity = if (validHumidities.isNotEmpty()) validHumidities.average().toInt() else null

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💧 平均相對濕度", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (avgHumidity != null) "$avgHumidity%" else "--",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDarkBlue
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("☀️ 紫外線指數", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                val uvText = uiState.uvIndexInfo?.let { uv -> "${uv.value} (${uv.desc})" } ?: "無資料"
                                Text(
                                    text = uvText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDarkBlue
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PrimaryLightBlue)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "📝 今日天氣預報與概況", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryDarkBlue)
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.generalSituation.isNotBlank()) {
                            Text(text = "🌐 華南天氣概況", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryDarkBlue)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = uiState.generalSituation, fontSize = 13.sp, lineHeight = 19.sp, color = Color.DarkGray)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFB3E5FC))
                        }

                        if (uiState.todayForecast.isNotBlank()) {
                            Text(text = "🌤️ 本港地區天氣預報", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryDarkBlue)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = uiState.todayForecast, fontSize = 13.sp, lineHeight = 19.sp, color = Color.DarkGray)
                        }

                        if (uiState.outlook.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFB3E5FC))
                            Text(text = "🔮 未來展望", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryDarkBlue)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = uiState.outlook, fontSize = 13.sp, lineHeight = 19.sp, color = Color.DarkGray)
                        }

                        if (uiState.tcInfo.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFB3E5FC))
                            Text(text = "🌀 熱帶氣旋消息", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFD32F2F))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = uiState.tcInfo, fontSize = 13.sp, lineHeight = 19.sp, color = Color.DarkGray)
                        }

                        if (uiState.fireDangerWarning.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFB3E5FC))
                            Text(text = "🔥 火災危險警告描述", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE65100))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = uiState.fireDangerWarning, fontSize = 13.sp, lineHeight = 19.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            if (uiState.nineDayForecast.isNotEmpty()) {
                item {
                    Text(text = "📅 九天天氣預報", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryDarkBlue)
                }
                items(uiState.nineDayForecast) { forecast ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                val formattedDate = if (forecast.forecastDate.length == 8) {
                                    "${forecast.forecastDate.substring(4, 6)}月${forecast.forecastDate.substring(6, 8)}日"
                                } else forecast.forecastDate

                                Text(
                                    text = "$formattedDate (${forecast.week})",
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDarkBlue,
                                    fontSize = 15.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = forecast.forecastWeather,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = Color.DarkGray
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🌡️ ${forecast.forecastMintemp.value}°C - ${forecast.forecastMaxtemp.value}°C",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryDarkBlue
                                    )
                                    if (forecast.forecastRh.minrh > 0) {
                                        Text(
                                            text = "💧 ${forecast.forecastRh.minrh}% - ${forecast.forecastRh.maxrh}%",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                if (forecast.wind.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "🚩 ${forecast.wind}",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                }

                                if (forecast.psr.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "🌧️ 降雨概率: ${forecast.psr}",
                                        fontSize = 12.sp,
                                        color = PrimaryDarkBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (forecast.iconCode > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(
                                            color = Color(0xFFE0F2FE),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = "https://www.hko.gov.hk/images/HKOWxIconOutline/pic${forecast.iconCode}.png",
                                        contentDescription = "Weather Icon",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedWarning?.let { warning ->
        val scrollState = rememberScrollState()
        AlertDialog(
            onDismissRequest = { selectedWarning = null },
            title = {
                val titleText = if (warning.code == "SWT" && warning.detailsList.size > 1) {
                    "💡 特別天氣提示 (共 ${warning.detailsList.size} 條)"
                } else {
                    "⚠️ ${warning.name} 詳情"
                }
                Text(text = titleText, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
            },
            text = {
                Box(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(scrollState)
                ) {
                    if (warning.detailsList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            warning.detailsList.forEachIndexed { index, tip ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        if (warning.detailsList.size > 1) {
                                            Text(
                                                text = "📌 提示 ${index + 1}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = PrimaryDarkBlue
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                        Text(
                                            text = tip,
                                            fontSize = 13.sp,
                                            lineHeight = 19.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = warning.details,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedWarning = null }) {
                    Text("確定", color = PrimaryDarkBlue)
                }
            }
        )
    }

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

    if (showTempSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTempSheet = false },
            containerColor = Color.White
        ) {
            TempSheetContent(
                tempList = uiState.districtTemperatures,
                isApparentTempMode = uiState.isApparentTempMode,
                onToggleMode = { viewModel.toggleTemperatureMode() },
                onClose = { showTempSheet = false }
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

@Composable
fun TempSheetContent(
    tempList: List<DistrictTemperature>,
    isApparentTempMode: Boolean,
    onToggleMode: () -> Unit,
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
            val titleText = if (isApparentTempMode) "🌡️ 全港分區體感溫度" else "🌡️ 全港分區即時氣溫"
            Text(titleText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
            TextButton(onClick = onClose) {
                Text("關閉", color = Color.Gray)
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val subTitleText = if (isApparentTempMode) "結合分區風速與濕度計算之體感" else "即時錄得之區域氣溫"
            Text(subTitleText, fontSize = 12.sp, color = Color.Gray)

            FilterChip(
                selected = isApparentTempMode,
                onClick = onToggleMode,
                label = {
                    Text(
                        text = if (isApparentTempMode) "顯示實測氣溫" else "切換體感溫度",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFFE0B2),
                    selectedLabelColor = Color(0xFFE65100),
                    containerColor = Color(0xFFE3F2FD),
                    labelColor = PrimaryDarkBlue
                )
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        if (tempList.isEmpty()) {
            Text("暫無分區氣溫數據", color = Color.Gray, modifier = Modifier.padding(vertical = 20.dp))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(tempList) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${item.placeTc} (${item.placeEn})", fontSize = 14.sp, color = TextDark)
                        
                        val displayText = if (isApparentTempMode && item.apparentTempValue != null) {
                            "${item.apparentTempValue}${item.unit}"
                        } else {
                            "${item.tempValue}${item.unit}"
                        }

                        val badgeBg = if (isApparentTempMode) Color(0xFFFFF3E0) else Color(0xFFE3F2FD)
                        val badgeText = if (isApparentTempMode) Color(0xFFE65100) else PrimaryDarkBlue

                        Surface(
                            color = badgeBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = displayText,
                                color = badgeText,
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
