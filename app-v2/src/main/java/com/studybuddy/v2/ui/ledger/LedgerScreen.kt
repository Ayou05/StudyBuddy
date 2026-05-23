package com.studybuddy.v2.ui.ledger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.data.model.Debt
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.AppButton
import com.studybuddy.v2.ui.component.AppCard
import com.studybuddy.v2.ui.component.BackRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 账本 —— 双人见证的"欠条"。App 不沾资金，结算线下。
 *
 * 头部：双向余额（我欠 N · TA 欠 M），两个数字
 * 列表：每笔欠条 + 是谁欠谁 + 原因 + 日期 + "已结算"标记
 * 仅被欠方可点"收到了"勾销
 */
@Composable
fun LedgerScreen(
    onBack: () -> Unit = {},
    embedded: Boolean = false,
    viewModel: LedgerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors
    if (!embedded) BackHandler { onBack() }
    LaunchedEffect(Unit) { viewModel.load() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ClaudeSpacing.pageHorizontal)
                .padding(top = if (embedded) 0.dp else ClaudeSpacing.lg, bottom = ClaudeSpacing.xxl)
        ) {
            if (!embedded) {
                BackRow(onBack = onBack)
                Spacer(Modifier.height(ClaudeSpacing.md))
                Text("LEDGER", style = ClaudeType.CaptionUppercase, color = colors.muted)
                Spacer(Modifier.height(ClaudeSpacing.sm))
                Text("账本", style = ClaudeType.DisplayLg, color = colors.ink)
                Spacer(Modifier.height(ClaudeSpacing.xs))
            }
            Text(
                "工作日断连欠对方一份。每份 ¥${state.unitCents / 100}。结算线下进行。",
                style = ClaudeType.BodyMd, color = colors.muted
            )

            Spacer(Modifier.height(ClaudeSpacing.xl))

            // 双向余额卡
            BalanceCard(state)

            Spacer(Modifier.height(ClaudeSpacing.xl))

            Text("流水", style = ClaudeType.CaptionUppercase, color = colors.muted)
            Spacer(Modifier.height(ClaudeSpacing.sm))

            if (state.debts.isEmpty() && !state.loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ClaudeSpacing.xxl),
                    contentAlignment = Alignment.Center
                ) {
                    Text("还没有欠条。", style = ClaudeType.BodyMd, color = colors.muted)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.debts, key = { it.id }) { d ->
                        DebtRow(
                            d = d,
                            myUserId = state.myUserId,
                            partnerNickname = state.partnerNickname,
                            onSettle = { viewModel.settle(d) },
                            onDelete = { viewModel.deleteDebt(d) },
                            onVoid = { viewModel.voidDebt(d) }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.hairlineSoft)
                        )
                    }
                }
            }
        }

        // 右下角写入按钮
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(ClaudeSpacing.lg)
                .clip(CircleShape)
                .background(ClaudeColors.Primary)
                .clickable { viewModel.openCompose() }
                .padding(ClaudeSpacing.md)
        ) {
            Text("＋", style = ClaudeType.DisplayLg, color = ClaudeColors.OnPrimary)
        }

        if (state.composing) {
            ComposeDialog(
                partnerNickname = state.partnerNickname,
                unitCents = state.unitCents,
                onDismiss = viewModel::closeCompose,
                onSubmit = viewModel::submit
            )
        }
    }
}

@Composable
private fun BalanceCard(state: LedgerUiState) {
    val colors = MaterialTheme.appColors
    AppCard.Feature(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 我欠
            Column(modifier = Modifier.weight(1f)) {
                Text("我欠 ${state.partnerNickname}", style = ClaudeType.Caption, color = colors.muted)
                Spacer(Modifier.height(ClaudeSpacing.xxs))
                Text(
                    "${state.owedByMe} 份",
                    style = ClaudeType.DisplayMd,
                    color = if (state.owedByMe > 0) ClaudeColors.Warning else colors.ink
                )
                Text(
                    "≈ ¥${state.owedByMe * state.unitCents / 100}",
                    style = ClaudeType.Caption, color = colors.muted
                )
            }
            // 中线
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(64.dp)
                    .background(colors.hairline)
            )
            Spacer(Modifier.width(ClaudeSpacing.md))
            // TA 欠
            Column(modifier = Modifier.weight(1f)) {
                Text("${state.partnerNickname} 欠我", style = ClaudeType.Caption, color = colors.muted)
                Spacer(Modifier.height(ClaudeSpacing.xxs))
                Text(
                    "${state.owedByPartner} 份",
                    style = ClaudeType.DisplayMd,
                    color = if (state.owedByPartner > 0) ClaudeColors.Success else colors.ink
                )
                Text(
                    "≈ ¥${state.owedByPartner * state.unitCents / 100}",
                    style = ClaudeType.Caption, color = colors.muted
                )
            }
        }
    }
}

@Composable
private fun DebtRow(
    d: Debt,
    myUserId: String,
    partnerNickname: String,
    onSettle: () -> Unit,
    onDelete: () -> Unit,
    onVoid: () -> Unit
) {
    val colors = MaterialTheme.appColors
    val df = remember { SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) }
    val iOwe = d.fromUserId == myUserId
    val canSettle = !d.settled && d.toUserId == myUserId  // 仅被欠方可点

    val showMenu = remember { androidx.compose.runtime.mutableStateOf(false) }
    val showDeleteConfirm = remember { androidx.compose.runtime.mutableStateOf(false) }
    val showVoidConfirm = remember { androidx.compose.runtime.mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(d.id) {
                detectTapGestures(
                    onLongPress = { if (!d.settled) showMenu.value = true }
                )
            }
            .padding(vertical = ClaudeSpacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 方向标记
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(ClaudeRadius.xs))
                    .background(
                        if (d.settled) colors.hairlineSoft
                        else if (iOwe) ClaudeColors.Warning.copy(alpha = 0.12f)
                        else ClaudeColors.Success.copy(alpha = 0.12f)
                    )
                    .padding(horizontal = ClaudeSpacing.sm, vertical = ClaudeSpacing.xxs)
            ) {
                Text(
                    if (d.settled) "已结算"
                    else if (iOwe) "我欠 $partnerNickname"
                    else "$partnerNickname 欠我",
                    style = ClaudeType.Caption,
                    color = if (d.settled) colors.muted
                            else if (iOwe) ClaudeColors.Warning
                            else ClaudeColors.Success
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "${d.count} 份 · ¥${d.count * d.unitCents / 100}",
                style = ClaudeType.TitleSm,
                color = if (d.settled) colors.muted else colors.ink
            )
        }
        if (d.reason.isNotBlank()) {
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(d.reason, style = ClaudeType.BodySm, color = colors.body)
        }
        Spacer(Modifier.height(ClaudeSpacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(df.format(Date(d.createdAt)), style = ClaudeType.Caption, color = colors.muted)
            Spacer(Modifier.weight(1f))
            if (canSettle) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(ClaudeRadius.sm))
                        .border(1.dp, ClaudeColors.Primary, RoundedCornerShape(ClaudeRadius.sm))
                        .clickable { onSettle() }
                        .padding(horizontal = ClaudeSpacing.md, vertical = ClaudeSpacing.xs)
                ) {
                    Text("收到了", style = ClaudeType.Caption, color = ClaudeColors.Primary)
                }
            } else if (d.settled && d.settledAt != null) {
                Text(
                    "${df.format(Date(d.settledAt))} 已勾销",
                    style = ClaudeType.Caption, color = colors.muted
                )
            }
        }
    }

    // 长按菜单（核销 / 删除）
    if (showMenu.value) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showMenu.value = false },
            containerColor = colors.surfaceCard,
            title = { Text("怎么处理这笔？", style = ClaudeType.TitleMd, color = colors.ink) },
            text = {
                Column {
                    Text("核销 = 双方同意算了，但记录留着。", style = ClaudeType.Caption, color = colors.muted)
                    Spacer(Modifier.height(4.dp))
                    Text("删除 = 当作没发生过，记录消失。", style = ClaudeType.Caption, color = colors.muted)
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showMenu.value = false
                    showVoidConfirm.value = true
                }) {
                    Text("核销", color = ClaudeColors.Primary)
                }
            },
            dismissButton = {
                Row {
                    androidx.compose.material3.TextButton(onClick = {
                        showMenu.value = false
                        showDeleteConfirm.value = true
                    }) {
                        Text("删除", color = ClaudeColors.Warning)
                    }
                    Spacer(Modifier.size(ClaudeSpacing.xs))
                    androidx.compose.material3.TextButton(onClick = { showMenu.value = false }) {
                        Text("取消", color = colors.muted)
                    }
                }
            }
        )
    }

    if (showVoidConfirm.value) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showVoidConfirm.value = false },
            containerColor = colors.surfaceCard,
            title = { Text("确认核销？", style = ClaudeType.TitleMd, color = colors.ink) },
            text = {
                Text("这笔会标为'已核销'，留下记录但不再追究。",
                    style = ClaudeType.BodySm, color = colors.body)
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showVoidConfirm.value = false
                    onVoid()
                }) {
                    Text("确认", color = ClaudeColors.Primary)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showVoidConfirm.value = false }) {
                    Text("取消", color = colors.muted)
                }
            }
        )
    }

    if (showDeleteConfirm.value) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm.value = false },
            containerColor = colors.surfaceCard,
            title = { Text("确认删除？", style = ClaudeType.TitleMd, color = colors.ink) },
            text = {
                Text("彻底删掉这笔记录，无法恢复。",
                    style = ClaudeType.BodySm, color = colors.body)
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDeleteConfirm.value = false
                    onDelete()
                }) {
                    Text("删除", color = ClaudeColors.Warning)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteConfirm.value = false }) {
                    Text("取消", color = colors.muted)
                }
            }
        )
    }
}

@Composable
private fun ComposeDialog(
    partnerNickname: String,
    unitCents: Int,
    onDismiss: () -> Unit,
    onSubmit: (direction: String, count: Int, reason: String) -> Unit
) {
    val colors = MaterialTheme.appColors
    var direction by remember { mutableStateOf("I_OWE") }
    var count by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("写一笔", style = ClaudeType.TitleMd, color = colors.ink) },
        text = {
            Column {
                // 方向
                Row(modifier = Modifier.fillMaxWidth()) {
                    DirectionChip("我欠 $partnerNickname", direction == "I_OWE", { direction = "I_OWE" }, Modifier.weight(1f))
                    Spacer(Modifier.width(ClaudeSpacing.xs))
                    DirectionChip("$partnerNickname 欠我", direction == "PARTNER_OWES", { direction = "PARTNER_OWES" }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(ClaudeSpacing.md))
                OutlinedTextField(
                    value = count,
                    onValueChange = { count = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("份数（每份 ¥${unitCents / 100}）") },
                    singleLine = true,
                    textStyle = ClaudeType.BodyMd,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(ClaudeSpacing.sm))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("原因（可空）") },
                    singleLine = true,
                    textStyle = ClaudeType.BodySm,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppButton.Text(
                text = "记上",
                onClick = {
                    onSubmit(direction, count.toIntOrNull() ?: 1, reason.trim())
                }
            )
        },
        dismissButton = { AppButton.Text(text = "取消", onClick = onDismiss) },
        containerColor = colors.canvas
    )
}

@Composable
private fun DirectionChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(ClaudeRadius.md))
            .background(if (selected) ClaudeColors.Primary.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                1.dp,
                if (selected) ClaudeColors.Primary else colors.hairline,
                RoundedCornerShape(ClaudeRadius.md)
            )
            .clickable { onClick() }
            .padding(horizontal = ClaudeSpacing.sm, vertical = ClaudeSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = ClaudeType.Caption,
            color = if (selected) ClaudeColors.Primary else colors.body
        )
    }
}
