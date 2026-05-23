package com.studybuddy.v2.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.pet.saddle.BodyType
import com.studybuddy.v2.ui.pet.saddle.SaddleCatSprite
import com.studybuddy.v2.ui.pet.saddle.SaddleCatState
import kotlinx.coroutines.launch

/**
 * 开屏鞍部猫入场动画。
 * 1. 0-300ms：屏幕中央展开一个圆形"异次元传送门"（coral 圈扩散）
 * 2. 300-1000ms：鞍部猫从传送门中心被推出来（scale 0→1.2→1.0）
 * 3. 1000-1600ms：传送门收缩，鞍部猫朝右下角飞出，scale 渐缩，alpha 渐隐
 * 4. 完成后调用 onFinish
 *
 * 仅当鞍部猫已解锁时才播；未解锁直接 onFinish() 跳过。
 */
@Composable
fun SaddleSplashOverlay(
    visible: Boolean,
    onFinish: () -> Unit
) {
    if (!visible) return

    val colors = MaterialTheme.appColors
    val cfg = LocalConfiguration.current
    val density = LocalDensity.current

    val portalRadius = remember { Animatable(0f) }
    val catScale = remember { Animatable(0f) }
    val catAlpha = remember { Animatable(1f) }
    val catX = remember { Animatable(0f) }
    val catY = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val portalAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // 中央段：门 + 猫同时出现
        kotlinx.coroutines.coroutineScope {
            launch { portalRadius.animateTo(140f, tween(450, easing = EaseOutCubic)) }
            launch { catScale.animateTo(1.0f, tween(450, easing = EaseOutCubic)) }
            launch { catAlpha.animateTo(1f, tween(300)) }
        }
        // 短暂停留
        kotlinx.coroutines.delay(450)
        // 中央段：门 + 猫同时消失
        kotlinx.coroutines.coroutineScope {
            launch { portalRadius.animateTo(0f, tween(400, easing = EaseOutCubic)) }
            launch { portalAlpha.animateTo(0f, tween(400)) }
            launch { catScale.animateTo(0.4f, tween(400, easing = EaseOutCubic)) }
            launch { catAlpha.animateTo(0f, tween(400)) }
        }
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas),
        contentAlignment = Alignment.Center
    ) {
        // 异次元传送门 —— coral 圈
        Canvas(modifier = Modifier.size(320.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = portalRadius.value * 1.5f
            val a = portalAlpha.value
            drawCircle(
                color = ClaudeColors.Primary.copy(alpha = 0.20f * a),
                radius = r,
                center = c
            )
            drawCircle(
                color = ClaudeColors.Primary.copy(alpha = 0.45f * a),
                radius = r * 0.7f,
                center = c
            )
            drawCircle(
                color = ClaudeColors.Primary.copy(alpha = 0.75f * a),
                radius = r * 0.4f,
                center = c
            )
        }
        // 鞍部猫从中心弹出
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = catScale.value
                    scaleY = catScale.value
                    translationX = catX.value
                    translationY = catY.value
                    rotationZ = rotation.value
                    alpha = catAlpha.value
                }
        ) {
            SaddleCatSprite(
                state = SaddleCatState(),
                pixelSize = 8.dp,
                color = colors.mascotInk,
                bodyType = BodyType.HEAD_ONLY
            )
        }
    }
}
