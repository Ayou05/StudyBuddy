package com.studybuddy.v2.ui.vault

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.data.model.FundTransaction
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 共同金库 —— 替代账本 + 基金。
 *
 * 显示：
 * - 顶部余额大字（cream 卡）
 * - "加一笔"按钮
 * - 流水列表（左滑 → 核销 / 删除）
 *
 * 流水类型：
 * - PENALTY 工作日断连自动入金（coral 标记）
 * - DEPOSIT 主动充值（中性）
 * - WITHDRAWAL 用于愿望（带负号）
 *
 * voided=true 的流水变灰显示，不计入余额。
 */
@Composable
fun VaultScreen(viewModel: VaultViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(horizontal = ClaudeSpacing.pageHorizontal)
            .padding(top = ClaudeSpacing.lg, bottom = ClaudeSpacing.lg)
    ) {
        if (!state.hasPartner) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("绑定搭档后金库会出现", style = ClaudeType.BodyMd, color = colors.muted)
            }
        } else {
            // 余额卡
            BalanceCard(
                balanceCents = state.fund?.balanceCents ?: 0,
                totalIn = state.fund?.totalInCents ?: 0,
                totalOut = state.fund?.totalOutCents ?: 0,
                onAdd = viewModel::openAdd
            )

            Spacer(Modifier.height(ClaudeSpacing.lg))

            Text("流水", style = ClaudeType.CaptionUppercase, color = colors.muted)
            Spacer(Modifier.height(ClaudeSpacing.sm))

            if (state.transactions.isEmpty() && !state.loading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(top = ClaudeSpacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    Text("还没有流水", style = ClaudeType.BodyMd, color = colors.muted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(ClaudeSpacing.xs)
                ) {
                    items(state.transactions, key = { it.id }) { tx ->
                        SwipeableTxRow(
                            tx = tx,
                            onVoid = { viewModel.voidTransaction(tx) },
                            onDelete = { viewModel.deleteTransaction(tx) }
                        )
                    }
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddDepositDialog(
            onDismiss = viewModel::closeAdd,
            onConfirm = { amountCents, note ->
                viewModel.deposit(amountCents, note)
            }
        )
    }
}

@Composable
private fun BalanceCard(
    balanceCents: Long,
    totalIn: Long,
    totalOut: Long,
    onAdd: () -> Unit
) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ClaudeRadius.lg))
            .background(colors.surfaceCard)
            .padding(ClaudeSpacing.lg)
    ) {
        Column {
            Text("余额", style = ClaudeType.Caption, color = colors.muted)
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(
                "¥${balanceCents / 100}",
                style = ClaudeType.DisplayLg,
                color = colors.ink
            )
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(
                "已存 ¥${totalIn / 100} · 已花 ¥${totalOut / 100}",
                style = ClaudeType.Caption,
                color = colors.muted
            )
            Spacer(Modifier.height(ClaudeSpacing.md))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(ClaudeRadius.pill))
                    .background(ClaudeColors.Primary)
                    .clickable(onClick = onAdd)
                    .padding(horizontal = ClaudeSpacing.lg, vertical = ClaudeSpacing.xs)
            ) {
                Text("加一笔", style = ClaudeType.Button, color = ClaudeColors.OnPrimary)
            }
        }
    }
}

/** 长按弹菜单（核销 / 删除）—— 避免左滑跟外层 HorizontalPager 冲突 */
@Composable
private fun SwipeableTxRow(
    tx: FundTransaction,
    onVoid: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.appColors
    val showMenu = remember { mutableStateOf(false) }
    val showVoidConfirm = remember { mutableStateOf(false) }
    val showDeleteConfirm = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ClaudeRadius.md))
            .background(colors.surfaceCard)
            .pointerInput(tx.id) {
                detectTapGestures(
                    onLongPress = {
                        if (!tx.voided) showMenu.value = true
                    }
                )
            }
    ) {
        TxContent(tx)
    }

    if (showMenu.value) {
        AlertDialog(
            onDismissRequest = { showMenu.value = false },
            containerColor = colors.surfaceCard,
            title = { Text("怎么处理这笔？", style = ClaudeType.TitleMd, color = colors.ink) },
            text = {
                Column {
                    Text("核销 = 不计入余额，但留下记录", style = ClaudeType.Caption, color = colors.muted)
                    Spacer(Modifier.height(4.dp))
                    Text("删除 = 彻底清掉，余额反向调整", style = ClaudeType.Caption, color = colors.muted)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showMenu.value = false
                    showVoidConfirm.value = true
                }) { Text("核销", color = ClaudeColors.Primary) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showMenu.value = false
                        showDeleteConfirm.value = true
                    }) { Text("删除", color = ClaudeColors.Warning) }
                    Spacer(Modifier.width(ClaudeSpacing.xs))
                    TextButton(onClick = { showMenu.value = false }) {
                        Text("取消", color = colors.muted)
                    }
                }
            }
        )
    }

    if (showVoidConfirm.value) {
        AlertDialog(
            onDismissRequest = { showVoidConfirm.value = false },
            containerColor = colors.surfaceCard,
            title = { Text("核销这笔？", style = ClaudeType.TitleMd, color = colors.ink) },
            text = { Text("不再计入余额，但留下记录。", style = ClaudeType.BodySm, color = colors.body) },
            confirmButton = {
                TextButton(onClick = { showVoidConfirm.value = false; onVoid() }) {
                    Text("核销", color = ClaudeColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoidConfirm.value = false }) {
                    Text("取消", color = colors.muted)
                }
            }
        )
    }

    if (showDeleteConfirm.value) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm.value = false },
            containerColor = colors.surfaceCard,
            title = { Text("删除这笔？", style = ClaudeType.TitleMd, color = colors.ink) },
            text = { Text("彻底删掉记录，余额会反向调整。", style = ClaudeType.BodySm, color = colors.body) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm.value = false; onDelete() }) {
                    Text("删除", color = ClaudeColors.Warning)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm.value = false }) {
                    Text("取消", color = colors.muted)
                }
            }
        )
    }
}

@Composable
private fun TxContent(tx: FundTransaction) {
    val colors = MaterialTheme.appColors
    val df = remember { SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) }
    val (label, amountStr, accent) = when (tx.type) {
        "PENALTY" -> Triple("断连", "+¥${tx.amountCents / 100}", ClaudeColors.Primary)
        "DEPOSIT" -> Triple("存入", "+¥${tx.amountCents / 100}", colors.success)
        "WITHDRAWAL" -> Triple("支出", "-¥${tx.amountCents / 100}", ClaudeColors.AccentAmber)
        else -> Triple(tx.type, "¥${tx.amountCents / 100}", colors.muted)
    }
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ClaudeSpacing.md, vertical = ClaudeSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(ClaudeRadius.xs))
                .background(if (tx.voided) colors.hairlineSoft else accent.copy(alpha = 0.12f))
                .padding(horizontal = ClaudeSpacing.sm, vertical = ClaudeSpacing.xxs)
        ) {
            Text(
                if (tx.voided) "已核销" else label,
                style = ClaudeType.Caption,
                color = if (tx.voided) colors.muted else accent
            )
        }
        Spacer(Modifier.width(ClaudeSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                tx.note.ifBlank { "—" },
                style = ClaudeType.BodySm,
                color = if (tx.voided) colors.muted else colors.ink
            )
            Spacer(Modifier.height(2.dp))
            Text(
                df.format(Date(tx.at)),
                style = ClaudeType.Caption,
                color = colors.muted
            )
        }
        Text(
            amountStr,
            style = ClaudeType.TitleSm,
            color = if (tx.voided) colors.muted else colors.ink
        )
    }
}

@Composable
private fun AddDepositDialog(
    onDismiss: () -> Unit,
    onConfirm: (amountCents: Long, note: String) -> Unit
) {
    val colors = MaterialTheme.appColors
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceCard,
        title = { Text("加一笔", style = ClaudeType.TitleLg, color = colors.ink) },
        text = {
            Column {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("金额（元）") },
                    singleLine = true
                )
                Spacer(Modifier.height(ClaudeSpacing.sm))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(40) },
                    label = { Text("备注") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            val amount = amountStr.toLongOrNull() ?: 0L
            TextButton(
                onClick = { onConfirm(amount * 100, note.trim()) },
                enabled = amount > 0
            ) { Text("加上", color = ClaudeColors.Primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = colors.muted) }
        }
    )
}
