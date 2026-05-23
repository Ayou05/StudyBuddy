package com.studybuddy.v2.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ═════════════════════════════════════════════════════════════════════════════
// AppColors — Claude 设计语言里 Material 3 ColorScheme 不能直接表达的扩展色
// 通过 CompositionLocal 注入，组件读取 MaterialTheme.appColors.xxx
// ═════════════════════════════════════════════════════════════════════════════
@Immutable
data class AppColors(
    // Surface 三层节奏
    val canvas: Color,
    val surfaceSoft: Color,
    val surfaceCard: Color,
    val surfaceCreamStrong: Color,
    val surfaceDark: Color,
    val surfaceDarkElevated: Color,
    val surfaceDarkSoft: Color,
    // Text 阶梯
    val ink: Color,
    val bodyStrong: Color,
    val body: Color,
    val muted: Color,
    val mutedSoft: Color,
    val onDark: Color,
    val onDarkSoft: Color,
    // Lines
    val hairline: Color,
    val hairlineSoft: Color,
    // Accents
    val accentTeal: Color,
    val accentAmber: Color,
    val success: Color,
    val warning: Color,
    // Partner identity
    val partnerA: Color,
    val partnerB: Color,
    // 鞍部猫专属
    val mascotInk: Color,
    val mascotShip: Color
)

private val LightAppColors = AppColors(
    canvas = ClaudeColors.Canvas,
    surfaceSoft = ClaudeColors.SurfaceSoft,
    surfaceCard = ClaudeColors.SurfaceCard,
    surfaceCreamStrong = ClaudeColors.SurfaceCreamStrong,
    surfaceDark = ClaudeColors.SurfaceDark,
    surfaceDarkElevated = ClaudeColors.SurfaceDarkElevated,
    surfaceDarkSoft = ClaudeColors.SurfaceDarkSoft,
    ink = ClaudeColors.Ink,
    bodyStrong = ClaudeColors.BodyStrong,
    body = ClaudeColors.Body,
    muted = ClaudeColors.Muted,
    mutedSoft = ClaudeColors.MutedSoft,
    onDark = ClaudeColors.OnDark,
    onDarkSoft = ClaudeColors.OnDarkSoft,
    hairline = ClaudeColors.Hairline,
    hairlineSoft = ClaudeColors.HairlineSoft,
    accentTeal = ClaudeColors.AccentTeal,
    accentAmber = ClaudeColors.AccentAmber,
    success = ClaudeColors.Success,
    warning = ClaudeColors.Warning,
    partnerA = ClaudeColors.PartnerA,
    partnerB = ClaudeColors.PartnerB,
    mascotInk = ClaudeColors.MascotInk,
    mascotShip = ClaudeColors.MascotShip
)

private val DarkAppColors = AppColors(
    canvas = ClaudeColorsDark.Canvas,
    surfaceSoft = ClaudeColorsDark.SurfaceSoft,
    surfaceCard = ClaudeColorsDark.SurfaceCard,
    surfaceCreamStrong = ClaudeColorsDark.SurfaceCreamStrong,
    surfaceDark = ClaudeColorsDark.SurfaceDark,
    surfaceDarkElevated = ClaudeColorsDark.SurfaceDarkElevated,
    surfaceDarkSoft = ClaudeColorsDark.SurfaceDarkSoft,
    ink = ClaudeColorsDark.Ink,
    bodyStrong = ClaudeColorsDark.BodyStrong,
    body = ClaudeColorsDark.Body,
    muted = ClaudeColorsDark.Muted,
    mutedSoft = ClaudeColorsDark.MutedSoft,
    onDark = ClaudeColorsDark.OnDark,
    onDarkSoft = ClaudeColorsDark.OnDarkSoft,
    hairline = ClaudeColorsDark.Hairline,
    hairlineSoft = ClaudeColorsDark.HairlineSoft,
    accentTeal = ClaudeColorsDark.AccentTeal,
    accentAmber = ClaudeColorsDark.AccentAmber,
    success = ClaudeColorsDark.Success,
    warning = ClaudeColorsDark.Warning,
    partnerA = ClaudeColorsDark.PartnerA,
    partnerB = ClaudeColorsDark.PartnerB,
    mascotInk = ClaudeColorsDark.MascotInk,
    mascotShip = ClaudeColorsDark.MascotShip
)

private val LocalAppColors = staticCompositionLocalOf { LightAppColors }

val MaterialTheme.appColors: AppColors
    @Composable get() = LocalAppColors.current

// ═════════════════════════════════════════════════════════════════════════════
// 把 AppColors 投影到 Material 3 ColorScheme（让 Material 组件也能用得对）
// ═════════════════════════════════════════════════════════════════════════════
private fun lightScheme(): ColorScheme = lightColorScheme(
    primary = ClaudeColors.Primary,
    onPrimary = ClaudeColors.OnPrimary,
    primaryContainer = ClaudeColors.SurfaceCreamStrong,
    onPrimaryContainer = ClaudeColors.Ink,
    secondary = ClaudeColors.AccentTeal,
    onSecondary = ClaudeColors.OnPrimary,
    secondaryContainer = ClaudeColors.SurfaceCard,
    onSecondaryContainer = ClaudeColors.Ink,
    tertiary = ClaudeColors.AccentAmber,
    onTertiary = ClaudeColors.Ink,
    background = ClaudeColors.Canvas,
    onBackground = ClaudeColors.Ink,
    surface = ClaudeColors.Canvas,
    onSurface = ClaudeColors.Ink,
    surfaceVariant = ClaudeColors.SurfaceCard,
    onSurfaceVariant = ClaudeColors.Body,
    error = ClaudeColors.Error,
    onError = ClaudeColors.OnPrimary,
    outline = ClaudeColors.Hairline,
    outlineVariant = ClaudeColors.HairlineSoft
)

private fun darkScheme(): ColorScheme = darkColorScheme(
    primary = ClaudeColorsDark.Primary,
    onPrimary = ClaudeColorsDark.OnPrimary,
    primaryContainer = ClaudeColorsDark.SurfaceCreamStrong,
    onPrimaryContainer = ClaudeColorsDark.Ink,
    secondary = ClaudeColorsDark.AccentTeal,
    onSecondary = ClaudeColorsDark.OnPrimary,
    secondaryContainer = ClaudeColorsDark.SurfaceCard,
    onSecondaryContainer = ClaudeColorsDark.Ink,
    tertiary = ClaudeColorsDark.AccentAmber,
    onTertiary = ClaudeColorsDark.Ink,
    background = ClaudeColorsDark.Canvas,
    onBackground = ClaudeColorsDark.Ink,
    surface = ClaudeColorsDark.Canvas,
    onSurface = ClaudeColorsDark.Ink,
    surfaceVariant = ClaudeColorsDark.SurfaceCard,
    onSurfaceVariant = ClaudeColorsDark.Body,
    error = ClaudeColorsDark.Error,
    onError = ClaudeColorsDark.OnPrimary,
    outline = ClaudeColorsDark.Hairline,
    outlineVariant = ClaudeColorsDark.HairlineSoft
)

// ═════════════════════════════════════════════════════════════════════════════
// 主题入口 — 整个 v2 唯一的 @Composable 主题函数
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun StudyBuddyV2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) darkScheme() else lightScheme()
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = ClaudeTypography,
            shapes = ClaudeShapes,
            content = content
        )
    }
}
