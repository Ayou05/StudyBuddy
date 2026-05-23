package com.studybuddy.v2.ui.focus

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.data.model.FocusStatus
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.AppButton
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * 沉浸式专注。整页 surface-dark。
 *
 * 交互模型（Apple-style 三层收敛）：
 *   主屏只显示 1 个**暂停**圆形按钮（避免误触放弃 / 心理负担）
 *   点暂停 → 时间定格 → 弹 PauseSheet（cream-card 底部）
 *   Sheet 三选项：继续 / 结束并保存 / 放弃
 *
 * 模式分支：
 *   COUNTDOWN 倒计时 → 圆环进度 + remaining 大数字 + "FOCUS · 25 MIN" 标签
 *   STOPWATCH 正计时 → 隐藏圆环 + elapsed 大数字 + Milestone 副标 + "FOCUS · 自由"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    focusType: String = "SINGLE",
    onDismiss: () -> Unit,
    viewModel: FocusViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val ctx = LocalContext.current

    // 全屏隐藏状态栏 / 导航栏
    DisposableEffect(Unit) {
        val window = (ctx as Activity).window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.isAppearanceLightStatusBars = false
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.isAppearanceLightStatusBars = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.setFocusType(focusType)
        viewModel.init()
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { e -> if (e is FocusEvent.Dismiss) onDismiss() }
    }
    BackHandler { viewModel.pause() }   // 物理返回 → 暂停（不直接退出）

    // PauseSheet 显隐：每次 status 变成 PAUSED 时自动弹
    var showPauseSheet by remember { mutableStateOf(false) }
    LaunchedEffect(state.status) {
        showPauseSheet = state.status == FocusStatus.PAUSED
    }

    val colors = MaterialTheme.appColors
    // 全屏入场：alpha 0→1 + 中央内容 0.96→1.0，800ms / Cubic(0.4,0,0.2,1) 近似 EaseOutCubic
    val entryAlpha = remember { Animatable(0f) }
    val entryScale = remember { Animatable(0.96f) }
    LaunchedEffect(Unit) {
        entryAlpha.animateTo(1f, tween(800, easing = EaseOutCubic))
    }
    LaunchedEffect(Unit) {
        entryScale.animateTo(1f, tween(800, easing = EaseOutCubic))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(entryAlpha.value)
                .scale(entryScale.value)
        ) {
            if (state.focusType == "SYNC") {
                SyncFocusBody(state = state, onPause = viewModel::pause)
            } else {
                FocusBody(state = state, onPause = viewModel::pause)
            }
        }

        // SYNC 3-2-1 全屏入场
        if (state.countdown321 > 0) {
            Countdown321Overlay(state.countdown321)
        }

        if (showPauseSheet) {
            PauseSheet(
                elapsedSec = state.elapsedSec,
                onResume = {
                    showPauseSheet = false
                    viewModel.resume()
                },
                onComplete = {
                    showPauseSheet = false
                    viewModel.complete()
                },
                onAbort = {
                    showPauseSheet = false
                    viewModel.abort()
                },
                onDismiss = {
                    // 滑下 sheet 视为继续
                    showPauseSheet = false
                    viewModel.resume()
                }
            )
        }

        AnimatedVisibility(
            visible = state.celebrating,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(300))
        ) {
            CompletedOverlay()
        }
    }
}

@Composable
private fun FocusBody(state: FocusUiState, onPause: () -> Unit) {
    val focusVm: FocusViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val colors = MaterialTheme.appColors
    val isStopwatch = state.mode == "STOPWATCH"
    val remaining = (state.plannedSec - state.elapsedSec).coerceAtLeast(0)
    val displaySec = if (isStopwatch) state.elapsedSec else remaining
    val showCompleteConfirm = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ClaudeSpacing.pageHorizontal)
            .padding(top = 64.dp, bottom = 48.dp)
    ) {
        // 顶部主题 caption —— caption-uppercase
        TopicCaption(state)

        Spacer(Modifier.weight(0.3f))

        // 大数字 —— Copernicus serif display-xl，cream 底无卡片包裹
        var clockBox by remember { mutableStateOf(IntSize.Zero) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { clockBox = it },
            contentAlignment = Alignment.Center
        ) {
            CountdownTextSerif(
                remainingOrElapsed = displaySec,
                paused = state.status == FocusStatus.PAUSED
            )
            // 鞍部猫以时钟为家活动
            ClockSideMascot(clockBox = clockBox, paused = state.status == FocusStatus.PAUSED)
        }

        // 副标信息（地标 / 正计时里程碑）
        if (state.landmarkName != null) {
            Spacer(Modifier.height(ClaudeSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                LandmarkBadge(name = state.landmarkName!!)
            }
        }
        if (isStopwatch) {
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Text(
                text = stopwatchMilestone(displaySec),
                style = ClaudeType.Caption,
                color = colors.muted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(Modifier.weight(0.3f))

        // 1px hairline 中线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.hairline)
        )

        Spacer(Modifier.height(ClaudeSpacing.lg))

        // TA 信息条
        PartnerInfoStripCream(state = state)

        Spacer(Modifier.weight(0.4f))

        // 暂停 / 完成 双按钮（普通，不长按）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ClaudeSpacing.md)
        ) {
            // 暂停 button-secondary
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(ClaudeRadius.md))
                    .background(colors.canvas)
                    .border(1.dp, colors.hairline, RoundedCornerShape(ClaudeRadius.md))
                    .clickable(onClick = onPause),
                contentAlignment = Alignment.Center
            ) {
                Text("暂停", style = ClaudeType.Button, color = colors.ink)
            }
            // 完成 button-primary（带二次确认）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(ClaudeRadius.md))
                    .background(ClaudeColors.Primary)
                    .clickable { showCompleteConfirm.value = true },
                contentAlignment = Alignment.Center
            ) {
                Text("完成", style = ClaudeType.Button, color = ClaudeColors.OnPrimary)
            }
        }
    }

    if (showCompleteConfirm.value) {
        val mm = state.elapsedSec / 60
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCompleteConfirm.value = false },
            containerColor = colors.surfaceCard,
            title = { Text("完成这一段？", style = ClaudeType.TitleMd, color = colors.ink) },
            text = {
                Text(
                    if (mm < 1) "刚开始而已，要再坚持一下吗？"
                    else "已专注 $mm 分钟。这一段会被记录。",
                    style = ClaudeType.BodySm,
                    color = colors.body
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showCompleteConfirm.value = false
                    focusVm.complete()
                }) { Text("完成", color = ClaudeColors.Primary) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showCompleteConfirm.value = false
                }) { Text("继续专注", color = colors.muted) }
            }
        )
    }
}

/** 顶部主题名 caption-uppercase（按规范 12sp / 1.5px tracking） */
@Composable
private fun TopicCaption(state: FocusUiState) {
    val colors = MaterialTheme.appColors
    val name = state.topicName?.takeIf { it.isNotBlank() } ?: "自由专注"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        if (state.topicColorHex != null) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(parseTopicColor(state.topicColorHex!!))
            )
            Spacer(Modifier.size(ClaudeSpacing.sm))
        }
        Text(
            name.uppercase(),
            style = ClaudeType.CaptionUppercase,
            color = colors.muted
        )
    }
}

/** 大数字时钟 —— Copernicus serif display-xl，weight 400，负字间距，cream 底直接显示 */
@Composable
private fun CountdownTextSerif(remainingOrElapsed: Long, paused: Boolean) {
    val colors = MaterialTheme.appColors
    val mm = remainingOrElapsed / 60
    val ss = remainingOrElapsed % 60
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(remainingOrElapsed) {
        if (!paused) {
            pulse.animateTo(1.02f, tween(110, easing = EaseOutCubic))
            pulse.animateTo(1f, tween(110, easing = EaseOutCubic))
        }
    }
    Text(
        text = "%02d : %02d".format(mm, ss),
        style = androidx.compose.ui.text.TextStyle(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
            fontSize = 84.sp,
            lineHeight = 96.sp,
            letterSpacing = (-1.5).sp
        ),
        color = if (paused) colors.muted else colors.ink,
        modifier = Modifier.scale(pulse.value),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

/** TA 信息条 —— cream 底，简单一行+副行 */
@Composable
private fun PartnerInfoStripCream(state: FocusUiState) {
    val colors = MaterialTheme.appColors
    val partnerName = state.partnerNickname?.takeIf { it.isNotBlank() } ?: "TA"
    val partnerSec = state.partnerElapsedSec
    val partnerActive = state.partnerFocusStatus == "ACTIVE"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(partnerName, style = ClaudeType.TitleSm, color = colors.ink)
            Spacer(Modifier.height(2.dp))
            val sub = when {
                partnerActive && state.partnerInLibrary -> "在图书馆 · 专注中"
                partnerActive -> "正在专注"
                state.partnerFocusStatus == "PAUSED" -> "刚刚暂停了"
                else -> "还没开始"
            }
            Text(sub, style = ClaudeType.Caption, color = colors.muted)
        }
        if (partnerSec > 0) {
            Text(
                "%02d:%02d".format(partnerSec / 60, partnerSec % 60),
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                    fontSize = 22.sp,
                    letterSpacing = (-0.3).sp
                ),
                color = colors.body
            )
        }
    }
}

@Composable
private fun SelfClockCard(
    displaySec: Long,
    paused: Boolean,
    isStopwatch: Boolean,
    landmarkName: String?,
    modifier: Modifier = Modifier
) {
    // deprecated: 旧的 dark mockup 版本，留作引用，不再使用
    val colors = MaterialTheme.appColors
    var clockBox by remember { mutableStateOf(IntSize.Zero) }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ClaudeRadius.xl))
                .background(colors.surfaceDarkElevated.copy(alpha = 0.6f))
                .padding(vertical = 40.dp, horizontal = 24.dp)
                .onSizeChanged { clockBox = it },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CountdownText(remainingOrElapsed = displaySec, paused = paused)
            if (landmarkName != null) {
                Spacer(Modifier.height(ClaudeSpacing.md))
                LandmarkBadge(name = landmarkName)
            }
            if (isStopwatch) {
                Spacer(Modifier.height(ClaudeSpacing.xs))
                Text(
                    text = stopwatchMilestone(displaySec),
                    style = ClaudeType.Caption,
                    color = colors.onDarkSoft.copy(alpha = 0.6f)
                )
            }
        }
        ClockSideMascot(clockBox = clockBox, paused = paused)
    }
}

/** TA 信息条：横排 一行紧凑（旧 dark 版） */
@Composable
private fun PartnerInfoStrip(state: FocusUiState, onCallTa: () -> Unit) {
    val colors = MaterialTheme.appColors
    val partnerName = state.partnerNickname?.takeIf { it.isNotBlank() } ?: "TA"
    val partnerSec = state.partnerElapsedSec
    val partnerActive = state.partnerFocusStatus == "ACTIVE"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左：TA 名字 + 主题 + 状态副标
        Column(modifier = Modifier.weight(1f)) {
            Text(partnerName, style = ClaudeType.TitleSm, color = colors.onDark)
            Spacer(Modifier.height(2.dp))
            val sub = when {
                partnerActive && state.partnerInLibrary -> "在图书馆 · 专注中"
                partnerActive -> "专注中"
                state.partnerFocusStatus == "PAUSED" -> "刚刚暂停了"
                else -> "还没开始 · 寄飞机叫 TA？"
            }
            Text(sub, style = ClaudeType.Caption, color = colors.onDarkSoft)
        }
        // 右：TA 倒计时数字（如果在专注）
        if (partnerSec > 0) {
            Text(
                "%02d:%02d".format(partnerSec / 60, partnerSec % 60),
                style = ClaudeType.TimerMd,
                color = colors.onDark
            )
        }
    }
}

private fun parseTopicColor(hex: String): androidx.compose.ui.graphics.Color = try {
    val cleaned = hex.removePrefix("#")
    val argb = cleaned.toLong(16) or 0xFF000000L
    androidx.compose.ui.graphics.Color(argb.toInt())
} catch (_: Exception) {
    ClaudeColors.Primary
}

@Composable
private fun CountdownText(remainingOrElapsed: Long, paused: Boolean) {
    val colors = MaterialTheme.appColors
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(remainingOrElapsed) {
        if (!paused) {
            pulse.animateTo(1.04f, tween(110, easing = EaseOutCubic))
            pulse.animateTo(1f, tween(110, easing = EaseOutCubic))
        }
    }
    val totalSec = remainingOrElapsed
    val hh = totalSec / 3600
    val mm = (totalSec % 3600) / 60
    val ss = totalSec % 60
    val text = if (hh > 0) "%d:%02d:%02d".format(hh, mm, ss) else "%02d:%02d".format(mm, ss)
    Text(
        text,
        style = ClaudeType.TimerXl,
        color = colors.onDark,
        modifier = Modifier.scale(pulse.value)
    )
}

/** 正计时模式下的递进里程碑文字。 */
private fun stopwatchMilestone(elapsedSec: Long): String {
    val min = elapsedSec / 60
    return when {
        min < 5 -> "刚开始。把节奏慢下来。"
        min < 25 -> "进入状态了。"
        min < 50 -> "已经超过一个番茄。"
        min < 90 -> "已经超过半小时。"
        min < 120 -> "已经超过一小时。"
        else -> "深度心流中。"
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// PauseSheet —— Apple-style 三层收敛的二级菜单
// ═════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PauseSheet(
    elapsedSec: Long,
    onResume: () -> Unit,
    onComplete: () -> Unit,
    onAbort: () -> Unit,    // 保留签名兼容旧调用，但 UI 不再暴露
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.appColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mm = elapsedSec / 60
    val ss = elapsedSec % 60

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.canvas,
        contentColor = colors.ink,
        shape = RoundedCornerShape(topStart = ClaudeRadius.xl, topEnd = ClaudeRadius.xl)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ClaudeSpacing.lg)
                .padding(top = ClaudeSpacing.sm, bottom = ClaudeSpacing.xl)
        ) {
            Text("已暂停 · 已专注", style = ClaudeType.CaptionUppercase, color = colors.muted)
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(
                "%02d:%02d".format(mm, ss),
                style = ClaudeType.TimerLg,
                color = colors.ink
            )
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(
                if (mm < 1) "刚刚才开始，要再坚持一下吗？"
                else "够了的话可以保存这一段。这一段的时间会留下来。",
                style = ClaudeType.BodySm,
                color = colors.muted
            )

            Spacer(Modifier.height(ClaudeSpacing.xl))

            AppButton.Primary(
                text = "继续",
                leadingIcon = "play",
                onClick = onResume,
                fullWidth = true
            )
            Spacer(Modifier.height(ClaudeSpacing.sm))
            AppButton.Secondary(
                text = "结束并保存",
                leadingIcon = "check",
                onClick = onComplete,
                fullWidth = true
            )
            // 不再有"放弃这一段" —— 任何暂停后想停都走"结束并保存"，已专注时长全部累计
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 完成庆祝层
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun CompletedOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClaudeColors.Primary)
    ) {
        Confetti()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ClaudeSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("完成了。", style = ClaudeType.DisplayXl, color = ClaudeColors.OnPrimary)
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Text(
                "把这份专注记下来，明天继续。",
                style = ClaudeType.BodyMd,
                color = ClaudeColors.OnPrimary.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun Confetti() {
    val palette = listOf(
        ClaudeColors.OnPrimary,
        ClaudeColors.AccentTeal,
        ClaudeColors.AccentAmber,
        ClaudeColors.SurfaceCreamStrong
    )
    val pieces = remember {
        List(40) {
            Triple(
                (Math.random() * 360.0).toFloat(),
                (0.4 + Math.random() * 0.5).toFloat(),
                palette[(Math.random() * palette.size).toInt()]
            )
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(1500, easing = LinearEasing)) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height * 0.45f
        val time = t.value
        pieces.forEach { (angle, speed, color) ->
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            val gravity = 600f * time * time
            val x = cx + cos(rad) * speed * size.width * 0.5f * time
            val y = cy + sin(rad) * speed * size.height * 0.3f * time + gravity
            rotate(degrees = angle * time * 4f, pivot = Offset(x, y)) {
                drawRect(
                    color = color,
                    topLeft = Offset(x - 3.dp.toPx(), y - 6.dp.toPx()),
                    size = Size(6.dp.toPx(), 12.dp.toPx())
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SYNC 双分栏 —— 左 50% 我，右 50% TA，中间 1dp coral hairline
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun SyncFocusBody(state: FocusUiState, onPause: () -> Unit) {
    val colors = MaterialTheme.appColors
    val remaining = (state.plannedSec - state.elapsedSec).coerceAtLeast(0)
    val displaySec = if (state.mode == "STOPWATCH") state.elapsedSec else remaining

    Row(modifier = Modifier.fillMaxSize()) {
        // 左：我
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = ClaudeSpacing.md, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("我", style = ClaudeType.CaptionUppercase, color = colors.onDarkSoft)
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Text(state.goal.ifBlank { "把无关紧要的事都放下" },
                style = ClaudeType.BodySm, color = colors.onDarkSoft.copy(alpha = 0.7f))
            if (state.landmarkName != null) {
                Spacer(Modifier.height(ClaudeSpacing.sm))
                LandmarkBadge(name = state.landmarkName!!)
            }
            Spacer(Modifier.weight(1f))
            CountdownText(remainingOrElapsed = displaySec, paused = state.status == FocusStatus.PAUSED)
            Spacer(Modifier.height(ClaudeSpacing.lg))
            Text(
                text = when (state.syncState) {
                    SyncFocusStateMachine.State.INVITING -> "等待 ${state.partner.nickname.ifBlank { "TA" }} 应答…"
                    SyncFocusStateMachine.State.CONFIRMING -> "TA 来了。准备开始…"
                    SyncFocusStateMachine.State.ACTIVE,
                    SyncFocusStateMachine.State.PAUSED -> "和 ${state.partner.nickname.ifBlank { "TA" }} 一起专注中"
                    SyncFocusStateMachine.State.ABORTED -> "已结束"
                    SyncFocusStateMachine.State.COMPLETED -> "完成了。"
                    else -> "专注中"
                },
                style = ClaudeType.BodySm, color = colors.onDarkSoft
            )
            Spacer(Modifier.weight(1f))
            AppButton.IconCircle(
                iconName = "pause",
                onClick = onPause,
                size = 64.dp,
                bg = colors.surfaceDarkElevated,
                fg = colors.onDark
            )
        }
        // 中线
        Box(modifier = Modifier
            .width(1.dp)
            .fillMaxSize()
            .background(ClaudeColors.Primary.copy(alpha = 0.4f)))
        // 右：TA
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(colors.surfaceDarkSoft)
                .padding(horizontal = ClaudeSpacing.md, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(state.partner.nickname.ifBlank { "TA" },
                style = ClaudeType.CaptionUppercase, color = colors.onDarkSoft)
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Text(
                text = when {
                    !state.partner.isPresent -> "离线"
                    state.partner.focusStatus == "PAUSED" -> "暂停了"
                    state.partner.focusStatus == "ACTIVE" -> "正在专注"
                    else -> "刚到"
                },
                style = ClaudeType.BodySm, color = colors.onDarkSoft.copy(alpha = 0.7f)
            )
            Spacer(Modifier.weight(1f))
            val partnerSec = state.partner.elapsedSec
            val pHh = partnerSec / 3600
            val pMm = (partnerSec % 3600) / 60
            val pSs = partnerSec % 60
            val partnerText = if (pHh > 0) "%d:%02d:%02d".format(pHh, pMm, pSs)
                              else "%02d:%02d".format(pMm, pSs)
            Text(
                partnerText,
                style = ClaudeType.TimerXl,
                color = colors.onDark.copy(alpha = if (state.partner.isPresent) 1f else 0.4f)
            )
            Spacer(Modifier.height(ClaudeSpacing.lg))
            if (state.partner.isInLibrary) {
                LandmarkBadge(name = state.partner.libraryName ?: "TA 的常去地")
            }
            Spacer(Modifier.weight(2f))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3-2-1 全屏入场
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun Countdown321Overlay(n: Int) {
    val colors = MaterialTheme.appColors
    val scale = remember(n) { Animatable(1.4f) }
    val alpha = remember(n) { Animatable(1f) }
    LaunchedEffect(n) {
        scale.animateTo(1f, tween(900, easing = EaseOutCubic))
    }
    LaunchedEffect(n) {
        delay(700)
        alpha.animateTo(0f, tween(300))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceDark.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$n",
            style = ClaudeType.DisplayXl,
            color = ClaudeColors.OnDark,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        )
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// 在地标内 —— 一句温柔的陪伴
// 不写 "×2"，不写 "buff"。只是被这个地方记住了
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LandmarkBadge(name: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                ClaudeColors.Primary.copy(alpha = 0.10f),
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = ClaudeSpacing.sm, vertical = 4.dp)
    ) {
        Canvas(modifier = Modifier.size(6.dp)) {
            drawCircle(color = ClaudeColors.Primary)
        }
        Spacer(Modifier.width(ClaudeSpacing.xs))
        Text(
            "在 $name",
            style = ClaudeType.Caption,
            color = ClaudeColors.Primary
        )
    }
}
