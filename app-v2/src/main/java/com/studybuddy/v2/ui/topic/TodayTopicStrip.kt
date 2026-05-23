package com.studybuddy.v2.ui.topic

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors

/**
 * 主页"今天专注什么"主区 —— 横向滑动切换主题。
 *
 * 视觉：
 * - 顶部 caption "今天做"
 * - 中央衬线大字主题名（左右滑切换 / pager indicator）
 * - 累计时长副标
 * - 长按主题名 → 编辑（改名/改色/归档）
 * - 末尾 "+ 新加一件事" 页
 *
 * 单手 UX：滑动是核心交互，拇指轻松完成。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodayTopicStrip(
    viewModel: TopicViewModel = hiltViewModel(),
    onTopicChanged: (String?) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors

    // 列表：[自由专注] + 所有主题 + [+新建]
    // 自由专注用 null 表示
    val items: List<TopicItem> = buildList {
        add(TopicItem.Free)
        state.topics.forEach { add(TopicItem.Topic(it)) }
        add(TopicItem.AddNew)
    }

    // 找到当前选中位置
    val initialPage = remember(state.currentTopicId, state.topics.size) {
        if (state.currentTopicId == null) 0
        else {
            val idx = state.topics.indexOfFirst { it.id == state.currentTopicId }
            if (idx >= 0) idx + 1 else 0
        }
    }
    val pagerState = rememberPagerState(initialPage = initialPage) { items.size }

    LaunchedEffect(pagerState.currentPage) {
        val item = items.getOrNull(pagerState.currentPage)
        when (item) {
            is TopicItem.Free -> {
                viewModel.selectTopic(null)
                onTopicChanged(null)
            }
            is TopicItem.Topic -> {
                viewModel.selectTopic(item.topic.id)
                onTopicChanged(item.topic.id)
            }
            is TopicItem.AddNew, null -> Unit  // 不选中
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("今天做", style = ClaudeType.CaptionUppercase, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.sm))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(120.dp)
        ) { page ->
            val item = items[page]
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                when (item) {
                    is TopicItem.Free -> FreeTopicCard()
                    is TopicItem.Topic -> TopicCard(
                        topic = item.topic,
                        onLongPress = { viewModel.openEdit(item.topic) }
                    )
                    is TopicItem.AddNew -> AddNewCard(onClick = viewModel::openAddDialog)
                }
            }
        }

        Spacer(Modifier.height(ClaudeSpacing.sm))
        // 底部圆点指示器
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            items.forEachIndexed { idx, _ ->
                val isActive = pagerState.currentPage == idx
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (isActive) 6.dp else 4.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            if (isActive) ClaudeColors.Primary
                            else colors.hairline
                        )
                )
            }
        }
    }

    // 复用 TopicViewModel 已有的对话框
    if (state.showAddDialog) {
        SimpleAddTopicDialog(
            name = state.newTopicName,
            onNameChange = viewModel::updateNewTopicName,
            onSave = viewModel::saveNewTopic,
            onDismiss = viewModel::closeAddDialog
        )
    }
    state.editingTopic?.let { topic ->
        SimpleEditTopicDialog(
            topicName = topic.name,
            onRename = { viewModel.rename(topic.id, it) },
            onArchive = { viewModel.archive(topic.id) },
            onDismiss = viewModel::closeEdit
        )
    }
}

private sealed class TopicItem {
    object Free : TopicItem()
    data class Topic(val topic: com.studybuddy.v2.data.model.FocusTopic) : TopicItem()
    object AddNew : TopicItem()
}

@Composable
private fun FreeTopicCard() {
    val colors = MaterialTheme.appColors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "自由专注",
            style = serifTitleStyle(),
            color = colors.ink
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "想做啥都行",
            style = ClaudeType.Caption,
            color = colors.muted
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopicCard(
    topic: com.studybuddy.v2.data.model.FocusTopic,
    onLongPress: () -> Unit
) {
    val colors = MaterialTheme.appColors
    val accent = parseHex(topic.colorHex)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.pointerInput(topic.id) {
            detectTapGestures(onLongPress = { onLongPress() })
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 像素方块色标
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(accent)
            )
            Spacer(Modifier.width(ClaudeSpacing.sm))
            Text(
                topic.name,
                style = serifTitleStyle(),
                color = colors.ink
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "已陪你 ${topic.totalFocusMs / 3_600_000}h · ${topic.sessionCount} 次",
            style = ClaudeType.Caption,
            color = colors.muted
        )
    }
}

@Composable
private fun AddNewCard(onClick: () -> Unit) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(ClaudeRadius.md))
            .border(1.dp, colors.hairline, RoundedCornerShape(ClaudeRadius.md))
            .clickable(onClick = onClick)
            .padding(horizontal = ClaudeSpacing.xl, vertical = ClaudeSpacing.md)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "+",
                style = serifTitleStyle().copy(fontSize = 36.sp),
                color = ClaudeColors.Primary
            )
            Text(
                "新加一件事",
                style = ClaudeType.Caption,
                color = ClaudeColors.Primary
            )
        }
    }
}

@Composable
private fun SimpleAddTopicDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.appColors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceCard,
        title = { Text("新加一件事", style = ClaudeType.TitleLg, color = colors.ink) },
        text = {
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                textStyle = serifTitleStyle().copy(color = colors.ink, fontSize = 22.sp),
                cursorBrush = SolidColor(ClaudeColors.Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.hairline, RoundedCornerShape(ClaudeRadius.sm))
                    .padding(ClaudeSpacing.md),
                decorationBox = { inner ->
                    if (name.isEmpty()) {
                        Text("GRE 单词 / 毕设 / 论文阅读…",
                            style = serifTitleStyle().copy(fontSize = 22.sp),
                            color = colors.muted)
                    }
                    inner()
                }
            )
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
private fun SimpleEditTopicDialog(
    topicName: String,
    onRename: (String) -> Unit,
    onArchive: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.appColors
    val nameState = remember { androidx.compose.runtime.mutableStateOf(topicName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceCard,
        title = { Text("编辑主题", style = ClaudeType.TitleLg, color = colors.ink) },
        text = {
            BasicTextField(
                value = nameState.value,
                onValueChange = { nameState.value = it.take(16) },
                textStyle = serifTitleStyle().copy(color = colors.ink, fontSize = 22.sp),
                cursorBrush = SolidColor(ClaudeColors.Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.hairline, RoundedCornerShape(ClaudeRadius.sm))
                    .padding(ClaudeSpacing.md)
            )
        },
        confirmButton = {
            TextButton(onClick = { onRename(nameState.value) }) {
                Text("保存", color = ClaudeColors.Primary)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onArchive) { Text("归档", color = colors.warning) }
                Spacer(Modifier.width(ClaudeSpacing.xs))
                TextButton(onClick = onDismiss) { Text("取消", color = colors.muted) }
            }
        }
    )
}

private fun parseHex(hex: String): Color = try {
    val cleaned = hex.removePrefix("#")
    val argb = cleaned.toLong(16) or 0xFF000000L
    Color(argb.toInt())
} catch (_: Exception) {
    ClaudeColors.Primary
}

private fun serifTitleStyle() = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Normal,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.3).sp,
    textAlign = TextAlign.Center
)
