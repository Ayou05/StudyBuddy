package com.studybuddy.v2.ui.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 长按 600ms 触发的 Focus 控制按钮 —— 防误触设计。
 *
 * 交互：
 * 1. 默认态：64dp 椭圆 cream-card，居中"长按"小字
 * 2. 长按时：进度环 fill 0→1 over 600ms（drawArc 圆弧）+ 内部填色加深
 * 3. 提前松手：进度回弹消失，无操作
 * 4. 600ms 触发：haptic 震动一下 → 弹"暂停 / 完成"二选一卡
 * 5. 二选一卡 5 秒不点 → 自动收起
 *
 * 不做"放弃" —— ADHD 友好：任何时刻停都不会"白来"，全部累计。
 */
@Composable
fun LongPressFinishButton(
    onPause: () -> Unit,
    onComplete: () -> Unit
) {
    val colors = MaterialTheme.appColors
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val progress = remember { Animatable(0f) }
    val showChoices = remember { mutableStateOf(false) }

    // 5 秒自动收起选项卡
    LaunchedEffect(showChoices.value) {
        if (showChoices.value) {
            delay(5_000)
            showChoices.value = false
        }
    }

    Box(contentAlignment = Alignment.Center) {
        // 二选一卡（在按钮上方弹出）
        AnimatedVisibility(
            visible = showChoices.value,
            enter = fadeIn(tween(200)) + scaleIn(tween(220), initialScale = 0.92f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.92f)
        ) {
            Box(modifier = Modifier.padding(bottom = 96.dp)) {
                ChoiceCard(
                    onPause = {
                        showChoices.value = false
                        onPause()
                    },
                    onComplete = {
                        showChoices.value = false
                        onComplete()
                    }
                )
            }
        }

        // 长按按钮本体
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.surfaceDarkElevated)
                .border(1.dp, ClaudeColors.OnDark.copy(alpha = 0.12f), CircleShape)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        // 启动进度动画
                        val job = scope.launch {
                            progress.snapTo(0f)
                            progress.animateTo(1f, tween(600))
                            // 走到 1 = 触发
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showChoices.value = true
                        }
                        // 等待松手
                        waitForUpOrCancellation()
                        // 提前松手 → 取消并回弹
                        if (!progress.isRunning) {
                            // 已经走完，让弹出 choices 自然处理
                        } else {
                            job.cancel()
                            scope.launch { progress.animateTo(0f, tween(200)) }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 进度环
            Canvas(modifier = Modifier.size(72.dp)) {
                val sw = 3.dp.toPx()
                if (progress.value > 0f) {
                    drawArc(
                        color = ClaudeColors.Primary,
                        startAngle = -90f,
                        sweepAngle = 360f * progress.value,
                        useCenter = false,
                        topLeft = Offset(sw / 2, sw / 2),
                        size = Size(size.width - sw, size.height - sw),
                        style = Stroke(width = sw)
                    )
                }
            }
            // 中央小字
            Text(
                if (showChoices.value) "选" else "长按",
                style = ClaudeType.Caption,
                color = ClaudeColors.OnDark.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    onPause: () -> Unit,
    onComplete: () -> Unit
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surfaceDarkElevated)
            .border(1.dp, ClaudeColors.OnDark.copy(alpha = 0.18f), RoundedCornerShape(28.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // 暂停（左）
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onPause)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("暂停", style = ClaudeType.Button, color = ClaudeColors.OnDark)
        }
        // 中线
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(20.dp)
                .background(ClaudeColors.OnDark.copy(alpha = 0.18f))
        )
        // 完成（右）—— coral 强调
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(ClaudeColors.Primary.copy(alpha = 0.18f))
                .clickable(onClick = onComplete)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("完成", style = ClaudeType.Button, color = ClaudeColors.Primary)
        }
    }
}
