package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkMintPrimary,
    onPrimary = DarkForestBackground,
    primaryContainer = ForestGreen,
    onPrimaryContainer = MintTint,
    secondary = DarkTerracotta,
    onSecondary = DarkForestBackground,
    tertiary = WarmGold,
    background = DarkForestBackground,
    onBackground = DarkCreamText,
    surface = DarkForestSurface,
    onSurface = DarkCreamText,
    surfaceVariant = DarkForestSurfaceVariant,
    onSurfaceVariant = Color(0xFFB7C2B9),
    outline = Color(0xFF3E5246),
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = MintTint,
    onPrimaryContainer = ForestGreen,
    secondary = Terracotta,
    onSecondary = Color.White,
    tertiary = WarmGold,
    background = CreamBackground,
    onBackground = EspressoInk,
    surface = CreamBackground,
    onSurface = EspressoInk,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = MutedOlive,
    outline = SoftBorder,
)

@Composable
fun AyasMarketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
