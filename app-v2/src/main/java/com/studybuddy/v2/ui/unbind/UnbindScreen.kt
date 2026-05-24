package com.studybuddy.v2.ui.unbind

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
 * 解除搭档关系页面。
 *
 * 两态：
 * 1. 没绑搭档 → 显示"还没有搭档"占位
 * 2. 已绑 → 显示说明 + "解除关系"按钮，点击弹二次确认，确认后立即解绑
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
            state.done -> DoneState()
            !state.hasPartner -> NoPartnerState()
            else -> NormalState(
                partnerNickname = state.partnerNickname.ifBlank { "TA" },
                operating = state.operating,
                onRequest = viewModel::showConfirm
            )
        }
    }

    if (state.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissConfirm,
            containerColor = colors.surfaceCard,
            title = { Text("确认解除关系？", style = ClaudeType.TitleLg, color = colors.ink) },
            text = {
                Text(
                    "解除后，你们的搭档关系会立即结束。共建的金库、信件、便签都会保留，但不再同步。",
                    style = ClaudeType.BodySm, color = colors.body
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmUnbind) {
                    Text("确认解除", color = ClaudeColors.Warning)
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
        Column {
            Text("还没有搭档。", style = ClaudeType.TitleMd, color = colors.ink)
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(
                "在主页绑定搭档后，这里可以解除关系。",
                style = ClaudeType.BodySm, color = colors.muted
            )
        }
    }
}

@Composable
private fun NormalState(
    partnerNickname: String,
    operating: Boolean,
    onRequest: () -> Unit
) {
    val colors = MaterialTheme.appColors
    Column {
        AppCard.Feature(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
            Column {
                Text("和 $partnerNickname 正在一起。", style = ClaudeType.TitleMd, color = colors.ink)
                Spacer(Modifier.height(ClaudeSpacing.sm))
                Text(
                    "解除关系后，搭档绑定会立即结束。共建的金库、信件、便签都会保留，但不再同步更新。",
                    style = ClaudeType.BodySm, color = colors.body
                )
            }
        }
        Spacer(Modifier.height(ClaudeSpacing.xl))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ClaudeRadius.md))
                .border(1.dp, ClaudeColors.Warning, RoundedCornerShape(ClaudeRadius.md))
                .clickable(enabled = !operating, onClick = onRequest)
                .padding(vertical = ClaudeSpacing.md),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (operating) "解除中…" else "解除关系",
                style = ClaudeType.Button,
                color = ClaudeColors.Warning
            )
        }
    }
}

@Composable
private fun DoneState() {
    val colors = MaterialTheme.appColors
    AppCard.Feature(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
        Column {
            Text("已解除。", style = ClaudeType.TitleMd, color = colors.ink)
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(
                "你们的搭档关系已结束。共建的数据都保留着。",
                style = ClaudeType.BodySm, color = colors.muted
            )
        }
    }
}
