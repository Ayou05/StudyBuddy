package com.studybuddy.v2.ui.settings

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.AppCard
import com.studybuddy.v2.ui.component.AppIcon
import com.studybuddy.v2.ui.component.BackRow

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onOpenUnbind: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
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
        // 顶部返回行
        BackRow(onBack = onBack)
        Spacer(Modifier.height(ClaudeSpacing.md))

        Text("SETTINGS", style = ClaudeType.CaptionUppercase, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.sm))
        Text("设置", style = ClaudeType.DisplayLg, color = colors.ink)
        Spacer(Modifier.height(ClaudeSpacing.xs))
        Text("把它调成你的样子。", style = ClaudeType.BodyMd, color = colors.muted)

        // ─── App 模式 ────────────────────────────────────────
        Spacer(Modifier.height(ClaudeSpacing.xl))
        SectionTitle("MODE")
        AppCard.Feature(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
            Column {
                LabeledChips(
                    label = "App 当下的状态",
                    options = listOf("FOCUS" to "专注", "LEISURE" to "娱乐"),
                    selectedKey = state.appMode,
                    onPick = viewModel::setAppMode
                )
                Spacer(Modifier.height(ClaudeSpacing.sm))
                Text(
                    if (state.appMode == "LEISURE")
                        "娱乐模式：宠物不衰减；TA 开始专注 / 工作日断连这类提醒静默；其它陪伴功能照常。"
                    else
                        "专注模式：宠物会按时衰减；专注向情境提醒会推送。",
                    style = ClaudeType.Caption,
                    color = colors.muted
                )
            }
        }

        // ─── 专注偏好 ───────────────────────────────────────
        Spacer(Modifier.height(ClaudeSpacing.xl))
        SectionTitle("FOCUS")
        AppCard.Feature(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
            Column {
                LabeledChips(
                    label = "默认模式",
                    options = listOf("COUNTDOWN" to "倒计时", "STOPWATCH" to "正计时"),
                    selectedKey = state.focusMode,
                    onPick = viewModel::setFocusMode
                )
                if (state.focusMode == "COUNTDOWN") {
                    Spacer(Modifier.height(ClaudeSpacing.md))
                    LabeledChips(
                        label = "默认时长（分钟）",
                        options = listOf(15, 25, 45, 90).map { it.toString() to it.toString() },
                        selectedKey = state.focusDurationMin.toString(),
                        onPick = { viewModel.setFocusDuration(it.toInt()) }
                    )
                }
            }
        }

        // ─── 外观 ────────────────────────────────────────────
        Spacer(Modifier.height(ClaudeSpacing.xl))
        SectionTitle("APPEARANCE")
        AppCard.Feature(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
            Column {
                LabeledChips(
                    label = "主页双人卡样式",
                    options = listOf("WAVE" to "波形", "TYPOGRAPHY" to "字号"),
                    selectedKey = state.partnerWidgetStyle,
                    onPick = viewModel::setPartnerWidget
                )
                Spacer(Modifier.height(ClaudeSpacing.md))
                LabeledChips(
                    label = "主题",
                    options = listOf("false" to "浅色", "true" to "深色"),
                    selectedKey = state.darkTheme.toString(),
                    onPick = { viewModel.setDark(it.toBoolean()) }
                )
            }
        }

        // ─── 账本 ────────────────────────────────────────────
        Spacer(Modifier.height(ClaudeSpacing.xl))
        SectionTitle("LEDGER")
        AppCard.Feature(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
            LabeledChips(
                label = "每份金额",
                options = listOf(
                    "300" to "¥3", "500" to "¥5", "1000" to "¥10",
                    "2000" to "¥20", "5000" to "¥50"
                ),
                selectedKey = state.ledgerUnitCents.toString(),
                onPick = { viewModel.setLedgerUnitCents(it.toInt()) }
            )
        }

        // ─── 情境提醒 ────────────────────────────────────────
        Spacer(Modifier.height(ClaudeSpacing.xl))
        SectionTitle("MOMENTS")
        AppCard.Feature(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
            Column {
                Text(
                    "情境浮卡 —— 关掉就再也不提醒。",
                    style = ClaudeType.Caption,
                    color = colors.muted
                )
                Spacer(Modifier.height(ClaudeSpacing.sm))
                listOf(
                    "meeting_started" to "见面开始",
                    "meeting_ended" to "见面结束",
                    "stay_detected" to "共同停留回家后",
                    "partner_started_focus" to "TA 开始专注",
                    "weekday_break_noticed" to "工作日断连"
                ).forEach { (type, label) ->
                    MomentToggleRow(
                        label = label,
                        enabled = type !in state.momentDisabled,
                        onChange = { on -> viewModel.toggleMomentType(type, on) }
                    )
                }
            }
        }

        // ─── 关系 ────────────────────────────────────────────
        Spacer(Modifier.height(ClaudeSpacing.xl))
        SectionTitle("RELATIONSHIP")
        AppCard.Feature(
            modifier = Modifier.fillMaxWidth().clickable { onOpenUnbind() },
            padding = ClaudeSpacing.lg
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("解除搭档关系", style = ClaudeType.BodySm, color = colors.body)
                    Spacer(Modifier.height(2.dp))
                    Text("7 天冷静期，期间可反悔", style = ClaudeType.Caption, color = colors.muted)
                }
                Text("›", style = ClaudeType.TitleLg, color = colors.muted)
            }
        }

        // ─── 关于 ────────────────────────────────────────────
        Spacer(Modifier.height(ClaudeSpacing.xl))
        SectionTitle("ABOUT")
        AppCard.Feature(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.tapAbout() },
            padding = ClaudeSpacing.lg
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("版本", style = ClaudeType.BodySm, color = colors.body)
                Spacer(Modifier.weight(1f))
                Text("v2.0.0", style = ClaudeType.BodySm, color = colors.muted)
            }
        }
    }

    if (state.showEasterEggInput) {
        EasterEggDialog(
            error = state.easterEggError,
            onDismiss = viewModel::dismissEasterEggInput,
            onSubmit = viewModel::submitEasterEggCode
        )
    }
}

@Composable
private fun EasterEggDialog(
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val colors = MaterialTheme.appColors
    val inputState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("> 输入口令", style = ClaudeType.Code, color = colors.ink) },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = inputState.value,
                    onValueChange = { inputState.value = it },
                    singleLine = true,
                    placeholder = { Text("cc-pet-...", style = ClaudeType.Code, color = colors.muted) },
                    textStyle = ClaudeType.Code
                )
                if (error != null) {
                    Spacer(Modifier.height(ClaudeSpacing.xs))
                    Text(error, style = ClaudeType.Caption, color = ClaudeColors.Warning)
                }
            }
        },
        confirmButton = {
            com.studybuddy.v2.ui.component.AppButton.Text("解锁", { onSubmit(inputState.value) })
        },
        dismissButton = {
            com.studybuddy.v2.ui.component.AppButton.Text("取消", onDismiss)
        },
        containerColor = colors.canvas,
        shape = RoundedCornerShape(ClaudeRadius.lg)
    )
}

@Composable
private fun SectionTitle(text: String) {
    val colors = MaterialTheme.appColors
    Text(text, style = ClaudeType.CaptionUppercase, color = colors.muted)
    Spacer(Modifier.height(ClaudeSpacing.sm))
}

@Composable
private fun LabeledChips(
    label: String,
    options: List<Pair<String, String>>,   // key to label
    selectedKey: String,
    onPick: (String) -> Unit
) {
    val colors = MaterialTheme.appColors
    Column {
        Text(label, style = ClaudeType.Caption, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ClaudeSpacing.xs)
        ) {
            options.forEach { (k, label) ->
                val selected = k == selectedKey
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(ClaudeRadius.md))
                        .background(if (selected) ClaudeColors.Primary else Color.Transparent)
                        .border(
                            1.dp,
                            if (selected) ClaudeColors.Primary else colors.hairline,
                            RoundedCornerShape(ClaudeRadius.md)
                        )
                        .clickable { onPick(k) }
                        .padding(horizontal = ClaudeSpacing.md, vertical = ClaudeSpacing.sm)
                ) {
                    Text(
                        label,
                        style = ClaudeType.Button,
                        color = if (selected) ClaudeColors.OnPrimary else colors.ink
                    )
                }
            }
        }
    }
}

@Composable
private fun MomentToggleRow(
    label: String,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!enabled) }
            .padding(vertical = ClaudeSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = ClaudeType.BodySm, color = colors.body)
        Spacer(Modifier.weight(1f))
        // 简易 toggle：32x18 圆角，激活态 coral，未激活 hairline
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (enabled) ClaudeColors.Primary else colors.hairline)
                .clickable { onChange(!enabled) }
        ) {
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .width(16.dp)
                    .height(16.dp)
                    .clip(CircleShape)
                    .background(ClaudeColors.OnPrimary)
                    .align(if (enabled) Alignment.CenterEnd else Alignment.CenterStart)
            )
        }
    }
}
