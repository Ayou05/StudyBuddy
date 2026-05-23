package com.studybuddy.v2.ui.pet.saddle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * 鞍部猫精灵。
 *
 * 设计宗旨："它就那样，但它在。"
 *
 * 行为规则：
 * - **IDLE 不抖、不浮、不弹** —— 它**不动**才是它在的证明
 * - **唯一的"活"** = 每 4-6 秒眨一次眼（随机抖动避免节拍感），3 帧渐闭 ~70ms 后立即回 IDLE
 * - TYPING 在脸右侧外加 1×2 光标方块，220ms 闪一次
 * - WATCHING / SQUINT 时整体水平偏移 1px 朝向 [SaddleCatState.gaze]
 *
 * @param bodyType HEAD_ONLY 给"角落探出"场景；FULL_BODY 给终端展示位
 */
@Composable
fun SaddleCatSprite(
    state: SaddleCatState,
    pixelSize: Dp,
    color: Color = Color(0xFF181715),
    bodyType: BodyType = BodyType.FULL_BODY,
    modifier: Modifier = Modifier
) {
    val rows: Int
    val cols: Int
    val baseFrame: IntArray
    val blinkFrames: List<IntArray>

    when (bodyType) {
        BodyType.HEAD_ONLY -> {
            rows = SaddleCatFrames.HeadOnly.ROWS
            cols = SaddleCatFrames.HeadOnly.COLS
            baseFrame = SaddleCatFrames.HeadOnly.framesFor(state.pose).first()
            blinkFrames = emptyList() // 头版不做眨眼，避免角落里太花
        }
        BodyType.FULL_BODY -> {
            rows = SaddleCatFrames.FullBody.ROWS
            cols = SaddleCatFrames.FullBody.COLS
            baseFrame = SaddleCatFrames.FullBody.framesFor(state.pose).first()
            blinkFrames = SaddleCatFrames.FullBody.blinkFrames
        }
    }

    // 当前显示的矩阵 —— IDLE 状态下会被 blink 序列短暂覆盖
    var currentFrame by remember(state.pose, bodyType) { mutableStateOf(baseFrame) }

    // IDLE 眨眼循环
    LaunchedEffect(state.pose, bodyType) {
        if (state.pose == Pose.IDLE && blinkFrames.isNotEmpty()) {
            while (true) {
                // 随机 4-7s 等待
                delay(4000L + Random.nextLong(3000L))
                // 渐次闭眼 3 帧 × 60-80ms
                for (frame in blinkFrames) {
                    currentFrame = frame
                    delay(70L)
                }
                // 立即睁眼
                currentFrame = baseFrame
            }
        } else {
            currentFrame = baseFrame
        }
    }

    // TYPING 光标闪烁
    var cursorOn by remember { mutableIntStateOf(1) }
    LaunchedEffect(state.pose) {
        if (state.pose == Pose.TYPING) {
            while (true) {
                delay(220L)
                cursorOn = 1 - cursorOn
            }
        } else {
            cursorOn = 1
        }
    }

    val gazeDx = when (state.gaze) {
        GazeDirection.LEFT -> (-1).dp
        GazeDirection.RIGHT -> 1.dp
        else -> 0.dp
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.offset(x = gazeDx)) {
            // 描边色 —— 用极弱透明度做轮廓增强，主色已选过两模式可见
            val luma = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
            val outlineColor = if (luma < 0.5f)
                Color(0xFFFAF9F5).copy(alpha = 0.30f)
            else
                Color(0xFF181715).copy(alpha = 0.30f)
            Box(modifier = Modifier.offset(x = -1.dp, y = 0.dp)) {
                PixelMatrix(matrix = currentFrame, rows = rows, cols = cols, pixelSize = pixelSize, color = outlineColor)
            }
            Box(modifier = Modifier.offset(x = 1.dp, y = 0.dp)) {
                PixelMatrix(matrix = currentFrame, rows = rows, cols = cols, pixelSize = pixelSize, color = outlineColor)
            }
            Box(modifier = Modifier.offset(x = 0.dp, y = -1.dp)) {
                PixelMatrix(matrix = currentFrame, rows = rows, cols = cols, pixelSize = pixelSize, color = outlineColor)
            }
            Box(modifier = Modifier.offset(x = 0.dp, y = 1.dp)) {
                PixelMatrix(matrix = currentFrame, rows = rows, cols = cols, pixelSize = pixelSize, color = outlineColor)
            }
            // 主色 ink
            PixelMatrix(
                matrix = currentFrame,
                rows = rows,
                cols = cols,
                pixelSize = pixelSize,
                color = color
            )
        }
        if (state.pose == Pose.TYPING && cursorOn == 1) {
            Box(
                modifier = Modifier.offset(
                    x = pixelSize * (cols / 2 + 1),
                    y = -pixelSize / 2
                )
            ) {
                PixelMatrix(
                    matrix = intArrayOf(1, 1),
                    rows = 2,
                    cols = 1,
                    pixelSize = pixelSize,
                    color = color
                )
            }
        }
    }
}
