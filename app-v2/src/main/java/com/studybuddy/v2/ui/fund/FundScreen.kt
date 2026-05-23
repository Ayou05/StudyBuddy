package com.studybuddy.v2.ui.fund

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.data.model.FundTransaction
import com.studybuddy.v2.data.model.SharedFund
import com.studybuddy.v2.data.model.WishItem
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.AppCard
import com.studybuddy.v2.ui.component.BackRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FundScreen(
    onBack: () -> Unit = {},
    embedded: Boolean = false,
    viewModel: FundViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors
    val scroll = rememberScrollState()
    if (!embedded) androidx.activity.compose.BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = ClaudeSpacing.pageHorizontal)
                .padding(top = if (embedded) 0.dp else ClaudeSpacing.lg, bottom = ClaudeSpacing.xxl)
        ) {
            if (!embedded) {
                BackRow(onBack = onBack)
                Spacer(Modifier.height(ClaudeSpacing.md))
                Text("FUND", style = ClaudeType.CaptionUppercase, color = colors.muted)
                Spacer(Modifier.height(ClaudeSpacing.sm))
                Text("公共基金", style = ClaudeType.DisplayLg, color = colors.ink)
                Spacer(Modifier.height(ClaudeSpacing.xs))
            }
            Text("一份共同账本，存放你们的承诺与愿望。", style = ClaudeType.BodyMd, color = colors.muted)

            Spacer(Modifier.height(ClaudeSpacing.xl))

            when {
                !state.hasPartner -> EmptyHint(
                    title = "等搭档绑定",
                    body = "绑定一个搭档之后，公共基金会自动建立。"
                )
                state.fund == null -> EmptyHint(
                    title = "尚未启用",
                    body = "下次专注完成后，公共基金会自动初始化。"
                )
                else -> {
                    BalanceCard(state.fund!!)
                    Spacer(Modifier.height(ClaudeSpacing.lg))
                    if (state.fund!!.wishlist.isNotEmpty()) {
                        WishlistSection(state.fund!!.wishlist)
                        Spacer(Modifier.height(ClaudeSpacing.lg))
                    }
                    if (state.fund!!.transactions.isNotEmpty()) {
                        TransactionsSection(state.fund!!.transactions.takeLast(20).reversed())
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(title: String, body: String) {
    val colors = MaterialTheme.appColors
    AppCard.Outline(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
        Column {
            Text(title, style = ClaudeType.TitleMd, color = colors.ink)
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(body, style = ClaudeType.BodySm, color = colors.muted)
        }
    }
}

@Composable
private fun BalanceCard(fund: SharedFund) {
    val colors = MaterialTheme.appColors
    AppCard.Dark(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
        Column {
            Text("BALANCE", style = ClaudeType.CaptionUppercase, color = colors.onDarkSoft)
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Text(formatYuan(fund.balanceCents), style = ClaudeType.TimerLg, color = colors.onDark)
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "累计存入 ${formatYuan(fund.totalInCents)}",
                    style = ClaudeType.Caption, color = colors.onDarkSoft
                )
                Spacer(Modifier.height(0.dp))
                Text("  ·  ", style = ClaudeType.Caption, color = colors.onDarkSoft)
                Text(
                    "累计支出 ${formatYuan(fund.totalOutCents)}",
                    style = ClaudeType.Caption, color = colors.onDarkSoft
                )
            }
        }
    }
}

@Composable
private fun WishlistSection(wishes: List<WishItem>) {
    val colors = MaterialTheme.appColors
    Text("WISHLIST", style = ClaudeType.CaptionUppercase, color = colors.muted)
    Spacer(Modifier.height(ClaudeSpacing.sm))
    Column(verticalArrangement = Arrangement.spacedBy(ClaudeSpacing.sm)) {
        wishes.forEach { w ->
            AppCard.Feature(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.md) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(w.name, style = ClaudeType.TitleSm, color = colors.ink)
                        Spacer(Modifier.height(0.dp))
                        Text(
                            "  ${formatYuan(w.savedCents)} / ${formatYuan(w.targetCents)}",
                            style = ClaudeType.Caption, color = colors.muted
                        )
                    }
                    Spacer(Modifier.height(ClaudeSpacing.sm))
                    val progress = if (w.targetCents <= 0) 0f
                        else (w.savedCents.toFloat() / w.targetCents).coerceIn(0f, 1f)
                    ProgressTrack(progress = progress, tint = ClaudeColors.Primary)
                }
            }
        }
    }
}

@Composable
private fun TransactionsSection(txs: List<FundTransaction>) {
    val colors = MaterialTheme.appColors
    Text("TRANSACTIONS", style = ClaudeType.CaptionUppercase, color = colors.muted)
    Spacer(Modifier.height(ClaudeSpacing.sm))
    AppCard.Outline(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.md) {
        Column {
            txs.forEachIndexed { idx, tx ->
                if (idx > 0) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.hairlineSoft))
                }
                TransactionRow(tx)
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: FundTransaction) {
    val colors = MaterialTheme.appColors
    val isIn = tx.type == "DEPOSIT"
    val sign = if (isIn) "+" else "−"
    val amountColor = if (isIn) colors.success else colors.warning
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ClaudeSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier) {
            Text(
                tx.note.ifBlank {
                    when (tx.type) {
                        "DEPOSIT" -> "存入"
                        "WITHDRAWAL" -> "支取"
                        "PENALTY" -> "断签罚金"
                        else -> tx.type
                    }
                },
                style = ClaudeType.BodySm, color = colors.ink
            )
            Spacer(Modifier.height(2.dp))
            Text(formatTime(tx.at), style = ClaudeType.Caption, color = colors.muted)
        }
        Spacer(Modifier.height(0.dp))
        Text(
            "$sign ${formatYuan(tx.amountCents)}",
            style = ClaudeType.TitleSm, color = amountColor,
            modifier = Modifier
        )
    }
}

@Composable
private fun ProgressTrack(progress: Float, tint: Color) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(colors.hairlineSoft)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(tint)
        )
    }
}

private fun formatYuan(cents: Long): String {
    val yuan = cents / 100.0
    return "¥%.2f".format(yuan)
}
private fun formatTime(t: Long): String {
    if (t <= 0) return ""
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(t))
}
