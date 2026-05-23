package com.studybuddy.v2.ui.letter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 纸飞机寄出动画 —— 纯 Canvas drawPath。
 *
 * 时间线：
 * 0   - 360ms  矩形纸张折叠成飞机（4 个折角各自 rotate + translate，120ms × 3 步）
 * 360 - 440ms  折好停 80ms（"折好了"的微停）
 * 440 - 840ms  沿 1/4 圆弧飞向右上角（400ms easeOutCubic）+ 缩到 0.3 + alpha → 0
 * 总时长：840ms
 *
 * 完成后调 [onComplete]，外部清除动画状态。
 */
@Composable
fun PlaneFlyAnimation(
    color: Color,
    onComplete: () -> Unit
) {
    val foldProgress = remember { Animatable(0f) }   // 0 = 矩形 / 1 = 折好的飞机
    val flyProgress = remember { Animatable(0f) }    // 0 = 中央 / 1 = 飞到右上角
    val alphaAnim = remember { Animatable(1f) }
    val scaleAnim = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Stage 1：折叠 360ms
        foldProgress.animateTo(1f, tween(360, easing = EaseOutCubic))
        // Stage 2：停 80ms
        kotlinx.coroutines.delay(80)
        // Stage 3：飞出 400ms（同时缩 + alpha 渐隐）
        kotlinx.coroutines.coroutineScope {
            launch { flyProgress.animateTo(1f, tween(400, easing = EaseOutCubic)) }
            launch { scaleAnim.animateTo(0.3f, tween(400, easing = EaseOutCubic)) }
            launch { alphaAnim.animateTo(0f, tween(400, easing = EaseOutCubic)) }
        }
        onComplete()
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        // 起点：屏幕中央偏下（输入框附近）
        val startX = size.width / 2f
        val startY = size.height * 0.7f
        // 终点：右上角外
        val endX = size.width * 1.1f
        val endY = -size.height * 0.05f

        // 沿 1/4 圆弧（控制点在右上）
        val ctrlX = size.width * 0.95f
        val ctrlY = size.height * 0.5f
        val t = flyProgress.value
        val cx = (1 - t) * (1 - t) * startX + 2 * (1 - t) * t * ctrlX + t * t * endX
        val cy = (1 - t) * (1 - t) * startY + 2 * (1 - t) * t * ctrlY + t * t * endY

        val planeSize = 80.dp.toPx() * scaleAnim.value
        val fold = foldProgress.value

        translate(left = cx - planeSize / 2, top = cy - planeSize / 2) {
            // 飞行时绕轨迹方向旋转 ~ -25 度（右上飞）
            val rotZ = -25f * t
            rotate(rotZ, pivot = Offset(planeSize / 2, planeSize / 2)) {
                drawPaperFold(planeSize, fold, color, alphaAnim.value)
            }
        }
    }
}

/**
 * 绘制一张正在折叠的纸 → 飞机。
 *
 * fold = 0：完整矩形（白纸）
 * fold = 1：折好的纸飞机三角形
 * 中间过渡：左右两边向中线对折 + 顶点尖起
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPaperFold(
    sizeDp: Float,
    fold: Float,
    color: Color,
    alpha: Float
) {
    val s = sizeDp
    val c = color.copy(alpha = alpha * 0.9f)
    val edge = color.copy(alpha = alpha * 0.6f)

    // 矩形 → 飞机：左半和右半各自向中线旋转折叠
    // fold=0 时画完整 80×60 矩形；fold=1 时画对称三角形
    val width = s * (1f - fold * 0.4f)            // 折叠时整体收窄
    val left = (s - width) / 2f
    val right = left + width

    // 上边沿：fold=0 是直线，fold=1 是 ↑ 尖
    val topMidY = -s * 0.05f * fold               // 中间顶点轻微上凸
    val topSideY = s * 0.1f * fold                // 两侧顶点下降形成机翼

    // 下边沿：fold=0 直线，fold=1 微尖
    val botMidY = s * 0.5f + s * 0.15f * fold     // 中间下尖
    val botSideY = s * 0.5f - s * 0.05f * fold

    val path = Path().apply {
        moveTo(left, topSideY)
        lineTo(s / 2f, topMidY)
        lineTo(right, topSideY)
        lineTo(right, botSideY)
        lineTo(s / 2f, botMidY)
        lineTo(left, botSideY)
        close()
    }
    drawPath(path = path, color = c)

    // 中线折痕（只在折叠开始后出现）
    if (fold > 0.2f) {
        drawLine(
            color = edge,
            start = Offset(s / 2f, topMidY),
            end = Offset(s / 2f, botMidY),
            strokeWidth = 1.dp.toPx()
        )
    }
}
