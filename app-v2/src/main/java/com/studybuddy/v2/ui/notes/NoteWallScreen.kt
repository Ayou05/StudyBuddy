package com.studybuddy.v2.ui.notes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.studybuddy.v2.data.model.Note
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 便签墙 —— 朋友圈式时间流。
 *
 * 设计立场：
 * - 不做无边记式无限画布（手机操作累、复杂度高、易出 bug）
 * - 按时间倒序垂直流，新便签在顶部
 * - 卡片样式：第一张图当背景 + 高斯模糊（亚克力质感）
 * - 多图：底部缩略图条，点击全屏查看
 * - 双击删除自己发的便签
 * - 底部悬浮"贴一张"按钮
 */
@Composable
fun NoteWallScreen(viewModel: NoteWallViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors

    // 图片全屏查看器状态
    var viewerImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var viewerInitIdx by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().background(colors.canvas)) {
        if (state.notes.isEmpty() && !state.loading) {
            EmptyState(onAdd = viewModel::openEdit)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = ClaudeSpacing.pageHorizontal,
                    vertical = ClaudeSpacing.lg
                ),
                verticalArrangement = Arrangement.spacedBy(ClaudeSpacing.md)
            ) {
                item {
                    Column {
                        Text("WALL", style = ClaudeType.CaptionUppercase, color = colors.muted)
                        Spacer(Modifier.height(ClaudeSpacing.xs))
                        Text("便签墙", style = ClaudeType.DisplayMd, color = colors.ink)
                        Spacer(Modifier.height(ClaudeSpacing.xs))
                        Text(
                            "我们一起的小事，一张一张贴上来。",
                            style = ClaudeType.Caption,
                            color = colors.muted
                        )
                        Spacer(Modifier.height(ClaudeSpacing.md))
                    }
                }
                items(state.notes, key = { it.id }) { note ->
                    MomentCard(
                        note = note,
                        myUserId = state.myUserId,
                        myNickname = state.myNickname,
                        partnerNickname = state.partnerNickname,
                        onDelete = { viewModel.deleteNote(note.id) },
                        onImageClick = { idx ->
                            viewerImages = note.imageUrls.map {
                                "${PbConfig.BASE_URL}api/files/${PbConfig.NOTES}/${note.id}/$it"
                            }
                            viewerInitIdx = idx
                        }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }  // 给悬浮按钮让位
            }
        }

        // 悬浮"贴一张"按钮
        FloatingAddButton(
            onClick = viewModel::openEdit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(ClaudeSpacing.lg)
        )

        // 编辑对话框
        if (state.showEditDialog) {
            EditDialog(
                draft = state.draft,
                pickedImages = state.pickedImages,
                saving = state.saving,
                onDraftChange = viewModel::updateDraft,
                onPickImages = viewModel::addImages,
                onRemoveImage = viewModel::removeImage,
                onSave = viewModel::saveNote,
                onDismiss = viewModel::closeEdit
            )
        }

        // 图片全屏查看器
        AnimatedVisibility(
            visible = viewerImages.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (viewerImages.isNotEmpty()) {
                ImageViewerOverlay(
                    images = viewerImages,
                    initialIndex = viewerInitIdx,
                    onDismiss = { viewerImages = emptyList() }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 朋友圈式便签卡片：头像 + 名字 + 时间 → 文字段 → 图片 grid（垂直分层、泾渭分明）
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MomentCard(
    note: Note,
    myUserId: String,
    myNickname: String,
    partnerNickname: String,
    onDelete: () -> Unit,
    onImageClick: (idx: Int) -> Unit
) {
    val colors = MaterialTheme.appColors
    val isMine = note.authorId == myUserId
    val authorName = if (isMine) myNickname.ifBlank { "我" } else partnerNickname.ifBlank { "TA" }
    val authorInitial = authorName.firstOrNull()?.toString() ?: "·"
    val avatarColor = if (isMine) ClaudeColors.Primary else ClaudeColors.AccentAmber
    var showDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(note.id) {
                detectTapGestures(onLongPress = { if (isMine) showDelete = true })
            }
            .padding(vertical = ClaudeSpacing.sm)
    ) {
        // ── Header: avatar + name + time ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 头像：色块圆 + 首字母
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    authorInitial,
                    style = ClaudeType.TitleSm,
                    color = ClaudeColors.OnPrimary
                )
            }
            Spacer(Modifier.size(ClaudeSpacing.sm))
            Column {
                Text(authorName, style = ClaudeType.TitleSm, color = colors.ink)
                Text(formatTime(note.createdAt), style = ClaudeType.Caption, color = colors.muted)
            }
        }

        Spacer(Modifier.height(ClaudeSpacing.sm))

        // ── 正文 ──
        if (note.text.isNotBlank()) {
            Text(
                note.text,
                style = ClaudeType.BodyMd,
                color = colors.ink,
                modifier = Modifier.padding(start = 44.dp)  // 与头像对齐缩进
            )
        }

        // ── 图片 grid（朋友圈式 3 列）──
        if (note.imageUrls.isNotEmpty()) {
            if (note.text.isNotBlank()) {
                Spacer(Modifier.height(ClaudeSpacing.sm))
            }
            Box(modifier = Modifier.padding(start = 44.dp)) {
                NineGrid(
                    imageUrls = note.imageUrls.map {
                        "${PbConfig.BASE_URL}api/files/${PbConfig.NOTES}/${note.id}/$it"
                    },
                    onClick = onImageClick
                )
            }
        }

        Spacer(Modifier.height(ClaudeSpacing.sm))

        // ── 分割线 ──
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.hairlineSoft)
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = colors.surfaceCard,
            title = { Text("删掉这张？", style = ClaudeType.TitleMd, color = colors.ink) },
            text = { Text("不能撤销。", style = ClaudeType.BodySm, color = colors.body) },
            confirmButton = {
                TextButton(onClick = { showDelete = false; onDelete() }) {
                    Text("删除", color = ClaudeColors.Warning)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("取消", color = colors.muted)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 朋友圈式 grid：1 张大图 / 2-4 张 2x2 / 5-9 张 3x3
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NineGrid(
    imageUrls: List<String>,
    onClick: (idx: Int) -> Unit
) {
    if (imageUrls.isEmpty()) return
    when (imageUrls.size) {
        1 -> {
            // 单图：4:3 大图
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClick(0) }
            ) {
                AsyncImage(
                    model = imageUrls[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        else -> {
            // 多图：3 列 grid
            val cols = if (imageUrls.size == 4) 2 else 3
            val rows = (imageUrls.size + cols - 1) / cols
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (r in 0 until rows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (c in 0 until cols) {
                            val idx = r * cols + c
                            if (idx < imageUrls.size) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onClick(idx) }
                                ) {
                                    AsyncImage(
                                        model = imageUrls[idx],
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 悬浮"贴一张"按钮
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FloatingAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(ClaudeColors.Primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("+", style = ClaudeType.DisplayMd, color = ClaudeColors.OnPrimary)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 空态
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = Modifier.fillMaxSize().padding(ClaudeSpacing.pageHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("WALL", style = ClaudeType.CaptionUppercase, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.xs))
        Text("便签墙", style = ClaudeType.DisplayLg, color = colors.ink)
        Spacer(Modifier.height(ClaudeSpacing.md))
        Text(
            "贴上第一张，从今天开始。",
            style = ClaudeType.BodyMd,
            color = colors.muted
        )
        Spacer(Modifier.height(ClaudeSpacing.xl))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(ClaudeRadius.pill))
                .background(ClaudeColors.Primary)
                .clickable(onClick = onAdd)
                .padding(horizontal = ClaudeSpacing.xl, vertical = ClaudeSpacing.md)
        ) {
            Text("贴一张", style = ClaudeType.Button, color = ClaudeColors.OnPrimary)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 图片全屏查看器（支持左右滑切多图）
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ImageViewerOverlay(
    images: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AsyncImage(
                model = images[page],
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        // 顶部页码 + 关闭按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ClaudeSpacing.lg)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text("×", color = Color.White, style = ClaudeType.TitleLg)
            }
            Spacer(Modifier.fillMaxWidth(0.4f).height(1.dp))
            if (images.size > 1) {
                Text(
                    "${pagerState.currentPage + 1} / ${images.size}",
                    color = Color.White.copy(alpha = 0.7f),
                    style = ClaudeType.Caption
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 编辑对话框
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EditDialog(
    draft: String,
    pickedImages: List<android.net.Uri>,
    saving: Boolean,
    onDraftChange: (String) -> Unit,
    onPickImages: (List<android.net.Uri>) -> Unit,
    onRemoveImage: (android.net.Uri) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.appColors
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris -> if (uris.isNotEmpty()) onPickImages(uris) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceCard,
        title = { Text("贴一张便签", style = ClaudeType.TitleLg, color = colors.ink) },
        text = {
            Column {
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    textStyle = ClaudeType.BodyMd,
                    cursorBrush = SolidColor(ClaudeColors.Primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ClaudeSpacing.sm),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) {
                            Text(
                                "今天的对话很美。 / 一起去过的咖啡馆。",
                                style = ClaudeType.BodyMd,
                                color = colors.muted
                            )
                        }
                        inner()
                    }
                )
                Spacer(Modifier.height(ClaudeSpacing.sm))
                if (pickedImages.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ClaudeSpacing.xs)
                    ) {
                        pickedImages.forEach { uri ->
                            Box(modifier = Modifier.size(72.dp)) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(colors.ink.copy(alpha = 0.7f))
                                        .clickable { onRemoveImage(uri) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("×", color = colors.canvas, style = ClaudeType.Caption)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(ClaudeSpacing.xs))
                }
                if (pickedImages.size < 4) {
                    TextButton(
                        onClick = {
                            pickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Text(
                            "+ 加图片 (${pickedImages.size}/4)",
                            color = ClaudeColors.Primary,
                            style = ClaudeType.Caption
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = !saving && (draft.trim().isNotEmpty() || pickedImages.isNotEmpty())
            ) {
                Text(if (saving) "贴上中…" else "贴上", color = ClaudeColors.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("取消", color = colors.muted)
            }
        }
    )
}

private fun formatTime(epoch: Long): String {
    if (epoch <= 0) return ""
    val now = System.currentTimeMillis()
    val diff = now - epoch
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3600_000 -> "${diff / 60_000} 分钟前"
        diff < 86400_000 -> "${diff / 3600_000} 小时前"
        diff < 7 * 86400_000 -> "${diff / 86400_000} 天前"
        else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(epoch))
    }
}
