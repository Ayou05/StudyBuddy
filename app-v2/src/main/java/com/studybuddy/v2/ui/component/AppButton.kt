package com.studybuddy.v2.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors

// ═════════════════════════════════════════════════════════════════════════════
// AppButton — Claude 设计语言四态
//   AppButton.Primary   填充 coral，CTA 主色
//   AppButton.Secondary canvas 底 + hairline 描边，次要操作
//   AppButton.OnDark    coral 在 dark 表面上的反色（dark 卡内主按钮）
//   AppButton.Text      纯文本 link，coral 色，inline
//   AppButton.IconCircle 圆形图标按钮，36dp（小）/56dp（大）
//
// 所有变体：按压时 0.96 spring 回弹（damping = 0.55, stiffness MediumLow）
// ═════════════════════════════════════════════════════════════════════════════

object AppButton {
    @Composable
    fun Primary(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        leadingIcon: String? = null,
        fullWidth: Boolean = false
    ) = SolidButton(
        text = text,
        onClick = onClick,
        bg = if (enabled) ClaudeColors.Primary else ClaudeColors.PrimaryDisabled,
        fg = if (enabled) ClaudeColors.OnPrimary else ClaudeColors.Muted,
        modifier = modifier,
        leadingIcon = leadingIcon,
        fullWidth = fullWidth,
        enabled = enabled,
        pressedBg = ClaudeColors.PrimaryActive
    )

    @Composable
    fun Secondary(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        leadingIcon: String? = null,
        fullWidth: Boolean = false
    ) {
        val colors = MaterialTheme.appColors
        OutlineButton(
            text = text,
            onClick = onClick,
            bg = colors.canvas,
            fg = colors.ink,
            border = BorderStroke(1.dp, colors.hairline),
            modifier = modifier,
            leadingIcon = leadingIcon,
            fullWidth = fullWidth
        )
    }

    @Composable
    fun OnDark(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        leadingIcon: String? = null,
        fullWidth: Boolean = false
    ) {
        val colors = MaterialTheme.appColors
        SolidButton(
            text = text,
            onClick = onClick,
            bg = colors.surfaceDarkElevated,
            fg = colors.onDark,
            modifier = modifier,
            leadingIcon = leadingIcon,
            fullWidth = fullWidth,
            enabled = true,
            pressedBg = colors.surfaceDarkSoft
        )
    }

    @Composable
    fun Text(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        color: Color? = null
    ) {
        val effective = color ?: ClaudeColors.Primary
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.96f else 1f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
            label = "txt_press"
        )
        Box(
            modifier = modifier
                .scale(scale)
                .clip(RoundedCornerShape(ClaudeRadius.sm))
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(horizontal = ClaudeSpacing.xs, vertical = ClaudeSpacing.xxs)
        ) {
            Text(text = text, style = ClaudeType.Button, color = effective)
        }
    }

    @Composable
    fun IconCircle(
        iconName: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        size: Dp = 44.dp,
        bg: Color? = null,
        fg: Color? = null
    ) {
        val colors = MaterialTheme.appColors
        val effectiveBg = bg ?: colors.canvas
        val effectiveFg = fg ?: colors.ink
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.94f else 1f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
            label = "ic_press"
        )
        Box(
            modifier = modifier
                .size(size)
                .scale(scale)
                .clip(CircleShape)
                .background(effectiveBg)
                .border(1.dp, colors.hairline, CircleShape)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(name = iconName, size = size * 0.5f, tint = effectiveFg)
        }
    }
}

// ─── internal building blocks ───────────────────────────────────────────────

@Composable
private fun SolidButton(
    text: String,
    onClick: () -> Unit,
    bg: Color,
    fg: Color,
    pressedBg: Color,
    modifier: Modifier,
    leadingIcon: String?,
    fullWidth: Boolean,
    enabled: Boolean
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "press"
    )
    val effectiveBg = if (pressed && enabled) pressedBg else bg
    Box(
        modifier = modifier
            .let { if (fullWidth) it.fillMaxWidth() else it }
            .defaultMinSize(minHeight = 44.dp)
            .height(44.dp)
            .scale(scale)
            .clip(RoundedCornerShape(ClaudeRadius.md))
            .background(effectiveBg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = ClaudeSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                AppIcon(name = leadingIcon, size = 16.dp, tint = fg)
                Spacer(Modifier.width(ClaudeSpacing.xs))
            }
            Text(text = text, style = ClaudeType.Button, color = fg)
        }
    }
}

@Composable
private fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    bg: Color,
    fg: Color,
    border: BorderStroke,
    modifier: Modifier,
    leadingIcon: String?,
    fullWidth: Boolean
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "press"
    )
    Box(
        modifier = modifier
            .let { if (fullWidth) it.fillMaxWidth() else it }
            .defaultMinSize(minHeight = 44.dp)
            .height(44.dp)
            .scale(scale)
            .clip(RoundedCornerShape(ClaudeRadius.md))
            .background(bg)
            .border(border, RoundedCornerShape(ClaudeRadius.md))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = ClaudeSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                AppIcon(name = leadingIcon, size = 16.dp, tint = fg)
                Spacer(Modifier.width(ClaudeSpacing.xs))
            }
            Text(text = text, style = ClaudeType.Button, color = fg)
        }
    }
}
