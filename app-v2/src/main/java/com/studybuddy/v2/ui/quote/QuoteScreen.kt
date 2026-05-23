package com.studybuddy.v2.ui.quote

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.launch
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.data.model.Quote
import com.studybuddy.v2.data.model.QuoteVisibility
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.QuoteSerifEdit
import com.studybuddy.v2.theme.QuoteSerifEditSource
import com.studybuddy.v2.theme.QuoteSerifLarge
import com.studybuddy.v2.theme.QuoteSerifPlaceholder
import com.studybuddy.v2.theme.QuoteSerifSmall
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.AppButton
import com.studybuddy.v2.ui.component.BackRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 话廊 —— 灵光一闪的句子，私密角落、可选对端可见。
 *
 * 极简列表 + 衬线大字 + hairline 分隔。每条仅显示文本 + 来源 + 可见性副标。
 * 点右下角圆形 + 按钮写入。
 */

// 话廊专属字体 —— 统一管理在 theme/QuoteFont.kt，本地仅做别名指向
// 升级路径：把 EB Garamond ttf 放到 res/font/ 后改 QuoteFont.kt 里的 QuoteSerifFamily 即可
private val QuoteSerifItalic = QuoteSerifLarge
private val QuoteSourceStyle = QuoteSerifSmall
private val QuoteEditStyle = QuoteSerifEdit
private val QuoteEditPlaceholder = QuoteSerifPlaceholder
private val QuoteEditSourceStyle = QuoteSerifEditSource

@Composable
fun QuoteScreen(
    onBack: () -> Unit = {},
    viewModel: QuoteViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors
    BackHandler { onBack() }

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
                .padding(top = ClaudeSpacing.lg, bottom = ClaudeSpacing.xxl)
        ) {
            BackRow(onBack = onBack)
            Spacer(Modifier.height(ClaudeSpacing.lg))
            Text(
                "话廊",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    fontSize = 52.sp,
                    lineHeight = 60.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = colors.ink
            )
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Text(
                "灵光一闪的句子。",
                style = QuoteSourceStyle.copy(fontSize = 15.sp),
                color = colors.muted
            )
            Spacer(Modifier.height(ClaudeSpacing.xxl))

            if (state.quotes.isEmpty() && !state.loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "尚未落笔。",
                        style = QuoteSourceStyle.copy(fontSize = 16.sp),
                        color = colors.muted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(state.quotes, key = { it.id }) { q ->
                        QuoteRow(q, onDelete = { viewModel.delete(q.id) })
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

        // 右下角圆形 + 按钮
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
                onDismiss = viewModel::closeCompose,
                onSubmit = viewModel::submit
            )
        }
        // 错误提示
        if (state.errorMessage != null) {
            AlertDialog(
                onDismissRequest = viewModel::dismissError,
                title = { Text("没保存上", style = ClaudeType.TitleSm, color = colors.ink) },
                text = { Text(state.errorMessage ?: "", style = ClaudeType.BodySm, color = colors.body) },
                confirmButton = { AppButton.Text(text = "好", onClick = viewModel::dismissError) },
                containerColor = colors.canvas
            )
        }
    }
}

@Composable
private fun QuoteRow(q: Quote, onDelete: () -> Unit) {
    val colors = MaterialTheme.appColors
    val df = remember { SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) }
    var confirmDelete by remember { mutableStateOf(false) }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val revealDp = 88.dp
    val revealPx = with(density) { revealDp.toPx() }
    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxWidth()) {
        // 底层：露出的红色"删除"按钮（在右侧）
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(ClaudeColors.Error),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .width(revealDp)
                    .fillMaxHeight()
                    .clickable { confirmDelete = true },
                contentAlignment = Alignment.Center
            ) {
                Text("删除", style = ClaudeType.Button, color = ClaudeColors.OnPrimary)
            }
        }
        // 顶层：可滑动的内容
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { androidx.compose.ui.unit.IntOffset(offsetX.value.toInt(), 0) }
                .background(colors.canvas)
                .pointerInput(q.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dx ->
                            scope.launch {
                                val target = (offsetX.value + dx).coerceIn(-revealPx, 0f)
                                offsetX.snapTo(target)
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                val target = if (offsetX.value < -revealPx / 2f) -revealPx else 0f
                                offsetX.animateTo(target, androidx.compose.animation.core.tween(180))
                            }
                        }
                    )
                }
                .padding(vertical = ClaudeSpacing.xl)
        ) {
            Text(q.text, style = QuoteSerifItalic, color = colors.ink)
            Spacer(Modifier.height(ClaudeSpacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (q.source.isNotBlank()) {
                    Text("—— ${q.source}", style = QuoteSourceStyle, color = colors.body)
                    Spacer(Modifier.width(ClaudeSpacing.sm))
                    Text("·", style = QuoteSourceStyle, color = colors.muted)
                    Spacer(Modifier.width(ClaudeSpacing.sm))
                }
                Text(
                    df.format(Date(q.createdAt)),
                    style = QuoteSourceStyle.copy(fontStyle = FontStyle.Normal),
                    color = colors.muted
                )
                if (q.visibility == "PARTNER") {
                    Spacer(Modifier.width(ClaudeSpacing.sm))
                    Text("·", style = QuoteSourceStyle, color = colors.muted)
                    Spacer(Modifier.width(ClaudeSpacing.sm))
                    Text(
                        "对 TA 可见",
                        style = QuoteSourceStyle.copy(fontStyle = FontStyle.Normal),
                        color = ClaudeColors.Primary
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = {
                confirmDelete = false
                scope.launch { offsetX.animateTo(0f) }
            },
            title = { Text("删除这一句？", style = QuoteSourceStyle.copy(fontSize = 16.sp), color = colors.ink) },
            text = { Text(q.text, style = QuoteSourceStyle.copy(fontSize = 14.sp), color = colors.body) },
            confirmButton = {
                AppButton.Text(text = "删除", onClick = {
                    confirmDelete = false
                    onDelete()
                })
            },
            dismissButton = {
                AppButton.Text(text = "取消", onClick = {
                    confirmDelete = false
                    scope.launch { offsetX.animateTo(0f) }
                })
            },
            containerColor = colors.canvas
        )
    }
}

@Composable
private fun ComposeDialog(
    onDismiss: () -> Unit,
    onSubmit: (text: String, source: String, visibility: QuoteVisibility) -> Unit
) {
    val colors = MaterialTheme.appColors
    var text by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var partnerVisible by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("写下", style = ClaudeType.TitleMd, color = colors.ink) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("写下灵光一闪…", style = QuoteEditPlaceholder, color = colors.muted) },
                    minLines = 3,
                    maxLines = 8,
                    textStyle = QuoteEditStyle.copy(color = colors.ink),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(ClaudeSpacing.sm))
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    placeholder = { Text("来源（可空）", style = QuoteEditSourceStyle, color = colors.muted) },
                    singleLine = true,
                    textStyle = QuoteEditSourceStyle.copy(color = colors.ink),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(ClaudeSpacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(ClaudeRadius.sm))
                            .border(
                                1.dp,
                                if (partnerVisible) ClaudeColors.Primary else colors.hairline,
                                RoundedCornerShape(ClaudeRadius.sm)
                            )
                            .background(
                                if (partnerVisible) ClaudeColors.Primary.copy(alpha = 0.08f)
                                else Color.Transparent
                            )
                            .clickable { partnerVisible = !partnerVisible }
                            .padding(horizontal = ClaudeSpacing.sm, vertical = ClaudeSpacing.xs)
                    ) {
                        Text(
                            if (partnerVisible) "✓ 对 TA 可见" else "对 TA 可见",
                            style = ClaudeType.Caption,
                            color = if (partnerVisible) ClaudeColors.Primary else colors.body
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppButton.Text(
                text = "保存",
                onClick = {
                    onSubmit(
                        text,
                        source,
                        if (partnerVisible) QuoteVisibility.PARTNER else QuoteVisibility.PRIVATE
                    )
                }
            )
        },
        dismissButton = {
            AppButton.Text(text = "取消", onClick = onDismiss)
        },
        containerColor = colors.canvas
    )
}
