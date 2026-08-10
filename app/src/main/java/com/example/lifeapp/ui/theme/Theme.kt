package com.example.lifeapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryBlue = Color(0xFF1E88E5)
val PrimaryDarkBlue = Color(0xFF1565C0)
val PrimaryLightBlue = Color(0xFFE3F2FD)
val BackgroundLight = Color(0xFFF4F7FA)
val TextDark = Color(0xFF2C3E50)
val TextGray = Color(0xFF7F8C8D)
val WarningRed = Color(0xFFD32F2F)

@Composable
fun LifeAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PrimaryBlue,
            background = BackgroundLight,
            surface = Color.White
        ),
        content = content
    )
}
