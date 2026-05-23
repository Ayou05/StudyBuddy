package com.studybuddy.v2.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.data.repo.PreferencesStore
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.pet.saddle.BodyType
import com.studybuddy.v2.ui.pet.saddle.PixelMatrix
import com.studybuddy.v2.ui.pet.saddle.SaddleCatFrames
import com.studybuddy.v2.ui.pet.saddle.SaddleCatSprite
import com.studybuddy.v2.ui.pet.saddle.SaddleCatState
import com.studybuddy.v2.ui.pet.saddle.SaddleshipFrames
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 全屏鞍部猫漫游层。
 *
 * 设计变更（vs 早期版本）：
 * - **删掉右下角 56dp cream 玻璃悬浮框** —— 框 + 漫游中的鞍部猫语义打架，悬浮框被吐槽"丑"
 * - **pixelSize 2dp → 4dp**（HEAD_ONLY 头宽 40dp）—— 体积大一点更可爱、配合 PixelMatrix 消缝完整
 * - **鞍部猫本体可点**：透明命中区比可见 sprite 大 ~50%，方便点击
 * - **三手势**：
 *   - 单击 → popover 弹 2 秒小气泡（动态文案：今日专注分钟 + 一行假命令）
 *   - 双击 → 进 `/mascot` 全屏终端页（由 [onOpenMascotPage] 回调）
 *   - 长按 → 折叠 24h，逃生口
 * - **跨边不翻**：EdgeMascot 的 rotation 不再用 1200ms tween 动画，改成 50ms 快闪
 *   配合 [com.studybuddy.v2.ui.component.MascotRoamer.crossCornerStep] 的"角点歇 + snapTo"
 */
@Composable
fun MascotDock(
    visible: Boolean,
    onOpenMascotPage: () -> Unit,
    allowSleep: Boolean = false,
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val prefs = remember {
        EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            MascotDockEntryPoint::class.java
        ).preferencesStore()
    }
    val persistentState = remember {
        EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            MascotDockEntryPoint::class.java
        ).mascotPersistentState()
    }

    val unlocked by prefs.unlockedSaddleCat.collectAsState(initial = false)
    val enabled by prefs.mascotDockEnabled.collectAsState(initial = true)
    val collapsedUntil by prefs.mascotDockCollapsedUntil.collectAsState(initial = 0L)
    val nowMs = remember { System.currentTimeMillis() }
    val isCollapsed = collapsedUntil > nowMs
    val showMascot = visible && unlocked && enabled && !isCollapsed

    // 退场/入场淡入淡出
    val mascotAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (showMascot) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 500),
        label = "mascotAlpha"
    )

    val scope = rememberCoroutineScope()
    val roamer = remember { MascotRoamer(scope) }
    val avoidRegistry = remember { MascotAvoidRegistry() }
    var canvasBounds by remember { mutableStateOf<Rect?>(null) }
    val pose by roamer.pose.collectAsState()
    val mode by roamer.mode.collectAsState()

    // popover 状态：dock 单击鞍部猫触发，2s 后自动隐藏
    var popoverAt by remember { mutableStateOf<Offset?>(null) }
    var popoverText by remember { mutableStateOf("") }

    // 折叠 24h
    fun collapseFor24h() {
        scope.launch {
            prefs.setMascotDockCollapsedUntil(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)
        }
    }

    DisposableEffect(showMascot, canvasBounds) {
        if (showMascot && canvasBounds != null) {
            // 跨页恢复：上次离开时的 theta / pose
            scope.launch {
                roamer.hydrate(persistentState.lastTheta, persistentState.lastPose)
            }
            roamer.start(
                getBounds = { canvasBounds!! },
                getAvoidRegions = { avoidRegistry.snapshot() }
            )
        }
        onDispose {
            // 退场前快照：跨 tab 切回来时不重置
            persistentState.snapshot(roamer.snapshotTheta(), roamer.snapshotPose())
            roamer.stop()
        }
    }

    DisposableEffect(allowSleep, showMascot) {
        if (allowSleep && showMascot) {
            roamer.startIdleSleep()
        } else {
            roamer.stopIdleSleep()
        }
        onDispose { roamer.stopIdleSleep() }
    }

    CompositionLocalProvider(LocalMascotAvoidRegistry provides avoidRegistry) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    val w = coords.size.width.toFloat()
                    val h = coords.size.height.toFloat()
                    val topInset = with(density) { 28.dp.toPx() }
                    canvasBounds = Rect(
                        left = 0f,
                        top = topInset,
                        right = w,
                        bottom = h
                    )
                }
                // 观察屏幕点击 —— Initial pass 监听 down，不消费事件，用于 idle 计时重置
                .pointerInput(showMascot) {
                    if (!showMascot) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        roamer.bumpInteraction()
                    }
                }
        ) {
            content()

            if (mascotAlpha > 0.01f && canvasBounds != null) {
                val bounds = canvasBounds!!
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = mascotAlpha }) {
                    when (mode) {
                        MascotRoamer.Mode.EDGE -> EdgeMascot(
                            roamer = roamer,
                            bounds = bounds,
                            pose = pose,
                            density = density,
                            onSingleTap = { tapAt, mascotCenter ->
                                // 凑近点击触发"受惊"；普通点击弹 popover
                                val startled = roamer.startleIfNear(tapAt, mascotCenter)
                                if (!startled) {
                                    popoverAt = tapAt
                                    popoverText = nextPopoverLine()
                                    scope.launch {
                                        delay(2000L)
                                        popoverAt = null
                                    }
                                }
                            },
                            onDoubleTap = { onOpenMascotPage() },
                            onLongPress = { collapseFor24h() }
                        )
                        MascotRoamer.Mode.INTERIOR -> InteriorMascot(
                            roamer = roamer,
                            pose = pose,
                            density = density,
                            onSingleTap = { tapAt, mascotCenter ->
                                val startled = roamer.startleIfNear(tapAt, mascotCenter)
                                if (!startled) {
                                    popoverAt = tapAt
                                    popoverText = nextPopoverLine()
                                    scope.launch {
                                        delay(2000L)
                                        popoverAt = null
                                    }
                                }
                            },
                            onDoubleTap = { onOpenMascotPage() },
                            onLongPress = { collapseFor24h() }
                        )
                    }
                }
            }

            // popover 浮在最上层
            popoverAt?.let { pos ->
                MascotPopover(
                    text = popoverText,
                    anchor = pos,
                    bounds = canvasBounds,
                    density = density
                )
            }
        }
    }
}

private val popoverLines = listOf(
    "> tail -f study.log",
    "> [ok] mood synced",
    "> sudo make focus",
    "> $ pet saddle-cat",
    "> compiling focus.k...",
    "> syscall: meow()",
    "> zsh: command not found: distract",
    "> [warn] hunger < 30",
    "> awk '{ studying += 1 }'",
    "> grep -i hope ~/today",
    "> echo \"还在呢\"",
    "> ps aux | grep cozy"
)

private fun nextPopoverLine(): String = popoverLines.random()

@Composable
private fun EdgeMascot(
    roamer: MascotRoamer,
    bounds: Rect,
    pose: com.studybuddy.v2.ui.pet.saddle.Pose,
    density: androidx.compose.ui.unit.Density,
    onSingleTap: (tapAt: Offset, mascotCenter: Offset) -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val mascotColor = androidx.compose.material3.MaterialTheme.appColors.mascotInk
    val theta = roamer.theta
    val edge = roamer.edgeAt(theta, bounds)
    val railPoint = roamer.positionFor(theta, bounds)

    val pixelDp = 4.dp  // 2 → 4，更显眼，缝已修
    val spriteW = pixelDp * SaddleCatFrames.HeadOnly.COLS
    val spriteH = pixelDp * SaddleCatFrames.HeadOnly.ROWS
    val spriteWPx = with(density) { spriteW.toPx() }
    val spriteHPx = with(density) { spriteH.toPx() }

    val centerX: Float
    val centerY: Float
    val rotation: Float
    when (edge) {
        Edge.BOTTOM -> {
            centerX = railPoint.x
            centerY = railPoint.y - spriteHPx / 2f
            rotation = 0f
        }
        Edge.LEFT -> {
            centerX = railPoint.x + spriteHPx / 2f
            centerY = railPoint.y
            rotation = 90f
        }
        Edge.RIGHT -> {
            centerX = railPoint.x - spriteHPx / 2f
            centerY = railPoint.y
            rotation = 270f
        }
    }
    // rotation 改成 50ms 快闪 —— 配合 MascotRoamer.crossCornerStep 的"角点 snapTo"
    // theta 瞬切时 rotation 也几乎瞬切，从用户视角看 = 不翻
    val animRotation by animateFloatAsState(rotation, tween(50), label = "mascotEdgeRotation")

    val sideMax = maxOf(spriteWPx, spriteHPx)
    val topLeft = Offset(centerX - sideMax / 2f, centerY - sideMax / 2f)

    // 命中区比 sprite 大 50%，方便点击移动中的小目标
    val hitPadDp = 16.dp
    val hitSize = with(density) { sideMax.toDp() } + hitPadDp * 2

    Box(
        modifier = Modifier
            .size(hitSize)
            .offset(
                x = with(density) { topLeft.x.toDp() } - hitPadDp,
                y = with(density) { topLeft.y.toDp() } - hitPadDp
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { localOffset ->
                        // localOffset 是相对 hit area 左上角；映射回屏幕坐标
                        val tapAtScreen = Offset(
                            x = topLeft.x - 16.dp.toPx() + localOffset.x,
                            y = topLeft.y - 16.dp.toPx() + localOffset.y
                        )
                        onSingleTap(tapAtScreen, Offset(centerX, centerY))
                    },
                    onDoubleTap = { onDoubleTap() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .size(with(density) { sideMax.toDp() })
                .offset(x = hitPadDp, y = hitPadDp)
                .graphicsLayer { rotationZ = animRotation }
        ) {
            Box(
                modifier = Modifier
                    .size(width = spriteW, height = spriteH)
                    .offset(
                        x = with(density) { ((sideMax - spriteWPx) / 2f).toDp() },
                        y = with(density) { ((sideMax - spriteHPx) / 2f).toDp() }
                    )
            ) {
                // STARTLED 时整体 translateY -8dp 跳起来 + 200ms 回落
                val startled = pose == com.studybuddy.v2.ui.pet.saddle.Pose.STARTLED
                val jumpDy by animateFloatAsState(
                    targetValue = if (startled) -with(density) { 8.dp.toPx() } else 0f,
                    animationSpec = tween(200),
                    label = "mascotStartledJump"
                )
                Box(modifier = Modifier.graphicsLayer { translationY = jumpDy }) {
                    SaddleCatSprite(
                        state = SaddleCatState(pose = pose),
                        pixelSize = pixelDp,
                        color = mascotColor,
                        bodyType = BodyType.HEAD_ONLY
                    )
                }
            }
        }
    }
}

@Composable
private fun InteriorMascot(
    roamer: MascotRoamer,
    pose: com.studybuddy.v2.ui.pet.saddle.Pose,
    density: androidx.compose.ui.unit.Density,
    onSingleTap: (tapAt: Offset, mascotCenter: Offset) -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val mascotColor = androidx.compose.material3.MaterialTheme.appColors.mascotInk
    val shipColor = androidx.compose.material3.MaterialTheme.appColors.mascotShip
    val pixelDp = 4.dp  // INTERIOR 跟 EDGE 同尺寸
    val catW = pixelDp * SaddleCatFrames.HeadOnly.COLS
    val catH = pixelDp * SaddleCatFrames.HeadOnly.ROWS
    val shipW = pixelDp * SaddleshipFrames.COLS
    val shipH = pixelDp * SaddleshipFrames.ROWS
    val gapDp = 0.dp

    val totalH = catH + gapDp + shipH
    val totalW = maxOf(catW, shipW)

    val totalWPx = with(density) { totalW.toPx() }
    val totalHPx = with(density) { totalH.toPx() }
    val pos = roamer.interiorPos
    val topLeftX = pos.x - totalWPx / 2f
    val topLeftY = pos.y - totalHPx / 2f

    var shipFrameIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(180L)
            shipFrameIdx = (shipFrameIdx + 1) % SaddleshipFrames.thrustFrames.size
        }
    }

    val hitPadDp = 16.dp

    Box(
        modifier = Modifier
            .size(width = totalW + hitPadDp * 2, height = totalH + hitPadDp * 2)
            .offset(
                x = with(density) { topLeftX.toDp() } - hitPadDp,
                y = with(density) { topLeftY.toDp() } - hitPadDp
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { localOffset ->
                        val tapAtScreen = Offset(
                            x = topLeftX - 16.dp.toPx() + localOffset.x,
                            y = topLeftY - 16.dp.toPx() + localOffset.y
                        )
                        onSingleTap(tapAtScreen, Offset(pos.x, pos.y))
                    },
                    onDoubleTap = { onDoubleTap() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .size(width = totalW, height = totalH)
                .offset(x = hitPadDp, y = hitPadDp)
        ) {
            // 鞍部猫在上
            Box(
                modifier = Modifier
                    .size(width = catW, height = catH)
                    .offset(x = with(density) { ((totalWPx - with(density) { catW.toPx() }) / 2f).toDp() })
            ) {
                SaddleCatSprite(
                    state = SaddleCatState(pose = pose),
                    pixelSize = pixelDp,
                    color = mascotColor,
                    bodyType = BodyType.HEAD_ONLY
                )
            }
            // 飞船在下
            Box(
                modifier = Modifier
                    .size(width = shipW, height = shipH)
                    .offset(
                        x = with(density) { ((totalWPx - with(density) { shipW.toPx() }) / 2f).toDp() },
                        y = catH
                    )
            ) {
                PixelMatrix(
                    matrix = SaddleshipFrames.thrustFrames[shipFrameIdx],
                    rows = SaddleshipFrames.ROWS,
                    cols = SaddleshipFrames.COLS,
                    pixelSize = pixelDp,
                    color = shipColor
                )
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MascotDockEntryPoint {
    fun preferencesStore(): PreferencesStore
    fun mascotPersistentState(): MascotPersistentState
}
