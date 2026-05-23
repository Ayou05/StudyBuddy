package com.studybuddy.v2.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.data.model.SyncInvite
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.AppButton

/**
 * 收到邀请时弹出的 BottomSheet。60s 自动 decline。
 * 顶部一行细 hairline 倒计时进度条，从 1 减到 0。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncInviteSheet(
    invite: SyncInvite,
    fromNickname: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val colors = MaterialTheme.appColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val totalMs = (invite.expiresAt - System.currentTimeMillis()).coerceAtLeast(1L)
    var progress by remember(invite.id) { mutableFloatStateOf(1f) }
    LaunchedEffect(invite.id) {
        // 线性走完到 0 → 自动 decline
        androidx.compose.animation.core.Animatable(1f).animateTo(
            targetValue = 0f,
            animationSpec = tween(totalMs.toInt(), easing = LinearEasing)
        ) { progress = value }
        onDecline()
    }

    ModalBottomSheet(
        onDismissRequest = onDecline,
        sheetState = sheetState,
        containerColor = colors.canvas.copy(alpha = 0.92f),
        contentColor = colors.ink,
        shape = RoundedCornerShape(topStart = ClaudeRadius.xl, topEnd = ClaudeRadius.xl)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ClaudeSpacing.lg)
                .padding(top = ClaudeSpacing.sm, bottom = ClaudeSpacing.xl)
        ) {
            // 顶部 hairline 倒计时
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(colors.hairline.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(ClaudeColors.Primary)
                )
            }
            Spacer(Modifier.height(ClaudeSpacing.lg))

            Text("一起专注", style = ClaudeType.CaptionUppercase, color = colors.muted)
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Text(
                "$fromNickname 想和你一起专注 ${invite.plannedDurationMs / 60_000} 分钟",
                style = ClaudeType.TitleMd,
                color = colors.ink
            )
            if (invite.goal.isNotBlank()) {
                Spacer(Modifier.height(ClaudeSpacing.sm))
                Text(
                    "“${invite.goal}”",
                    style = ClaudeType.BodyMd,
                    color = colors.body
                )
            }

            Spacer(Modifier.height(ClaudeSpacing.xl))

            AppButton.Primary(
                text = "加入",
                leadingIcon = "play",
                onClick = onAccept,
                fullWidth = true
            )
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                AppButton.Text(
                    text = "下次吧",
                    onClick = onDecline,
                    color = colors.muted
                )
            }
        }
    }
}
