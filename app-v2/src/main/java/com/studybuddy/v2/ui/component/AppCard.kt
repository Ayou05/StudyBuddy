package com.studybuddy.v2.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.appColors

// ═════════════════════════════════════════════════════════════════════════════
// AppCard — Claude 表面三型
//
//   AppCard.Feature  cream-card 浅奶白卡（feature explanation 默认卡）
//   AppCard.Dark     dark-mockup 深棕黑卡（code-window / 倒计时大数字 / 余额）
//   AppCard.Coral    coral 满版 callout（关键 CTA / 庆祝瞬间）
//   AppCard.Outline  canvas + hairline 描边卡（pricing-tier 风）
//
// 都默认 12dp 圆角、内边距 32dp（卡内）；内容由调用方提供。
// 不带阴影 — Claude 设计哲学是 color-block first。
// ═════════════════════════════════════════════════════════════════════════════
object AppCard {
    @Composable
    fun Feature(
        modifier: Modifier = Modifier,
        padding: Dp = ClaudeSpacing.lg,
        radius: Dp = ClaudeRadius.lg,
        content: @Composable () -> Unit
    ) {
        val colors = MaterialTheme.appColors
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(radius))
                .background(colors.surfaceCard)
                .padding(padding)
        ) { content() }
    }

    @Composable
    fun Dark(
        modifier: Modifier = Modifier,
        padding: Dp = ClaudeSpacing.lg,
        radius: Dp = ClaudeRadius.lg,
        content: @Composable () -> Unit
    ) {
        val colors = MaterialTheme.appColors
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(radius))
                .background(colors.surfaceDark)
                .padding(padding)
        ) { content() }
    }

    @Composable
    fun Coral(
        modifier: Modifier = Modifier,
        padding: Dp = ClaudeSpacing.xl,
        radius: Dp = ClaudeRadius.lg,
        content: @Composable () -> Unit
    ) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(radius))
                .background(ClaudeColors.Primary)
                .padding(padding)
        ) { content() }
    }

    @Composable
    fun Outline(
        modifier: Modifier = Modifier,
        padding: Dp = ClaudeSpacing.lg,
        radius: Dp = ClaudeRadius.lg,
        content: @Composable () -> Unit
    ) {
        val colors = MaterialTheme.appColors
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(radius))
                .background(colors.canvas)
                .border(1.dp, colors.hairline, RoundedCornerShape(radius))
                .padding(padding)
        ) { content() }
    }
}
