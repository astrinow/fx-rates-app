package com.fxrates.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Colours that carry meaning and are not part of the Material scheme. */
@Immutable
data class AppColors(
    val positive: Color,
    val negative: Color,
    val neutral: Color,
)

/** The spacing scale. Layouts use these values instead of literal `dp`. */
@Immutable
data class AppSpacing(
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 16.dp,
    val l: Dp = 24.dp,
)

private val LightAppColors = AppColors(
    positive = Color(0xFF1B873F),
    negative = Color(0xFFC62828),
    neutral = Color(0xFF6B7280),
)
private val DarkAppColors = AppColors(
    positive = Color(0xFF4ADE80),
    negative = Color(0xFFF87171),
    neutral = Color(0xFF9CA3AF),
)

private val LocalAppColors = staticCompositionLocalOf { LightAppColors }
private val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }

object AppTheme {

    val colors: AppColors
        @Composable get() = LocalAppColors.current

    val spacing: AppSpacing
        @Composable get() = LocalAppSpacing.current

}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    CompositionLocalProvider(
        LocalAppColors provides if (isDark) DarkAppColors else LightAppColors,
    ) {
        MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {
            Surface(content = content)
        }
    }
}
