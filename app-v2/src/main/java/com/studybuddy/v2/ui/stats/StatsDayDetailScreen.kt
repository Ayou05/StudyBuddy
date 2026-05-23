package com.studybuddy.v2.ui.stats

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.data.model.FocusSession
import com.studybuddy.v2.data.model.statusEnum
import com.studybuddy.v2.data.model.typeEnum
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.BackRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 某一天的专注会话详情页。从 StatsScreen BarChart 双击柱子进入。
 *
 * 列表展示该日所有 sessions（含 ABORTED/COMPLETED/PAUSED），按时间倒序。
 * 顶部一张总结卡：完成/放弃/总分钟数。
 */
@Composable
fun StatsDayDetailScreen(
    ymd: String,
    onBack: () -> Unit,
    viewModel: StatsDayDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors
    BackHandler { onBack() }

    LaunchedEffect(ymd) { viewModel.load(ymd) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(horizontal = ClaudeSpacing.pageHorizontal)
            .padding(top = ClaudeSpacing.lg, bottom = ClaudeSpacing.xxl)
    ) {
        BackRow(onBack = onBack)
        Spacer(Modifier.height(ClaudeSpacing.md))

        Text("STATS · DAY", style = ClaudeType.CaptionUppercase, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.sm))
        Text(prettyYmd(ymd), style = ClaudeType.DisplayLg, color = colors.ink)
        Spacer(Modifier.height(ClaudeSpacing.xs))

        val totalMin = state.sessions
            .filter { it.statusEnum().name == "COMPLETED" }
            .sumOf { (it.actualDurationMs ?: 0L) / 60_000L }
        val completedCnt = state.sessions.count { it.statusEnum().name == "COMPLETED" }
        val abortedCnt = state.sessions.count { it.statusEnum().name == "ABORTED" }

        Text(
            "$totalMin 分钟 · 完成 $completedCnt · 放弃 $abortedCnt",
            style = ClaudeType.BodyMd,
            color = colors.muted
        )

        Spacer(Modifier.height(ClaudeSpacing.lg))

        when {
            state.loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("加载中…", style = ClaudeType.BodyMd, color = colors.muted)
                }
            }
            state.sessions.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("这天没有记录。", style = ClaudeType.BodyMd, color = colors.muted)
                }
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(ClaudeSpacing.sm)) {
                    items(state.sessions, key = { it.id }) { session ->
                        SessionRow(session)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: FocusSession) {
    val colors = MaterialTheme.appColors
    val statusName = session.statusEnum().name
    val typeName = session.typeEnum().name

    val accent = when (statusName) {
        "COMPLETED" -> colors.success
        "ABORTED" -> ClaudeColors.Primary
        "ACTIVE" -> ClaudeColors.AccentAmber
        "PAUSED" -> ClaudeColors.AccentAmber
        else -> colors.muted
    }
    val statusLabel = when (statusName) {
        "COMPLETED" -> "完成"
        "ABORTED" -> "放弃"
        "ACTIVE" -> "进行中"
        "PAUSED" -> "暂停"
        else -> statusName
    }

    val startTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(session.startedAt))
    val durationMin = (session.actualDurationMs ?: 0L) / 60_000L
    val plannedMin = session.plannedDurationMs / 60_000L

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ClaudeRadius.md))
            .background(colors.surfaceCard)
            .padding(ClaudeSpacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .padding(end = ClaudeSpacing.md)
            ) {
                // 左侧时间
                Column {
                    Text(startTime, style = ClaudeType.TitleSm, color = colors.ink)
                    Text(typeName, style = ClaudeType.Caption, color = colors.muted)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (durationMin > 0) "$durationMin / $plannedMin 分钟"
                    else "目标 $plannedMin 分钟",
                    style = ClaudeType.BodyMd,
                    color = colors.ink
                )
                if (session.goal.isNotBlank()) {
                    Text(session.goal, style = ClaudeType.Caption, color = colors.muted)
                }
                if (session.isInLibrary) {
                    Text("· 在地标内 ×1.5", style = ClaudeType.Caption, color = colors.success)
                }
            }
            Text(statusLabel, style = ClaudeType.Caption, color = accent)
        }
    }
}

private fun prettyYmd(ymd: String): String = try {
    val date = java.time.LocalDate.parse(ymd)
    val today = java.time.LocalDate.now()
    val diff = java.time.temporal.ChronoUnit.DAYS.between(date, today)
    when (diff) {
        0L -> "今天"
        1L -> "昨天"
        else -> "${date.monthValue} 月 ${date.dayOfMonth} 日"
    }
} catch (_: Exception) { ymd }
