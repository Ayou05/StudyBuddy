package com.studybuddy.v2.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors

/**
 * 更新检查 + 下载 + 安装的统一对话框入口。
 *
 * 状态机：
 * - 无 latest → 不显示
 * - 有 latest 但未开始下载 → 提示更新 + "下载"按钮
 * - 下载中 → 进度条
 * - readyApk != null → "安装"按钮
 * - failed → 错误提示 + 重试
 */
@Composable
fun UpdateDialog(viewModel: UpdateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors

    val v = state.latest ?: return  // 无新版不显示

    val title = "新版本 ${v.versionName}"
    val canForce = v.force

    AlertDialog(
        onDismissRequest = { if (!canForce) viewModel.dismiss() },
        containerColor = colors.surfaceCard,
        title = { Text(title, style = ClaudeType.TitleLg, color = colors.ink) },
        text = {
            Column {
                if (v.releaseNotes.isNotBlank()) {
                    Text(v.releaseNotes, style = ClaudeType.BodySm, color = colors.body)
                    Spacer(Modifier.height(ClaudeSpacing.sm))
                }
                if (v.apkSize > 0) {
                    Text(
                        "包大小 ${formatSize(v.apkSize)}",
                        style = ClaudeType.Caption, color = colors.muted
                    )
                }
                if (state.downloading || state.totalBytes > 0) {
                    Spacer(Modifier.height(ClaudeSpacing.md))
                    ProgressBar(progress = state.downloadProgress)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${formatSize(state.downloadedBytes)} / ${formatSize(state.totalBytes)}",
                        style = ClaudeType.Caption, color = colors.muted
                    )
                }
                state.failed?.let {
                    Spacer(Modifier.height(ClaudeSpacing.sm))
                    Text(it, style = ClaudeType.Caption, color = ClaudeColors.Warning)
                }
            }
        },
        confirmButton = {
            when {
                state.readyApk != null -> {
                    TextButton(onClick = viewModel::install) {
                        Text("立即安装", color = ClaudeColors.Primary)
                    }
                }
                state.downloading -> {
                    TextButton(onClick = {}, enabled = false) {
                        Text("下载中…", color = colors.muted)
                    }
                }
                else -> {
                    TextButton(onClick = viewModel::startDownload) {
                        Text("下载更新", color = ClaudeColors.Primary)
                    }
                }
            }
        },
        dismissButton = {
            if (!canForce) {
                TextButton(onClick = viewModel::dismiss) {
                    Text("稍后", color = colors.muted)
                }
            }
        }
    )
}

@Composable
private fun ProgressBar(progress: Float) {
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
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ClaudeColors.Primary)
        )
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.1f MB".format(bytes / 1024f / 1024f)
}
