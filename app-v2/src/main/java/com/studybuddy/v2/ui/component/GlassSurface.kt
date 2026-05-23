package com.studybuddy.v2.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.theme.ClaudeColors

/**
 * 仿 iOS 26 Liquid Glass 风格的玻璃面层。
 *
 * # 实现策略
 * Android 的 `Modifier.blur` 是 self-blur（把自身内容模糊），跟 iOS 那种"backdrop blur"
 * 是两种不同 API。真 backdrop blur 需要 API 33+ 的 createBackdropEffect 配合 RenderNode，
 * 跨设备不稳定，BottomBar 这种**滚动内容上层** 还会触发每帧重渲染掉帧。
 *
 * 所以这里**故意不用 blur**，靠多层视觉叠加近似 Liquid Glass：
 * 1. 半透 tint（alpha 0.78）—— 玻璃本体颜色
 * 2. **顶部 specular 高光** 1.5px 白色 0.32 alpha 渐变 —— 玻璃面反光
 * 3. **底部 specular 极淡反光** 1px 0.12 alpha —— 玻璃下沿
 * 4. **内部上半区微弱光晕** 0.06 alpha —— 玻璃透光感
 * 5. 1dp hairline 0.45 alpha —— 玻璃厚度
 *
 * 比之前单层 highlight 视觉立体一档，不掉帧，所有 API 一致。
 * 文字 / 图标在这层之上保持完全清晰可读 —— 这是 BottomBar 的关键。
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    shape: Shape = RoundedCornerShape(12.dp),
    hairline: Boolean = true,
    highlight: Boolean = true,
    content: @Composable () -> Unit
) {
    val effectiveTint = tint.copy(alpha = 0.78f)

    val base = modifier
        .clip(shape)
        .background(effectiveTint)

    val withSpecular = if (highlight) {
        base.drawWithContent {
            drawContent()
            // 顶部高光：1.5px 渐变（玻璃面反光）
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.32f),
                    1f to Color.Transparent
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, 1.5.dp.toPx())
            )
            // 内部上半区光晕：玻璃透光感
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.06f),
                    0.5f to Color.Transparent,
                    1f to Color.Transparent
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, size.height)
            )
            // 底部 specular：1px 极弱反光（玻璃下沿）
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to Color.White.copy(alpha = 0.12f)
                ),
                topLeft = Offset(0f, size.height - 1.dp.toPx()),
                size = Size(size.width, 1.dp.toPx())
            )
        }
    } else base

    val withHairline = if (hairline) {
        withSpecular.border(
            width = 1.dp,
            color = ClaudeColors.Hairline.copy(alpha = 0.45f),
            shape = shape
        )
    } else withSpecular

    Box(modifier = withHairline) { content() }
}
