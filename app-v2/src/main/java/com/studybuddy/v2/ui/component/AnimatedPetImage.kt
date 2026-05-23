package com.studybuddy.v2.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.data.model.GrowthStage
import com.studybuddy.v2.data.model.PetBreed
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * 整图宠物精灵 + Compose 整体动效。
 *
 * 替代 [LayeredPetSprite] —— 分层方案对原画切割精度要求过高，AI 切片会导致重影 / 缺片。
 * 这里改成对 [AppPetImage] 整张图做 graphicsLayer 动画：
 * - 呼吸：scaleY 0.99↔1.01，4s 一周期
 * - 头微歪：rotationZ ±0.8°，6s 一周期（整体歪一点点，不夸张）
 * - 偶发小跳：translationY 每 8-12s 弹一次，跳起 -3dp 再回弹，250ms
 * - 喂食：translationY 点头 3 次，180ms × 6（外部触发）
 *
 * 视觉上"它在那儿活着"够了，绝不会重影。
 */
@Composable
fun AnimatedPetImage(
    breed: PetBreed,
    growthStage: GrowthStage,
    feedingTrigger: Long = 0L,
    sleeping: Boolean = false,
    size: Dp = 200.dp,
    dirty: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "petAnim")

    // 呼吸（睡觉时呼吸幅度更明显一点）
    val breathRange = if (sleeping) 0.985f to 1.015f else 0.992f to 1.008f
    val breathScale by infinite.animateFloat(
        initialValue = breathRange.first,
        targetValue = breathRange.second,
        animationSpec = infiniteRepeatable(
            animation = tween(if (sleeping) 5000 else 4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // 头微歪 —— ±0.8°，比之前的 ±2° 收敛得多
    val tilt by infinite.animateFloat(
        initialValue = -0.8f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt"
    )

    // 偶发小跳
    val hop = remember { Animatable(0f) }
    LaunchedEffect(sleeping) {
        if (sleeping) return@LaunchedEffect
        while (true) {
            delay(8000L + Random.nextLong(4000L))
            hop.animateTo(-3f, tween(120, easing = EaseInOutSine))
            hop.animateTo(0f, tween(180, easing = EaseInOutSine))
        }
    }

    // 喂食点头 3 次
    val feedY = remember { Animatable(0f) }
    LaunchedEffect(feedingTrigger) {
        if (feedingTrigger > 0L) {
            repeat(3) {
                feedY.animateTo(6f, tween(180))
                feedY.animateTo(0f, tween(180))
            }
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        AppPetImage(
            breed = breed,
            growthStage = growthStage,
            size = size,
            dirty = dirty,
            modifier = Modifier.graphicsLayer {
                scaleX = breathScale
                scaleY = breathScale
                rotationZ = tilt
                translationY = hop.value + feedY.value
            }
        )
    }
}
