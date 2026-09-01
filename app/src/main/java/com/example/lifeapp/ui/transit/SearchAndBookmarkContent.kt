package com.example.lifeapp.ui.transit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifeapp.data.model.OperatorCompany
import com.example.lifeapp.ui.common.FullPageLoading
import com.example.lifeapp.ui.theme.PrimaryDarkBlue

private val BluePrimary = Color(0xFF1976D2)
private val BlueContainer = Color(0xFFE3F2FD)
private val BlueOnContainer = Color(0xFF0D47A1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTabContent(
    uiState: TransitUiState,
    viewModel: TransitSearchViewModel
) {
    var showCompanySheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.isLoadingRoutes) {
                FullPageLoading()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(uiState.filteredRoutes) { route ->
                        val serviceTypeInt = route.serviceType?.toIntOrNull() ?: 1
                        val isSpecialService = serviceTypeInt > 1

                        ListItem(
                            headlineContent = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = BlueContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = formatCompanyDisplayName(route.company),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BlueOnContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = route.routeName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = PrimaryDarkBlue
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = "${route.originZh} ➔ ${route.destinationZh}",
                                            fontSize = 14.sp,
                                            color = Color.DarkGray,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (isSpecialService) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            color = Color(0xFFFFE0B2),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "特別班次",
                                                color = Color(0xFFE65100),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.clickable { viewModel.selectRoute(route) }
                        )
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            color = BlueContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
            ) {
                // 搜尋列與公司過濾按鈕放在同一列
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.searchQuery.ifEmpty { "請點擊下方按鈕輸入路線..." },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (uiState.searchQuery.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.searchQuery.isEmpty()) Color.Gray else PrimaryDarkBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 右側選取公司 Filter 按鈕
                    Surface(
                        onClick = { showCompanySheet = true },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter Company",
                                tint = BluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = uiState.selectedCompany?.let { formatCompanyDisplayName(it) } ?: "全部公司",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = PrimaryDarkBlue
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = PrimaryDarkBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 0.dp)
                ) {
                    items(uiState.numericChips) { num ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.onChipClicked(num) },
                            label = {
                                Text(
                                    text = num.toString(),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            },
                            modifier = Modifier.height(34.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = PrimaryDarkBlue
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = BluePrimary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(vertical = 0.dp)
                    ) {
                        items(uiState.letterChips) { letter ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.onChipClicked(letter) },
                                label = {
                                    Text(
                                        text = letter.toString(),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                },
                                modifier = Modifier.height(34.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White,
                                    labelColor = PrimaryDarkBlue
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = false,
                                    borderColor = BluePrimary.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { viewModel.onBackspaceClicked() },
                        enabled = uiState.searchQuery.isNotEmpty(),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Backspace",
                            tint = if (uiState.searchQuery.isNotEmpty()) PrimaryDarkBlue else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // 向上彈出的公司選擇 ModalBottomSheet
    if (showCompanySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCompanySheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 8.dp)
            ) {
                Text(
                    text = "選擇交通公司",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDarkBlue,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // 「全部」選項
                ListItem(
                    headlineContent = {
                        Text(
                            text = "全部公司",
                            fontWeight = if (uiState.selectedCompany == null) FontWeight.Bold else FontWeight.Normal,
                            color = if (uiState.selectedCompany == null) BluePrimary else Color.Black
                        )
                    },
                    trailingContent = {
                        if (uiState.selectedCompany == null) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = BluePrimary)
                        }
                    },
                    modifier = Modifier.clickable {
                        viewModel.selectCompany(null)
                        showCompanySheet = false
                    }
                )

                // 動態公司選項
                uiState.availableCompanies.forEach { company ->
                    val isSelected = uiState.selectedCompany == company
                    ListItem(
                        headlineContent = {
                            Text(
                                text = formatCompanyDisplayName(company),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) BluePrimary else Color.Black
                            )
                        },
                        trailingContent = {
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = BluePrimary)
                            }
                        },
                        modifier = Modifier.clickable {
                            viewModel.selectCompany(company)
                            showCompanySheet = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarkTabContent(
    uiState: TransitUiState,
    viewModel: TransitSearchViewModel
) {
    if (uiState.bookmarks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("尚未新增任何收藏車站", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.bookmarks) { bookmark ->
                val etaList = uiState.bookmarkEtaMap[bookmark.bookmarkId] ?: emptyList()
                
                val operatorCompany = try {
                    OperatorCompany.valueOf(bookmark.company)
                } catch (_: Exception) {
                    OperatorCompany.KMB
                }

                val serviceTypeInt = bookmark.serviceType?.toIntOrNull() ?: 1
                val isKmbSpecialService = operatorCompany == OperatorCompany.KMB && serviceTypeInt > 1

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectBookmarkRoute(bookmark) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // 1. 頂部列：[公司] 路線名稱 起點 ➔ 終點  [特別班次(靠右)]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = BlueContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = formatCompanyDisplayName(operatorCompany),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = BlueOnContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = bookmark.routeName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = PrimaryDarkBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${bookmark.originZh} ➔ ${bookmark.destinationZh}",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // 九巴特別班次標籤（放在同一行最右側）
                            if (isKmbSpecialService) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFFFE0B2),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "特別班次",
                                        color = Color(0xFFE65100),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color(0xFFEEEEEE)
                        )

                        // 2. 車站名稱 與 書籤 Icon (右對齊)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = bookmark.stopNameZh,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDarkBlue,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.removeBookmark(bookmark.bookmarkId) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = "Remove Bookmark",
                                    tint = PrimaryDarkBlue
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color(0xFFEEEEEE)
                        )

                        // 3. 底部分欄 Layout (對齊詳細頁面邏輯與字體規格)
                        val hasAnyContent = etaList.any { !it.etaTimestamp.isNullOrEmpty() || !it.remarkZh.isNullOrEmpty() }

                        if (!hasAnyContent) {
                            // 無數據時對齊詳細頁面提示
                            Text(
                                text = "暫無到站時間數據",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF5C6BC0),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // --- 左側：ETA 2 與 ETA 3 ( weight 0.9f ) ---
                                Column(
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .padding(end = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val eta2 = etaList.getOrNull(1)
                                    val eta3 = etaList.getOrNull(2)

                                    // ETA 2 列
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (eta2 != null) {
                                            val minutes2 = getEtaMinutes(eta2.etaTimestamp)
                                            val clockTime2 = formatEtaTimeClock(eta2.etaTimestamp)
                                            
                                            // 字體 Size 對齊詳細頁面 bodyMedium (14sp/Bold) + 紅字邏輯 (<=3分)
                                            Text(
                                                text = formatEtaDisplay(minutes2, eta2.remarkZh),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (minutes2 != null && minutes2 <= 3) Color(0xFFD32F2F) else Color(0xFF0D47A1)
                                            )
                                            if (!eta2.etaTimestamp.isNullOrEmpty()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.SportsScore,
                                                    contentDescription = null,
                                                    tint = Color(0xFF1565C0),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                // 到站時間對齊詳細頁面 13.sp
                                                Text(
                                                    text = clockTime2,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1565C0)
                                                )
                                            }
                                        } else {
                                            Text(text = "--", style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                                        }
                                    }

                                    // ETA 3 列
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (eta3 != null) {
                                            val minutes3 = getEtaMinutes(eta3.etaTimestamp)
                                            val clockTime3 = formatEtaTimeClock(eta3.etaTimestamp)
                                            
                                            Text(
                                                text = formatEtaDisplay(minutes3, eta3.remarkZh),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (minutes3 != null && minutes3 <= 3) Color(0xFFD32F2F) else Color(0xFF0D47A1)
                                            )
                                            if (!eta3.etaTimestamp.isNullOrEmpty()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.SportsScore,
                                                    contentDescription = null,
                                                    tint = Color(0xFF1565C0),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = clockTime3,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1565C0)
                                                )
                                            }
                                        } else {
                                            Text(text = "--", style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                                        }
                                    }
                                }

                                VerticalDivider(
                                    modifier = Modifier
                                        .height(44.dp)
                                        .padding(horizontal = 2.dp),
                                    color = Color(0xFFEEEEEE)
                                )

                                // --- 右側：ETA 1 ( weight 1.1f，ETA1 與 到站時間1 同一行，巨型字體佔雙行高 ) ---
                                val eta1 = etaList.firstOrNull()
                                Column(
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .padding(start = 8.dp),
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (eta1 != null) {
                                        val minutes1 = getEtaMinutes(eta1.etaTimestamp)
                                        val clockTime1 = formatEtaTimeClock(eta1.etaTimestamp)
                                        val eta1Color = if (minutes1 != null && minutes1 <= 3) Color(0xFFD32F2F) else Color(0xFF0D47A1)

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 倒數分鐘 (例如 "1 分鐘" / "即將到站")
                                            Text(
                                                text = formatEtaDisplay(minutes1, eta1.remarkZh),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = eta1Color
                                            )

                                            // 同一行顯示 <格仔旗> <到站時間1>
                                            if (!eta1.etaTimestamp.isNullOrEmpty()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.SportsScore,
                                                    contentDescription = "到達時間",
                                                    tint = eta1Color,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = clockTime1,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = eta1Color
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "--",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
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
