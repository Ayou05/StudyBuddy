package com.studybuddy.v2.ui.letter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.data.model.Letter
import com.studybuddy.v2.data.model.isRead
import com.studybuddy.v2.data.model.kindEnum
import com.studybuddy.v2.data.repo.LetterRepo
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.LetterSerifBody
import com.studybuddy.v2.theme.LetterSerifMarker
import com.studybuddy.v2.theme.appColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 信件 + 飞机 tab。
 *
 * 结构：
 * - 顶部 SubTabs：飞机 / 信件
 * - 主体 LazyColumn：letters 时间倒序
 * - 底部输入区：飞机 30 字 + 寄出按钮 / 信件多行 + 寄出按钮
 * - 寄出动画浮层：飞机 PlaneFlyAnimation（信件以滑出 + 卷起代替）
 */
@Composable
fun LetterScreen(viewModel: LetterViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors

    Box(modifier = Modifier.fillMaxSize().background(colors.canvas)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ClaudeSpacing.pageHorizontal)
                .padding(top = ClaudeSpacing.lg, bottom = ClaudeSpacing.lg)
        ) {
            // 顶栏
            Text("LETTER", style = ClaudeType.CaptionUppercase, color = colors.muted)
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text("写给 ${state.partnerName}", style = ClaudeType.DisplayMd, color = colors.ink)
            Spacer(Modifier.height(ClaudeSpacing.lg))

            // 不再有 sub-tab —— 信件统一为一种载体，长短随意
            when {
                !state.hasPartner -> {
                    EmptyStateNoPartner()
                }
                else -> {
                    // 主体 — letters 列表
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(ClaudeSpacing.md),
                        reverseLayout = false
                    ) {
                        items(state.letters, key = { it.id }) { letter ->
                            LetterRow(
                                letter = letter,
                                isMine = letter.authorId == state.myUserId,
                                partnerName = state.partnerName
                            )
                        }
                    }

                    Spacer(Modifier.height(ClaudeSpacing.md))

                    // 输入区
                    InputArea(
                        tab = state.tab,
                        draft = state.draft,
                        onDraftChange = viewModel::updateDraft,
                        onSend = viewModel::send,
                        sending = state.sending
                    )
                }
            }
        }
        // 信件寄出动画 —— 删除原 PlaneFlyAnimation（纸飞机已不暴露）
    }
}

@Composable
private fun SubTabRow(current: LetterTab, onChange: (LetterTab) -> Unit) {
    val colors = MaterialTheme.appColors
    Row(horizontalArrangement = Arrangement.spacedBy(ClaudeSpacing.xs)) {
        SubTabChip(label = "飞机 ✈", selected = current == LetterTab.PLANE,
            onClick = { onChange(LetterTab.PLANE) })
        SubTabChip(label = "信件 ✉", selected = current == LetterTab.LETTER,
            onClick = { onChange(LetterTab.LETTER) })
    }
}

@Composable
private fun SubTabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(ClaudeRadius.pill))
            .background(
                if (selected) ClaudeColors.Primary.copy(alpha = 0.10f)
                else colors.canvas
            )
            .border(
                width = 1.dp,
                color = if (selected) ClaudeColors.Primary else colors.hairline,
                shape = RoundedCornerShape(ClaudeRadius.pill)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = ClaudeSpacing.lg, vertical = ClaudeSpacing.xs)
    ) {
        Text(
            label,
            style = ClaudeType.Button,
            color = if (selected) ClaudeColors.Primary else colors.body
        )
    }
}

@Composable
private fun LetterRow(
    letter: Letter,
    isMine: Boolean,
    partnerName: String
) {
    val colors = MaterialTheme.appColors
    val isPlane = letter.kindEnum().name == "PLANE"
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(letter.createdAt))
    val marker = letterTimeMarker(letter.createdAt)

    Column(
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 时段标
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(marker, style = LetterSerifMarker, color = colors.muted)
            Spacer(Modifier.fillMaxWidth(0.02f).height(0.dp))
            Text("· $time", style = ClaudeType.Caption, color = colors.muted)
            if (isPlane) {
                Spacer(Modifier.fillMaxWidth(0.02f).height(0.dp))
                Text("· ✈", style = ClaudeType.Caption, color = ClaudeColors.Primary)
            }
        }
        Spacer(Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .heightIn(max = if (isPlane) 80.dp else 600.dp)
                .clip(RoundedCornerShape(ClaudeRadius.md))
                .background(
                    if (isMine) ClaudeColors.Primary.copy(alpha = 0.06f)
                    else colors.surfaceCard
                )
                .padding(horizontal = ClaudeSpacing.md, vertical = ClaudeSpacing.sm)
        ) {
            Text(
                letter.text,
                style = LetterSerifBody,
                color = colors.ink
            )
        }
        if (!isMine && !letter.isRead()) {
            Spacer(Modifier.height(2.dp))
            Text("$partnerName 寄来的", style = ClaudeType.Caption, color = colors.muted)
        }
    }
}

@Composable
private fun InputArea(
    tab: LetterTab,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    sending: Boolean
) {
    val colors = MaterialTheme.appColors
    val maxLines = if (tab == LetterTab.PLANE) 1 else 6
    val placeholder = if (tab == LetterTab.PLANE) "30 字以内 · 一句信号" else "慢慢写吧。"
    val sendLabel = if (tab == LetterTab.PLANE) "折出去" else "寄出"
    val charCounter = if (tab == LetterTab.PLANE)
        "${draft.length} / ${LetterRepo.PLANE_MAX_CHARS}" else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ClaudeRadius.lg))
            .background(colors.surfaceCard)
            .padding(ClaudeSpacing.md)
    ) {
        BasicTextField(
            value = draft,
            onValueChange = onDraftChange,
            textStyle = LetterSerifBody.copy(color = colors.ink),
            cursorBrush = SolidColor(ClaudeColors.Primary),
            maxLines = maxLines,
            singleLine = tab == LetterTab.PLANE,
            modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp),
            decorationBox = { inner ->
                if (draft.isEmpty()) {
                    Text(placeholder, style = LetterSerifBody, color = colors.muted)
                }
                inner()
            }
        )
        Spacer(Modifier.height(ClaudeSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            charCounter?.let {
                Text(it, style = ClaudeType.Caption, color = colors.muted)
            }
            Spacer(Modifier.weight(1f))
            val canSend = draft.trim().isNotEmpty() && !sending
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(ClaudeRadius.pill))
                    .background(
                        if (canSend) ClaudeColors.Primary
                        else ClaudeColors.Primary.copy(alpha = 0.3f)
                    )
                    .clickable(enabled = canSend, onClick = onSend)
                    .padding(horizontal = ClaudeSpacing.lg, vertical = ClaudeSpacing.xs)
            ) {
                Text(sendLabel, style = ClaudeType.Button, color = ClaudeColors.OnPrimary)
            }
        }
    }
}

@Composable
private fun EmptyStateNoPartner() {
    val colors = MaterialTheme.appColors
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("还没有人收信", style = ClaudeType.TitleMd, color = colors.ink)
        Spacer(Modifier.height(ClaudeSpacing.xs))
        Text("先在「我的」绑定搭档", style = ClaudeType.BodyMd, color = colors.muted)
    }
}

/** 把时间戳映射到时段词："午后" / "傍晚" / "夜深了" 等 */
private fun letterTimeMarker(ts: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
    val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
    return when (h) {
        in 0..4 -> "深夜"
        in 5..7 -> "天刚亮"
        in 8..10 -> "上午"
        in 11..12 -> "临近正午"
        in 13..15 -> "午后"
        in 16..17 -> "傍晚"
        in 18..20 -> "夜色温柔"
        else -> "夜深了"
    }
}
