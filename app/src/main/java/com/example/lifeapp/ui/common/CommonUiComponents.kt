package com.example.lifeapp.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.lifeapp.ui.theme.PrimaryBlue

/**
 * 🛡️ 全局統一的全頁加載轉圈元件
 */
@Composable
fun FullPageLoading(
    modifier: Modifier = Modifier,
    color: Color = PrimaryBlue
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = color)
    }
}
