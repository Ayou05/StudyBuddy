package com.studybuddy.v2.ui.focus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.pet.saddle.BodyType
import com.studybuddy.v2.ui.pet.saddle.Pose
import com.studybuddy.v2.ui.pet.saddle.SaddleCatSprite
import com.studybuddy.v2.ui.pet.saddle.SaddleCatState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 鞍部猫"以时钟为家"陪伴动画。
 *
 * 设计立场：
 * - 时钟卡是猫的家，不需要额外"桌面"
 * - 猫永远在时钟卡的边缘活动（顶 / 冒号上 / 底 / 侧），不挡数字
 * - 30-45s 一个小动作，备考枯燥时给视觉惊喜，但不打断专注
 * - 不发声、不弹气泡、不响应用户操作（你专注它该干嘛干嘛）
 *
 * 必须由父布局把时钟卡的尺寸 [clockBox] 传进来，猫才知道边界。
 */
@Composable
fun ClockSideMascot(
    clockBox: IntSize,
    paused: Boolean = false
) {
    if (clockBox.width == 0 || clockBox.height == 0) return

    val color = androidx.compose.material3.MaterialTheme.appColors.mascotInk

    // 当前小动作 + 朝向
    var action by remember { mutableStateOf(MascotAction.SIT_TOP_LEFT) }
    var pose by remember { mutableStateOf(Pose.IDLE) }

    // 位置 + 旋转 + 透明度
    val xRel = remember { Animatable(0.15f) }   // 0..1（相对时钟卡宽度）
    val yRel = remember { Animatable(-0.15f) }  // 相对时钟卡高度（负 = 在卡上方）
    val rotation = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    // 切换动作脚本
    LaunchedEffect(paused) {
        if (paused) return@LaunchedEffect
        // 入场
        xRel.snapTo(0.15f); yRel.snapTo(-0.10f)
        while (true) {
            val next = MascotAction.values().random()
            action = next
            pose = next.defaultPose
            // 移到目标位置
            val (tx, ty) = next.targetRelativePos
            val (tr) = next.rotationDeg
            kotlinx.coroutines.coroutineScope {
                launch { xRel.animateTo(tx, tween(2200, easing = EaseInOutCubic)) }
                launch { yRel.animateTo(ty, tween(2200, easing = EaseInOutCubic)) }
                launch { rotation.animateTo(tr, tween(900, easing = EaseOutCubic)) }
            }
            // 停留时长（依动作）
            delay(next.holdMs.toLong())
        }
    }

    val pixelDp = 4.dp
    val spriteW = pixelDp * 7  // HEAD_ONLY COLS=10, 实际 cat head 宽 7 个 visible 列
    val spriteH = pixelDp * 7  // 简化为 7

    val cardWPx = clockBox.width.toFloat()
    val cardHPx = clockBox.height.toFloat()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.dp)  // 透明定位层，不占空间
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .offset(
                    x = with(androidx.compose.ui.platform.LocalDensity.current) {
                        (xRel.value * cardWPx).toDp()
                    },
                    y = with(androidx.compose.ui.platform.LocalDensity.current) {
                        (yRel.value * cardHPx).toDp()
                    }
                )
                .graphicsLayer {
                    rotationZ = rotation.value
                    this.alpha = alpha.value
                }
        ) {
            SaddleCatSprite(
                state = SaddleCatState(pose = pose),
                pixelSize = pixelDp,
                color = color,
                bodyType = BodyType.HEAD_ONLY
            )
        }
    }
}

/**
 * 猫的小动作脚本。
 *
 * targetRelativePos：(x, y) 相对时钟卡的比例位置
 *  - x = 0 在卡左边缘，1 在卡右边缘
 *  - y = -0.15 猫露上半身在卡顶，0 跨过卡上沿，1 在卡底，1.15 露上半身在卡底
 *
 * rotationDeg：sprite 旋转角度（趴着 = 0；侧躺 = 90；倒挂 = 180）
 *
 * holdMs：到达后停留多久再切下一个动作（含本次移动）
 */
private enum class MascotAction(
    val targetRelativePos: Pair<Float, Float>,
    val rotationDeg: List<Float>,
    val defaultPose: Pose,
    val holdMs: Int
) {
    /** 趴在时钟卡左上角（露半身） */
    SIT_TOP_LEFT(0.10f to -0.15f, listOf(0f), Pose.IDLE, 35_000),
    /** 趴在冒号上方 */
    SIT_ON_COLON(0.50f to -0.18f, listOf(0f), Pose.IDLE, 28_000),
    /** 趴在右上角 */
    SIT_TOP_RIGHT(0.85f to -0.15f, listOf(0f), Pose.IDLE, 35_000),
    /** 走到右侧伸懒腰 */
    STRETCH_RIGHT(1.05f to 0.30f, listOf(90f), Pose.IDLE, 22_000),
    /** 跑到底部边沿坐着 */
    SIT_BOTTOM_LEFT(0.15f to 1.05f, listOf(0f), Pose.IDLE, 30_000),
    /** 趴在中间冒号偷瞄 */
    PEEK_COLON(0.50f to -0.05f, listOf(0f), Pose.WATCHING, 18_000),
    /** 突然睡着了 */
    DOZE(0.30f to -0.18f, listOf(0f), Pose.SLEEPING, 40_000),
    /** 抓痒 */
    SCRATCH(0.70f to -0.15f, listOf(0f), Pose.HAPPY, 12_000),
    /** 蹲在底部 */
    CROUCH_BOTTOM_RIGHT(0.85f to 1.05f, listOf(0f), Pose.IDLE, 28_000),
    /** 走到左侧 */
    WALK_LEFT(-0.05f to 0.30f, listOf(270f), Pose.IDLE, 22_000),
    /** 打哈欠（也复用 sleeping 帧） */
    YAWN(0.40f to -0.15f, listOf(0f), Pose.SLEEPING, 8_000),
    /** 突然警觉 */
    ALERT(0.50f to -0.20f, listOf(0f), Pose.WATCHING, 10_000)
}

/** 暂停态：让猫"停下来"——固定在某个位置不切换动作 */
@Composable
fun PausedClockSideMascot(clockBox: IntSize) {
    ClockSideMascot(clockBox = clockBox, paused = true)
}
