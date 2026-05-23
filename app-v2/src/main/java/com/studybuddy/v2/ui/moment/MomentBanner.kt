package com.studybuddy.v2.ui.moment

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.data.moment.Moment
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors

/**
 * 情境层浮卡 —— 浮在 Home 顶部的 banner，向下滑掉关闭。
 *
 * UI 规则：
 * - cream-card 底（不抢戏）
 * - 1px coral hairline 凸出存在感
 * - 标题 + 一行说明 + 1 个动作按钮（按 Moment type 自动选 label / target）
 * - 向下滑掉 / 点关闭按钮 → 消失
 * - 不发系统通知（app 内出现）
 */
@Composable
fun MomentBanner(
    moment: Moment?,
    onAction: ((Moment) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = moment != null,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it / 2 },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 2 }
    ) {
        if (moment != null) {
            BannerCard(moment = moment, onAction = onAction, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun BannerCard(
    moment: Moment,
    onAction: ((Moment) -> Unit)?,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.appColors
    val actionLabel = actionLabelFor(moment)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ClaudeSpacing.pageHorizontal, vertical = ClaudeSpacing.sm)
            .clip(RoundedCornerShape(ClaudeRadius.lg))
            .background(colors.surfaceCard)
            .border(1.dp, ClaudeColors.Primary.copy(alpha = 0.4f), RoundedCornerShape(ClaudeRadius.lg))
            .padding(ClaudeSpacing.lg)
            .pointerInput(moment.id) {
                // 向下滑超过 60dp → 关闭
                var dragY = 0f
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragY > 60.dp.toPx()) onDismiss()
                        dragY = 0f
                    }
                ) { _, drag ->
                    if (drag > 0) dragY += drag
                }
            }
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    titleFor(moment),
                    style = ClaudeType.TitleSm,
                    color = colors.ink,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", style = ClaudeType.TitleMd, color = colors.muted)
                }
            }
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(
                bodyFor(moment),
                style = ClaudeType.BodySm,
                color = colors.body
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(ClaudeSpacing.md))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(ClaudeRadius.pill))
                        .background(ClaudeColors.Primary)
                        .clickable { onAction(moment) }
                        .padding(horizontal = ClaudeSpacing.lg, vertical = ClaudeSpacing.xs)
                ) {
                    Text(actionLabel, style = ClaudeType.Button, color = ClaudeColors.OnPrimary)
                }
            }
        }
    }
}

private fun titleFor(moment: Moment): String = when (moment) {
    is Moment.MeetingStarted -> "你们在一起了"
    is Moment.MeetingEnded -> "刚刚 TA 走了"
    is Moment.StayDetected -> "今天去了 ${moment.placeName}"
    is Moment.PartnerStartedFocus -> "${moment.partnerName} 开始了"
    is Moment.WeekdayBreakNoticed -> "昨天偷懒了"
}

private fun bodyFor(moment: Moment): String = when (moment) {
    is Moment.MeetingStarted -> "已经 ${moment.durationMin} 分钟了"
    is Moment.MeetingEnded -> "这次见面 ${moment.totalMs / 3_600_000}h ${(moment.totalMs / 60_000) % 60}min"
    is Moment.StayDetected -> "在那里待了 ${moment.durationMin} 分钟。要写一段记下来吗？"
    is Moment.PartnerStartedFocus -> "TA 准备专注 ${moment.plannedMin} 分钟，要一起加入吗？"
    is Moment.WeekdayBreakNoticed -> "${moment.ymd} 只专注了 ${moment.actualMin}/${moment.threshold} 分钟，¥${moment.penalizedYuan} 已入金库"
}

private fun actionLabelFor(moment: Moment): String = when (moment) {
    is Moment.MeetingStarted -> "拍一张吧"
    is Moment.MeetingEnded -> "记一下"
    is Moment.StayDetected -> "写一段"
    is Moment.PartnerStartedFocus -> "加入 TA"
    is Moment.WeekdayBreakNoticed -> "看金库"
}
