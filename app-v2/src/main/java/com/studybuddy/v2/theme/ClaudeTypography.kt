package com.studybuddy.v2.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ═════════════════════════════════════════════════════════════════════════════
// Claude Design Language — Typography
//
// 衬线显示字（display-xl/lg/md/sm）仿 Copernicus / Tiempos Headline，
// 用 FontFamily.Serif 兜底；P3 抛光阶段下载 EB Garamond / Cormorant Garamond
// 接入 R.font 后只需替换下面 SerifDisplay 一个值。
//
// Body / Title / Button / Nav 用 humanist sans —— FontFamily.SansSerif 兜底，
// 后期换 Inter。
//
// Code / Timer 用 Monospace 兜底 —— 后期换 JetBrains Mono。
//
// CRITICAL：所有 TextStyle 全字段构造，永不调 .copy()。Compose UI text 1.6.x 的
//          TextStyle.copy() 触发 PlatformTextStyle.spanStyle NPE 已确认。需要 weight
//          变体时显式定义新 token，或在 Text Composable 用 fontWeight 参数合并。
// ═════════════════════════════════════════════════════════════════════════════

private val SerifDisplay: FontFamily = FontFamily.Serif        // TODO: R.font.eb_garamond
private val SansBody:     FontFamily = FontFamily.SansSerif    // TODO: R.font.inter
private val MonoCode:     FontFamily = FontFamily.Monospace    // TODO: R.font.jetbrains_mono

object ClaudeType {
    // ─── Display (Serif, weight 400, negative tracking) ──────────────────────
    val DisplayXl = TextStyle(
        fontFamily = SerifDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 56.sp,        // 桌面 64sp，移动端略降
        lineHeight = 60.sp,
        letterSpacing = (-1.5).sp
    )
    val DisplayLg = TextStyle(
        fontFamily = SerifDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1.0).sp
    )
    val DisplayMd = TextStyle(
        fontFamily = SerifDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    )
    val DisplaySm = TextStyle(
        fontFamily = SerifDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    )

    // ─── Title (Sans, weight 500) ─────────────────────────────────────────────
    val TitleLg = TextStyle(
        fontFamily = SansBody,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    )
    val TitleMd = TextStyle(
        fontFamily = SansBody,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
    val TitleSm = TextStyle(
        fontFamily = SansBody,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )

    // ─── Body (Sans, weight 400) ──────────────────────────────────────────────
    val BodyMd = TextStyle(
        fontFamily = SansBody,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
    val BodySm = TextStyle(
        fontFamily = SansBody,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )

    // ─── Caption (Sans, weight 500) ───────────────────────────────────────────
    val Caption = TextStyle(
        fontFamily = SansBody,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )
    // 大写小标 —— "FOCUS · 25 MIN"、"NEW" 等
    val CaptionUppercase = TextStyle(
        fontFamily = SansBody,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.5.sp
    )

    // ─── Code / Timer (Monospace) ────────────────────────────────────────────
    val Code = TextStyle(
        fontFamily = MonoCode,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum, lnum"
    )
    // 专注倒计时 —— 大号等宽数字
    val TimerXl = TextStyle(
        fontFamily = MonoCode,
        fontWeight = FontWeight.Bold,
        fontSize = 72.sp,
        lineHeight = 80.sp,
        letterSpacing = (-2).sp,
        fontFeatureSettings = "tnum, lnum"
    )
    val TimerLg = TextStyle(
        fontFamily = MonoCode,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = "tnum, lnum"
    )
    val TimerMd = TextStyle(
        fontFamily = MonoCode,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-1).sp,
        fontFeatureSettings = "tnum, lnum"
    )

    // ─── Button / Nav-link (Sans 500, line-height 1.0 紧凑) ───────────────────
    val Button = TextStyle(
        fontFamily = SansBody,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )
    val NavLink = TextStyle(
        fontFamily = SansBody,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
}

// 映射到 Material 3 Typography（保持组件库默认行为可用）
val ClaudeTypography: Typography = Typography(
    displayLarge   = ClaudeType.DisplayXl,
    displayMedium  = ClaudeType.DisplayLg,
    displaySmall   = ClaudeType.DisplayMd,
    headlineLarge  = ClaudeType.DisplayMd,
    headlineMedium = ClaudeType.DisplaySm,
    headlineSmall  = ClaudeType.TitleLg,
    titleLarge     = ClaudeType.TitleLg,
    titleMedium    = ClaudeType.TitleMd,
    titleSmall     = ClaudeType.TitleSm,
    bodyLarge      = ClaudeType.BodyMd,
    bodyMedium     = ClaudeType.BodySm,
    bodySmall      = ClaudeType.Caption,
    labelLarge     = ClaudeType.Button,
    labelMedium    = ClaudeType.Caption,
    labelSmall     = ClaudeType.CaptionUppercase
)
