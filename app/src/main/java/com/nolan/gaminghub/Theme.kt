package com.nolan.gaminghub

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF050A18)
val NavyCard = Color(0xFF0B1530)
val ElectricBlue = Color(0xFF2D7DFF)

private val Scheme = darkColorScheme(
    primary = ElectricBlue, background = Navy, surface = NavyCard,
    onBackground = Color.White, onSurface = Color.White,
    surfaceVariant = NavyCard, onSurfaceVariant = Color(0xFF9FB3D9)
)

@Composable
fun HubTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Scheme, content = content)
