package com.studybuddy.v2.ui.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.data.model.FocusTopic
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors

/**
 * 多主题铭牌选择器 —— "鞍部风" D 方向。
 *
 * 视觉规则：
 * - 衬线大字主题名 + 累计时长（h 单位）
 * - 左侧 8dp 像素方块色标（用户选色）
 * - 选中态：coral 描边 + 填色色块
 * - 未选中：hairline 描边 + 空心色块
 * - 长按某行 → 编辑（改名 / 改色 / 归档）
 * - "+ 新加一件事" 行 → 弹建表
 *
 * 入口：Home 顶部 "今天做什么" 卡 / FocusSetupSheet 顶部 / Settings 主题管理（未来）
 */
@Composable
fun TopicPicker(
    viewModel: TopicViewModel = hiltViewModel(),
    onTopicSelected: (FocusTopic?) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors

    Column(modifier = Modifier.fillMaxWidth()) {
        // 标题
        Text(
            "今天做什么？",
            style = ClaudeType.TitleMd,
            color = colors.ink
        )
        Spacer(Modifier.height(ClaudeSpacing.sm))

        // 不绑主题选项（"自由专注"）
        TopicRow(
            displayName = "自由专注",
            displayHours = null,
            color = colors.muted,
            selected = state.currentTopicId == null,
            onClick = {
                viewModel.selectTopic(null)
                onTopicSelected(null)
            },
            onLongPress = null
        )

        // 主题列表
        state.topics.forEach { topic ->
            TopicRow(
                displayName = topic.name,
                displayHours = topic.totalFocusMs / 3_600_000.0,
                color = parseHex(topic.colorHex),
                selected = state.currentTopicId == topic.id,
                onClick = {
                    viewModel.selectTopic(topic.id)
                    onTopicSelected(topic)
                },
                onLongPress = { viewModel.openEdit(topic) }
            )
        }

        // 新加一件事
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ClaudeRadius.md))
                .clickable { viewModel.openAddDialog() }
                .padding(vertical = ClaudeSpacing.sm, horizontal = ClaudeSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("+", style = ClaudeType.TitleLg, color = ClaudeColors.Primary)
            Spacer(Modifier.size(ClaudeSpacing.sm))
            Text("新加一件事", style = ClaudeType.BodyMd, color = ClaudeColors.Primary)
        }
    }

    // 新建对话框
    if (state.showAddDialog) {
        AddTopicDialog(
            name = state.newTopicName,
            color = state.newTopicColor,
            onNameChange = viewModel::updateNewTopicName,
            onColorChange = viewModel::updateNewTopicColor,
            onSave = viewModel::saveNewTopic,
            onDismiss = viewModel::closeAddDialog
        )
    }

    // 编辑对话框
    state.editingTopic?.let { topic ->
        EditTopicDialog(
            topic = topic,
            onRename = { newName -> viewModel.rename(topic.id, newName) },
            onSetColor = { hex -> viewModel.setColor(topic.id, hex) },
            onArchive = { viewModel.archive(topic.id) },
            onDismiss = viewModel::closeEdit
        )
    }
}

@Composable
private fun TopicRow(
    displayName: String,
    displayHours: Double?,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?
) {
    val colors = MaterialTheme.appColors
    val rowMod = if (onLongPress != null) {
        Modifier.pointerInput(displayName) {
            detectTapGestures(
                onTap = { onClick() },
                onLongPress = { onLongPress() }
            )
        }
    } else {
        Modifier.clickable { onClick() }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ClaudeRadius.md))
            .background(if (selected) ClaudeColors.Primary.copy(alpha = 0.06f) else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) ClaudeColors.Primary else Color.Transparent,
                shape = RoundedCornerShape(ClaudeRadius.md)
            )
            .then(rowMod)
            .padding(horizontal = ClaudeSpacing.md, vertical = ClaudeSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 像素方块色标
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (selected) color else Color.Transparent)
                .border(1.dp, color, RoundedCornerShape(0.dp))
        )
        Spacer(Modifier.size(ClaudeSpacing.md))

        // 衬线主题名
        Text(
            displayName,
            style = nameStyle(),
            color = if (selected) ClaudeColors.Primary else colors.ink,
            modifier = Modifier.weight(1f)
        )

        // 累计时长
        if (displayHours != null) {
            Text(
                if (displayHours < 1.0) "%.1fh".format(displayHours)
                else "${displayHours.toInt()}h",
                style = ClaudeType.Caption,
                color = colors.muted
            )
        }
    }
}

@Composable
private fun AddTopicDialog(
    name: String,
    color: String,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.appColors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceCard,
        title = { Text("新加一件事", style = ClaudeType.TitleLg, color = colors.ink) },
        text = {
            Column {
                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    textStyle = nameStyle().copy(color = colors.ink),
                    cursorBrush = SolidColor(ClaudeColors.Primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.hairline, RoundedCornerShape(ClaudeRadius.sm))
                        .padding(ClaudeSpacing.md),
                    decorationBox = { inner ->
                        if (name.isEmpty()) {
                            Text("GRE 单词 / 毕设 / 论文阅读…", style = nameStyle(), color = colors.muted)
                        }
                        inner()
                    }
                )
                Spacer(Modifier.height(ClaudeSpacing.md))
                Text("选个颜色", style = ClaudeType.Caption, color = colors.muted)
                Spacer(Modifier.height(ClaudeSpacing.xs))
                ColorPalette(selected = color, onPick = onColorChange)
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = name.trim().isNotEmpty()) {
                Text("加上", color = ClaudeColors.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = colors.muted) }
        }
    )
}

@Composable
private fun EditTopicDialog(
    topic: FocusTopic,
    onRename: (String) -> Unit,
    onSetColor: (String) -> Unit,
    onArchive: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.appColors
    val nameState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(topic.name) }
    val colorState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(topic.colorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceCard,
        title = { Text("编辑", style = ClaudeType.TitleLg, color = colors.ink) },
        text = {
            Column {
                BasicTextField(
                    value = nameState.value,
                    onValueChange = { nameState.value = it.take(16) },
                    textStyle = nameStyle().copy(color = colors.ink),
                    cursorBrush = SolidColor(ClaudeColors.Primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.hairline, RoundedCornerShape(ClaudeRadius.sm))
                        .padding(ClaudeSpacing.md)
                )
                Spacer(Modifier.height(ClaudeSpacing.md))
                Text("颜色", style = ClaudeType.Caption, color = colors.muted)
                Spacer(Modifier.height(ClaudeSpacing.xs))
                ColorPalette(selected = colorState.value, onPick = { colorState.value = it })
                Spacer(Modifier.height(ClaudeSpacing.lg))
                Text("已陪你 ${topic.totalFocusMs / 3_600_000} 小时 · ${topic.sessionCount} 次",
                    style = ClaudeType.Caption, color = colors.muted)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onRename(nameState.value)
                if (colorState.value != topic.colorHex) onSetColor(colorState.value)
            }) { Text("保存", color = ClaudeColors.Primary) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onArchive) { Text("归档", color = colors.warning) }
                Spacer(Modifier.size(ClaudeSpacing.xs))
                TextButton(onClick = onDismiss) { Text("取消", color = colors.muted) }
            }
        }
    )
}

private val PALETTE = listOf(
    "#CC785C",  // coral（默认）
    "#D4A04A",  // amber
    "#5B8266",  // sage（沉静绿）
    "#4F5970",  // saddle ink（鞍部猫色）
    "#A37B73",  // 暮色棕
    "#B0BAD0"   // 雾蓝灰
)

@Composable
private fun ColorPalette(selected: String, onPick: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(ClaudeSpacing.sm)) {
        PALETTE.forEach { hex ->
            val color = parseHex(hex)
            val isSel = hex.equals(selected, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(color)
                    .border(
                        width = if (isSel) 2.dp else 0.dp,
                        color = MaterialTheme.appColors.ink,
                        shape = RoundedCornerShape(0.dp)
                    )
                    .clickable { onPick(hex) }
            )
        }
    }
}

private fun parseHex(hex: String): Color = try {
    val cleaned = hex.removePrefix("#")
    val argb = cleaned.toLong(16) or 0xFF000000L
    Color(argb.toInt())
} catch (_: Exception) {
    Color(0xFFCC785C.toInt())
}

/** 主题名衬线大字（呼应话廊 / 信件） */
private fun nameStyle() = TextStyle(
    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.2.sp
)
