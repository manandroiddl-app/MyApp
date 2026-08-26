package com.example.lifeapp.ui.transit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifeapp.data.model.OperatorCompany
import com.example.lifeapp.ui.common.FullPageLoading
import com.example.lifeapp.ui.theme.PrimaryDarkBlue

private val BluePrimary = Color(0xFF1976D2)
private val BlueContainer = Color(0xFFE3F2FD)
private val BlueOnContainer = Color(0xFF0D47A1)

@Composable
fun SearchTabContent(
    uiState: TransitUiState,
    viewModel: TransitSearchViewModel
) {
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
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
            contentPadding = PaddingValues(8.dp)
        ) {
            items(uiState.bookmarks) { bookmark ->
                val etaList = uiState.selectedStopEtaMap[bookmark.stopId] ?: emptyList()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { viewModel.selectBookmarkRoute(bookmark) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = BlueContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${formatCompanyDisplayName(bookmark.company)} ${bookmark.routeName}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = BlueOnContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "往 ${bookmark.destinationZh}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "車站：${bookmark.stopNameZh}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDarkBlue
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            if (etaList.isEmpty()) {
                                Text(
                                    text = "載入中或沒有預計班次",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            } else {
                                etaList.take(3).forEach { eta ->
                                    val etaMinutes = getEtaMinutes(eta.etaTimestamp)
                                    val display = formatEtaDisplay(etaMinutes, eta.remarkZh)

                                    Text(
                                        text = display,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BluePrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { viewModel.removeBookmark(bookmark.bookmarkId) }) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Remove Bookmark",
                                tint = Color(0xFFFFC107)
                            )
                        }
                    }
                }
            }
        }
    }
}
