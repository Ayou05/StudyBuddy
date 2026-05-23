package com.studybuddy.v2.ui.unbind

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.AppCard
import com.studybuddy.v2.ui.component.BackRow

/**
 * 解绑冷静期页面。
 *
 * 三态：
 * 1. 没绑搭档 → 显示"还没有搭档"占位
 * 2. 已绑且无未撤回请求 → 显示"解除关系"按钮（警示色）+ 说明
 * 3. 有冷静期请求 → 显示倒计时 + "我想反悔"按钮
 */
@Composable
fun UnbindScreen(
    onBack: () -> Unit = {},
    viewModel: UnbindViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ClaudeSpacing.pageHorizontal)
            .padding(top = ClaudeSpacing.lg, bottom = ClaudeSpacing.xxl)
    ) {
        BackRow(onBack = onBack)
        Spacer(Modifier.height(ClaudeSpacing.md))
        Text("RELATIONSHIP", style = ClaudeType.CaptionUppercase, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.sm))
        Text("解除搭档关系", style = ClaudeType.DisplayLg, color = colors.ink)
        Spacer(Modifier.height(ClaudeSpacing.xl))

        when {
            state.loading -> { /* empty */ }
            !state.hasPartner -> NoPartnerState()
            state.active != null -> CooldownState(
                cooldownEndsAt = state.active!!.cooldownEndsAt,
                operating = state.operating,
                onCancel = viewModel::cancelRequest
            )
            else -> NormalState(
                partnerNickname = state.partnerNickname.ifBlank { "TA" },
                onRequest = viewModel::showConfirm
            )
        }
    }

    if (state.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissConfirm,
            containerColor = colors.surfaceCard,
            title = { Text("真的要走吗？", style = ClaudeType.TitleLg, color = colors.ink) },
            text = {
                Text(
                    "解除关系会启动 7 天冷静期。这期间你们的宠物会冬眠（不再衰减），" +
                            "任一方都可以反悔。7 天后如无人反悔，关系会真正解除，所有共建数据保留但归档。",
                    style = ClaudeType.BodySm, color = colors.body
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::submitRequest) {
                    Text("开始冷静期", color = ClaudeColors.Warning)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissConfirm) {
                    Text("再想想", color = colors.muted)
                }
            }
        )
    }
}

@Composable
private fun NoPartnerState() {
    val colors = MaterialTheme.appColors
    AppCard.Feature(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
        Text("还没有搭档。", style = ClaudeType.TitleMd, color = colors.ink)
        Spacer(Modifier.height(ClaudeSpacing.xs))
        Text(
            "去主页绑定搭档之后，才需要解绑这一说。",
            style = ClaudeType.BodySm, color = colors.muted
        )
    }
}

@Composable
private fun NormalState(partnerNickname: String, onRequest: () -> Unit) {
    val colors = MaterialTheme.appColors
    Column {
        AppCard.Feature(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
            Text("和 $partnerNickname 正在一起。", style = ClaudeType.TitleMd, color = colors.ink)
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Text(
                "关系不是说断就断的。提交解除后，会有 7 天冷静期，期间任一方都可以反悔。",
                style = ClaudeType.BodySm, color = colors.body
            )
            Spacer(Modifier.height(ClaudeSpacing.md))
            Text(
                "冷静期内：\n• 宠物会冬眠，不再衰减\n• 见面/停留等情境提醒暂停\n• 共建的金库、信件、便签都保留",
                style = ClaudeType.BodySm, color = colors.muted
            )
        }
        Spacer(Modifier.height(ClaudeSpacing.xl))
        // 警示色按钮（描边款，避免显得太凶）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ClaudeRadius.md))
                .border(1.dp, ClaudeColors.Warning, RoundedCornerShape(ClaudeRadius.md))
                .clickable(onClick = onRequest)
                .padding(vertical = ClaudeSpacing.md),
            contentAlignment = Alignment.Center
        ) {
            Text("解除关系", style = ClaudeType.Button, color = ClaudeColors.Warning)
        }
    }
}

@Composable
private fun CooldownState(
    cooldownEndsAt: Long,
    operating: Boolean,
    onCancel: () -> Unit
) {
    val colors = MaterialTheme.appColors
    val now = System.currentTimeMillis()
    val msLeft = (cooldownEndsAt - now).coerceAtLeast(0L)
    val daysLeft = msLeft / (24 * 60 * 60 * 1000L)
    val hoursLeft = (msLeft / (60 * 60 * 1000L)) % 24

    AppCard.Coral(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
        Text("冷静期已开始", style = ClaudeType.TitleMd, color = ClaudeColors.OnPrimary)
        Spacer(Modifier.height(ClaudeSpacing.sm))
        Text(
            "还有 $daysLeft 天 $hoursLeft 小时",
            style = ClaudeType.DisplayLg,
            color = ClaudeColors.OnPrimary
        )
        Spacer(Modifier.height(ClaudeSpacing.sm))
        Text(
            "在这段时间里，你们的宠物在冬眠。任何时候你或 TA 反悔，关系都会回来。",
            style = ClaudeType.BodySm,
            color = ClaudeColors.OnPrimary.copy(alpha = 0.85f)
        )
    }
    Spacer(Modifier.height(ClaudeSpacing.xl))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ClaudeRadius.md))
            .background(ClaudeColors.Primary)
            .clickable(enabled = !operating, onClick = onCancel)
            .padding(vertical = ClaudeSpacing.md),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (operating) "取消中…" else "我想反悔",
            style = ClaudeType.Button,
            color = ClaudeColors.OnPrimary
        )
    }
}
