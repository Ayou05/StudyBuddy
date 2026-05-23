package com.studybuddy.v2.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * 分层骨骼宠物精灵 —— 1 张原画切成 7 层，代码驱动每层独立动画。
 *
 * # 资产约定
 * 资产位于 `app-v2/src/main/assets/pets/{breed}_{stage}_{emote}/{layer}.png`
 * 7 个 layer：body / head / ear_left / ear_right / tail / eye_left / eye_right
 * egg 阶段不切片，直接用 `pets/{breed}_egg.png` 单图
 *
 * # 渲染层级（从下到上）
 * tail → body → ear_left → ear_right → head → eye_left → eye_right
 *
 * # 动画驱动
 * - 呼吸：body scaleY 0.99–1.01，4s 一周期
 * - 头微歪：head rotationZ ±2°，6s 一周期（缓慢）
 * - 耳朵抖：ear_left / ear_right 每隔 8-15s 随机一只 5° 快速抽动 80ms
 * - 摇尾：tail rotationZ ±15° sin 波动，3s 一周期
 * - 眨眼：eye scaleY 1→0→1 随机 4-7s 一次，70ms
 * - 喂食：head 整体 translationY 上下点头 3 次，每次 200ms（外部触发）
 *
 * # 表情切换
 * 通过 emote 参数切到不同资产文件夹（happy/idle/sad/sleeping），整套图层一起换
 * sleeping 时眨眼频率改成"持续闭合"
 *
 * # 占位行为
 * 资产文件不存在时 AsyncImage 优雅降级（透明），不会崩
 */
@Composable
fun LayeredPetSprite(
    breed: String,                    // "orange" / "siamese" / "saddle_cat"
    stage: String,                    // "egg" / "baby" / "young" / "adult" / "ultimate"
    emote: String,                    // "idle" / "happy" / "sad" / "sleeping"
    size: Dp = 200.dp,
    facingLeft: Boolean = false,
    feedingTrigger: Long = 0L,       // 外部 ++ 触发喂食动画
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val asset = "pets/${breed}_${stage}_${emote}"

    // egg 阶段单图直接渲染
    if (stage == "egg") {
        AsyncImage(
            model = ImageRequest.Builder(ctx)
                .data("file:///android_asset/pets/${breed}_egg.png")
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .size(size)
                .graphicsLayer { scaleX = if (facingLeft) -1f else 1f }
        )
        return
    }

    // 全身呼吸 —— infiniteTransition 驱动 body scaleY
    val infinite = rememberInfiniteTransition(label = "petBreath")
    val breathScale by infinite.animateFloat(
        initialValue = 0.99f,
        targetValue = 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    // 头微歪
    val headTilt by infinite.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "headTilt"
    )
    // 摇尾 —— sin 函数手动驱动以获得更自然的 ±15° 摆动
    val tailPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tailPhase"
    )
    val tailRot = sin(tailPhase) * 15f

    // 耳朵抖 —— 偶发，左右随机
    var earLeftTwitch by remember { mutableStateOf(0f) }
    var earRightTwitch by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((8_000L + Random.nextLong(7_000L)))
            if (Random.nextBoolean()) {
                earLeftTwitch = 5f; delay(80); earLeftTwitch = 0f
            } else {
                earRightTwitch = -5f; delay(80); earRightTwitch = 0f
            }
        }
    }

    // 眨眼 —— sleeping 时持续闭合
    var blinkScale by remember { mutableStateOf(1f) }
    LaunchedEffect(emote) {
        if (emote == "sleeping") {
            blinkScale = 0.05f
        } else {
            blinkScale = 1f
            while (true) {
                delay((4_000L + Random.nextLong(3_000L)))
                val anim = Animatable(1f)
                anim.animateTo(0.05f, tween(80))
                blinkScale = anim.value
                anim.animateTo(1f, tween(80))
                blinkScale = anim.value
            }
        }
    }

    // 喂食 —— translationY 点头
    val headFeedY = remember { Animatable(0f) }
    LaunchedEffect(feedingTrigger) {
        if (feedingTrigger > 0L) {
            repeat(3) {
                headFeedY.animateTo(8f, tween(180))
                headFeedY.animateTo(0f, tween(180))
            }
        }
    }

    val flipX = if (facingLeft) -1f else 1f

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = flipX },
        contentAlignment = Alignment.Center
    ) {
        // 1. tail（最底层）
        Layer(asset, "tail",
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = tailRot
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.6f, 0.7f)
                }
        )
        // 2. body —— 呼吸
        Layer(asset, "body",
            Modifier
                .fillMaxSize()
                .graphicsLayer { scaleY = breathScale }
        )
        // 3. ear_left
        Layer(asset, "ear_left",
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = earLeftTwitch
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.4f, 0.55f)
                }
        )
        // 4. ear_right
        Layer(asset, "ear_right",
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = earRightTwitch
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.6f, 0.55f)
                }
        )
        // 5. head —— 头微歪 + 喂食点头
        Layer(asset, "head",
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = headTilt
                    translationY = headFeedY.value
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.7f)
                }
        )
        // 6-7. 眼睛 —— 跟随头一起动 + 眨眼
        Layer(asset, "eye_left",
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = headTilt
                    translationY = headFeedY.value
                    scaleY = blinkScale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.7f)
                }
        )
        Layer(asset, "eye_right",
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = headTilt
                    translationY = headFeedY.value
                    scaleY = blinkScale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.7f)
                }
        )
    }
}

@Composable
private fun Layer(assetDir: String, name: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(ctx)
            .data("file:///android_asset/$assetDir/$name.png")
            .crossfade(false)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
