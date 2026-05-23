package com.studybuddy.v2.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studybuddy.v2.theme.appColors

/**
 * 鞍部猫 popover —— 单击鞍部猫时弹出的小气泡。
 *
 * - 锚点在 [anchor]（屏幕坐标），气泡浮在锚点上方 28dp
 * - 等宽小字，dark-mockup 黑底
 * - 200ms 淡入，外部控制 2 秒后自动隐藏
 * - 边界保护：靠近屏边时自动调整避免出屏
 */
@Composable
fun MascotPopover(
    text: String,
    anchor: Offset,
    bounds: Rect?,
    density: androidx.compose.ui.unit.Density
) {
    val colors = MaterialTheme.appColors
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(text, anchor) {
        alpha.snapTo(0f)
        alpha.animateTo(1f, tween(200))
    }

    var bubbleW by remember { mutableStateOf(0) }
    var bubbleH by remember { mutableStateOf(0) }

    val gapDp = 28.dp
    val gapPx = with(density) { gapDp.toPx() }

    var topLeftX = anchor.x - bubbleW / 2f
    var topLeftY = anchor.y - bubbleH - gapPx
    if (bounds != null) {
        if (topLeftX < bounds.left + 16f) topLeftX = bounds.left + 16f
        if (topLeftX + bubbleW > bounds.right - 16f) topLeftX = bounds.right - 16f - bubbleW
        if (topLeftY < bounds.top + 16f) topLeftY = anchor.y + gapPx
    }

    Box(
        modifier = Modifier
            .offset(
                x = with(density) { topLeftX.toDp() },
                y = with(density) { topLeftY.toDp() }
            )
            .graphicsLayer { this.alpha = alpha.value }
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceDark)
            .border(1.dp, colors.hairlineSoft, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .onGloballyPositioned {
                bubbleW = it.size.width
                bubbleH = it.size.height
            }
    ) {
        Text(
            text = text,
            color = colors.onDark,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}
