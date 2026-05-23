package com.studybuddy.v2.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 话廊 / 信件用的衬线字体家族。
 *
 * 当前实现：FontFamily.Serif（Android 兜底，中文 → 思源宋体 / 西文 → Noto Serif）+ italic
 * 配合精细字重 + letterSpacing 调得尽量接近 EB Garamond 体感。
 *
 * **升级路径**：把 EB Garamond ttf 放到 `res/font/`，改 [QuoteSerifFamily] 指向那个 FontFamily 即可。
 * 升级建议字体：
 *   - EB Garamond 12 Italic (推荐，最经典的"诗集"感)
 *   - Cormorant Garamond Italic (更现代)
 *   - Crimson Pro Italic (中性可读)
 *
 * 中文字体一并打包思源宋体 italic / Noto Serif CJK SC Italic，避免中文走到默认 fallback。
 */
val QuoteSerifFamily: FontFamily = FontFamily.Serif

/** 话廊列表 — 大字唯美 */
val QuoteSerifLarge = TextStyle(
    fontFamily = QuoteSerifFamily,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
    fontSize = 26.sp,
    lineHeight = 40.sp,
    letterSpacing = 0.2.sp
)

/** 话廊列表 — 来源 / 副标 */
val QuoteSerifSmall = TextStyle(
    fontFamily = QuoteSerifFamily,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.4.sp
)

/** 编辑器 — 主体输入 */
val QuoteSerifEdit = TextStyle(
    fontFamily = QuoteSerifFamily,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
    fontSize = 20.sp,
    lineHeight = 32.sp,
    letterSpacing = 0.3.sp
)

/** 编辑器 — placeholder（同 Edit，独立出来未来可单独调） */
val QuoteSerifPlaceholder = QuoteSerifEdit

/** 编辑器 — 来源行 */
val QuoteSerifEditSource = TextStyle(
    fontFamily = QuoteSerifFamily,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.3.sp
)

/** 信件正文（用同一 family，比话廊小一档） */
val LetterSerifBody = TextStyle(
    fontFamily = QuoteSerifFamily,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 30.sp,
    letterSpacing = 0.25.sp
)

/** 信件时段标记（"午后" / "傍晚" 这种） */
val LetterSerifMarker = TextStyle(
    fontFamily = QuoteSerifFamily,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 22.sp,
    letterSpacing = 1.5.sp  // 时段标 字间稍宽，做"小标题"感
)
