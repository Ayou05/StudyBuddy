package com.studybuddy.v2.ui.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.theme.ClaudeColors

/**
 * GlassSurface 的语义包装，避免每次手填 tint。
 */
object GlassPresets {

    /** 浅色玻璃 —— 用在 BottomBar / Sheet / Map 浮卡。底下要有 cream canvas 才显效 */
    @Composable
    fun Cream(
        modifier: Modifier = Modifier,
        shape: Shape = RoundedCornerShape(12.dp),
        hairline: Boolean = true,
        highlight: Boolean = true,
        content: @Composable () -> Unit
    ) = GlassSurface(
        modifier = modifier,
        tint = ClaudeColors.SurfaceCard,
        shape = shape,
        hairline = hairline,
        highlight = highlight,
        content = content
    )

    /** 暗色玻璃 —— 用在 Snackbar / 暗底浮层 */
    @Composable
    fun Dark(
        modifier: Modifier = Modifier,
        shape: Shape = RoundedCornerShape(12.dp),
        hairline: Boolean = true,
        highlight: Boolean = true,
        content: @Composable () -> Unit
    ) = GlassSurface(
        modifier = modifier,
        tint = ClaudeColors.SurfaceDarkElevated,
        shape = shape,
        hairline = hairline,
        highlight = highlight,
        content = content
    )
}
