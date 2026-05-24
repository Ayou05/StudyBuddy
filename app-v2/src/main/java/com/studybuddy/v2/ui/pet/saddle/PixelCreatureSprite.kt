package com.studybuddy.v2.ui.pet.saddle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutBack
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.data.model.GrowthStage
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * 通用像素生物渲染器 —— 鞍部猫和它的朋友们 v2。
 *
 * 双层渲染：
 * 1. 主色矩阵（breed 默认色）
 * 2. 次色 pattern 矩阵（暹罗面罩 / 奶牛斑 / 兔肚白 / 狐胸前白 / 蛋斑纹）
 *
 * Q 弹动效（graphicsLayer，不动矩阵）：
 * - 呼吸 scaleY（加强版 0.98 ↔ 1.02）
 * - 微歪 rotationZ
 * - 偶发跳起（Squash & Stretch：起跳压扁 → 空中拉长 → 落地压扁回弹）
 * - 喂食弹性（纵向拉伸 + 横向收缩，EaseInOutElastic）
 * - 开心摇摆（mood > 80 时，rotationZ ±12° + scaleX 挤压）
 * - 眨眼（TODO：局部矩阵 + 整体微缩）
 */
@Composable
fun PixelCreatureSprite(
    breed: SaddleFriendsFrames.Breed,
    growthStage: GrowthStage,
    pose: Pose = Pose.IDLE,
    feedingTrigger: Long = 0L,
    strokeTrigger: Long = 0L,    // 抚摸 → 头部小颤
    cleanTrigger: Long = 0L,      // 清洁 → 抖动 + 闪亮
    sleeping: Boolean = false,
    mood: Float = 50f,
    pixelSize: Dp = 8.dp,
    color: Color? = null,
    modifier: Modifier = Modifier
) {
    val spec = remember(breed, growthStage, pose) {
        SaddleFriendsFrames.specFor(breed, growthStage, pose)
    }

    val mainColor = color ?: parseHex(SaddleFriendsFrames.colorHexFor(breed))
    val patternColor = spec.patternHex?.let { parseHex(it) }

    val infinite = rememberInfiniteTransition(label = "creatureAnim")

    // 1. 加强呼吸（0.98 ↔ 1.02，比原来的 0.995 ↔ 1.005 更明显）
    val breathScale by infinite.animateFloat(
        initialValue = if (sleeping) 0.97f else 0.98f,
        targetValue = if (sleeping) 1.03f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (sleeping) 5000 else 4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    val tilt by infinite.animateFloat(
        initialValue = -0.6f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt"
    )

    // 2. 跳起落地 Squash & Stretch（Q 弹版）
    val hopY = remember { Animatable(0f) }
    val hopSquashY = remember { Animatable(1f) }  // scaleY 挤压
    val hopSquashX = remember { Animatable(1f) }  // scaleX 拉伸
    LaunchedEffect(sleeping) {
        if (sleeping) return@LaunchedEffect
        while (true) {
            delay(8000L + Random.nextLong(4000L))
            // 起跳前：压扁蓄力
            hopSquashY.animateTo(0.7f, tween(100, easing = FastOutSlowInEasing))
            hopSquashX.animateTo(1.15f, tween(100, easing = FastOutSlowInEasing))
            // 起跳：空中拉长
            hopY.animateTo(-8f, tween(200, easing = FastOutSlowInEasing))
            hopSquashY.animateTo(1.2f, tween(200, easing = FastOutSlowInEasing))
            hopSquashX.animateTo(0.85f, tween(200, easing = FastOutSlowInEasing))
            // 落地：压扁缓冲 + 回弹
            hopY.animateTo(0f, tween(150, easing = FastOutSlowInEasing))
            hopSquashY.animateTo(0.8f, tween(80))
            hopSquashX.animateTo(1.2f, tween(80))
            // 回弹到正常
            hopSquashY.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
            hopSquashX.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        }
    }

    // 3. 喂食弹性（纵向拉伸 + 横向收缩，模拟 Q 弹张嘴）
    val feedSquashY = remember { Animatable(1f) }
    val feedSquashX = remember { Animatable(1f) }
    LaunchedEffect(feedingTrigger) {
        if (feedingTrigger > 0L) {
            repeat(3) {
                // 张嘴：纵向拉伸 + 横向收缩
                feedSquashY.animateTo(1.15f, tween(150, easing = FastOutSlowInEasing))
                feedSquashX.animateTo(0.95f, tween(150, easing = FastOutSlowInEasing))
                // 闭嘴：回弹
                feedSquashY.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
                feedSquashX.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
                delay(100)
            }
        }
    }

    // 4. 开心摇摆（mood > 80 时触发）
    val happyRotation = remember { Animatable(0f) }
    val happySquashX = remember { Animatable(1f) }
    LaunchedEffect(mood, sleeping) {
        if (sleeping || mood <= 80f) return@LaunchedEffect
        while (true) {
            delay(20000L + Random.nextLong(10000L))
            // 左摇 + 挤压
            happyRotation.animateTo(-12f, tween(400, easing = EaseInOutBack))
            happySquashX.animateTo(0.95f, tween(400, easing = EaseInOutBack))
            // 右摇 + 挤压
            happyRotation.animateTo(12f, tween(600, easing = EaseInOutBack))
            happySquashX.animateTo(1.05f, tween(600, easing = EaseInOutBack))
            // 回中
            happyRotation.animateTo(0f, tween(400, easing = EaseInOutBack))
            happySquashX.animateTo(1f, tween(400, easing = EaseInOutBack))
        }
    }

    // 5. 眨眼（每 4-8s 偶发；用 scaleY 短暂压扁模拟闭眼，整体微下沉）
    val blinkScaleY = remember { Animatable(1f) }
    LaunchedEffect(sleeping) {
        if (sleeping) return@LaunchedEffect
        while (true) {
            delay(4000L + Random.nextLong(4000L))
            blinkScaleY.animateTo(0.92f, tween(80))
            blinkScaleY.animateTo(1f, tween(120))
            // 偶尔双连眨
            if (Random.nextInt(4) == 0) {
                delay(140L)
                blinkScaleY.animateTo(0.92f, tween(80))
                blinkScaleY.animateTo(1f, tween(120))
            }
        }
    }

    // 6. 抚摸 → 头部小颤（rotationZ 短脉冲 ±3°，3 次）
    val strokeRotation = remember { Animatable(0f) }
    LaunchedEffect(strokeTrigger) {
        if (strokeTrigger > 0L) {
            repeat(3) {
                strokeRotation.animateTo(-3f, tween(80))
                strokeRotation.animateTo(3f, tween(120))
            }
            strokeRotation.animateTo(0f, tween(100))
        }
    }

    // 7. 清洁 → 抖动（translationX 左右快速振荡 + alpha 轻闪）
    val cleanShakeX = remember { Animatable(0f) }
    val cleanAlpha = remember { Animatable(1f) }
    LaunchedEffect(cleanTrigger) {
        if (cleanTrigger > 0L) {
            repeat(5) {
                cleanShakeX.animateTo(-3f, tween(50))
                cleanShakeX.animateTo(3f, tween(70))
            }
            cleanShakeX.animateTo(0f, tween(50))
            cleanAlpha.animateTo(0.7f, tween(80))
            cleanAlpha.animateTo(1f, tween(200))
        }
    }

    val totalW = pixelSize * SaddleFriendsFrames.COLS
    val totalH = pixelSize * SaddleFriendsFrames.ROWS

    Box(
        modifier = modifier.size(width = totalW, height = totalH)
    ) {
        Box(
            modifier = Modifier
                .size(width = totalW, height = totalH)
                .graphicsLayer {
                    // 组合所有动画效果
                    scaleX = breathScale * hopSquashX.value * feedSquashX.value * happySquashX.value
                    scaleY = breathScale * hopSquashY.value * feedSquashY.value * blinkScaleY.value
                    rotationZ = tilt + happyRotation.value + strokeRotation.value
                    translationY = hopY.value
                    translationX = cleanShakeX.value
                    alpha = cleanAlpha.value
                }
        ) {
            // 主色（底层）
            PixelMatrix(
                matrix = spec.main,
                rows = SaddleFriendsFrames.ROWS,
                cols = SaddleFriendsFrames.COLS,
                pixelSize = pixelSize,
                color = mainColor
            )
            // 次色 pattern（叠在主色上，透明位置不画）
            if (spec.pattern != null && patternColor != null) {
                PixelMatrix(
                    matrix = spec.pattern,
                    rows = SaddleFriendsFrames.ROWS,
                    cols = SaddleFriendsFrames.COLS,
                    pixelSize = pixelSize,
                    color = patternColor
                )
            }
        }
    }
}

private fun parseHex(hex: String): Color = try {
    val cleaned = hex.removePrefix("#")
    val argb = cleaned.toLong(16) or 0xFF000000L
    Color(argb.toInt())
} catch (_: Exception) {
    Color(0xFFCC785C.toInt())
}
