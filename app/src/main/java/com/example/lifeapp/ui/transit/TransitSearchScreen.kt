package com.example.lifeapp.ui.transit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifeapp.data.model.OperatorCompany
import com.example.lifeapp.ui.common.AutoRefreshLifecycleHandler
import com.example.lifeapp.ui.theme.PrimaryDarkBlue

private val BluePrimary = Color(0xFF1976D2)
private val BlueOnPrimary = Color(0xFFFFFFFF)
private val BlueContainer = Color(0xFFE3F2FD)
private val BlueOnContainer = Color(0xFF0D47A1)

@Composable
fun TransitSearchScreen(
    viewModel: TransitSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    AutoRefreshLifecycleHandler(
        onStartRefresh = { viewModel.onResumeRefresh() },
        onStopRefresh = { viewModel.onPauseStopRefresh() },
        onResumeFetch = { viewModel.refreshCurrentEtasImmediately() }
    )

    val customColorScheme = lightColorScheme(
        primary = BluePrimary,
        onPrimary = BlueOnPrimary,
        primaryContainer = BlueContainer,
        onPrimaryContainer = BlueOnContainer
    )

    MaterialTheme(colorScheme = customColorScheme) {
        Scaffold(
            bottomBar = {
                if (uiState.selectedRoute != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { viewModel.clearSelectedRoute() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                contentPadding = PaddingValues(vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("返回搜尋結果", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            val topPadding = paddingValues.calculateTopPadding()
            val bottomPadding = if (uiState.selectedRoute != null) paddingValues.calculateBottomPadding() else 0.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topPadding, bottom = bottomPadding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val route = uiState.selectedRoute
                    if (route != null) {
                        val serviceTypeInt = route.serviceType?.toIntOrNull() ?: 1
                        val isSpecialService = serviceTypeInt > 1

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

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = route.routeName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = PrimaryDarkBlue
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${route.originZh} ➔ ${route.destinationZh}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray,
                            modifier = Modifier.weight(1f)
                        )

                        if (isSpecialService) {
                            Spacer(modifier = Modifier.width(4.dp))
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
                    } else {
                        Text(
                            text = "公共交通查詢",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = PrimaryDarkBlue
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (uiState.selectedRoute == null) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            TabRow(
                                selectedTabIndex = uiState.currentTab.ordinal,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Tab(
                                    selected = uiState.currentTab == TransitTab.SEARCH,
                                    onClick = { viewModel.selectTab(TransitTab.SEARCH) },
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("搜尋路線", fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )
                                Tab(
                                    selected = uiState.currentTab == TransitTab.BOOKMARK,
                                    onClick = { viewModel.selectTab(TransitTab.BOOKMARK) },
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("已收藏", fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                when (uiState.currentTab) {
                                    TransitTab.SEARCH -> SearchTabContent(uiState = uiState, viewModel = viewModel)
                                    TransitTab.BOOKMARK -> BookmarkTabContent(uiState = uiState, viewModel = viewModel)
                                }
                            }
                        }
                    } else {
                        // 修正位置：對齊 RouteDetailContent 嘅新參數簽名
                        RouteDetailContent(
                            uiState = uiState,
                            onBackClick = { viewModel.clearSelectedRoute() },
                            onToggleBookmark = { stop -> viewModel.toggleBookmark(stop) },
                            onToggleTrackVehicle = { stopId, seq, timestamp ->
                                viewModel.toggleTrackVehicle(stopId, seq, timestamp)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 輔助函式：轉換巴士公司 Enum 顯示名稱
 */
private fun formatCompanyDisplayName(company: OperatorCompany?): String {
    return when (company) {
        OperatorCompany.KMB -> "九巴"
        OperatorCompany.CTB -> "城巴"
        OperatorCompany.NWFB -> "新巴"
        OperatorCompany.NLB -> "嶼巴"
        OperatorCompany.MTR_BUS -> "港鐵巴士"
        else -> "巴士"
    }
}
