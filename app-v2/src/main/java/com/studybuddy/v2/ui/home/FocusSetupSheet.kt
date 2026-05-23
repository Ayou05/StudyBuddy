package com.studybuddy.v2.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.AppButton

/**
 * 专注模式与时长配置 BottomSheet。
 *
 * 不强制每次弹 —— 主按钮直进 Focus（走 PreferencesStore 上次的偏好）；
 * 配置只藏在按钮下方一行 muted caption ("倒计时 · 25 分钟 →") 整行可点。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSetupSheet(
    currentMode: String,            // "COUNTDOWN" / "STOPWATCH"
    currentDurationMin: Int,
    onDismiss: () -> Unit,
    onConfirm: (mode: String, durationMin: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.appColors
    var mode by remember { mutableStateOf(currentMode) }
    var duration by remember { mutableStateOf(currentDurationMin) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.canvas.copy(alpha = 0.82f),  // 玻璃感半透
        contentColor = colors.ink,
        shape = RoundedCornerShape(topStart = ClaudeRadius.xl, topEnd = ClaudeRadius.xl)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ClaudeSpacing.lg)
                .padding(top = ClaudeSpacing.sm, bottom = ClaudeSpacing.xl)
        ) {
            Text("专注偏好", style = ClaudeType.TitleMd, color = colors.ink)
            Spacer(Modifier.height(ClaudeSpacing.xxs))
            Text("可以随时改。下次会记住。", style = ClaudeType.BodySm, color = colors.muted)

            Spacer(Modifier.height(ClaudeSpacing.lg))

            // ─── 模式：分段控件 ─────────────────────────
            Text("模式", style = ClaudeType.CaptionUppercase, color = colors.muted)
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ClaudeRadius.md))
                    .border(1.dp, colors.hairline, RoundedCornerShape(ClaudeRadius.md))
            ) {
                SegmentTab(
                    label = "倒计时",
                    selected = mode == "COUNTDOWN",
                    onClick = { mode = "COUNTDOWN" },
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(colors.hairline)
                )
                SegmentTab(
                    label = "正计时",
                    selected = mode == "STOPWATCH",
                    onClick = { mode = "STOPWATCH" },
                    modifier = Modifier.weight(1f)
                )
            }

            // ─── 时长（仅倒计时显示） ───────────────────
            if (mode == "COUNTDOWN") {
                Spacer(Modifier.height(ClaudeSpacing.lg))
                Text("时长", style = ClaudeType.CaptionUppercase, color = colors.muted)
                Spacer(Modifier.height(ClaudeSpacing.xs))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ClaudeSpacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(15, 25, 45, 90).forEach { d ->
                        DurationChip(
                            minutes = d,
                            selected = d == duration,
                            onClick = { duration = d },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(ClaudeSpacing.lg))
                Text(
                    "正计时不限制时长。完成时由你决定何时停。",
                    style = ClaudeType.BodySm,
                    color = colors.muted
                )
            }

            Spacer(Modifier.height(ClaudeSpacing.xl))
            AppButton.Primary(
                text = "用这个开始",
                onClick = {
                    onConfirm(mode, duration)
                    onDismiss()
                },
                fullWidth = true
            )
        }
    }
}

@Composable
private fun SegmentTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.appColors
    val bg = if (selected) ClaudeColors.Primary else Color.Transparent
    val fg = if (selected) ClaudeColors.OnPrimary else colors.ink
    Box(
        modifier = modifier
            .height(40.dp)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = ClaudeType.Button, color = fg)
    }
}

@Composable
private fun DurationChip(
    minutes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.appColors
    val bg = if (selected) colors.surfaceCard else colors.canvas
    val borderColor = if (selected) ClaudeColors.Primary else colors.hairline
    val borderW = if (selected) 1.5.dp else 1.dp
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(ClaudeRadius.md))
            .background(bg)
            .border(borderW, borderColor, RoundedCornerShape(ClaudeRadius.md))
            .clickable(onClick = onClick)
            .padding(vertical = ClaudeSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$minutes", style = ClaudeType.TitleMd, color = colors.ink)
        Text("min", style = ClaudeType.Caption, color = colors.muted)
    }
}
