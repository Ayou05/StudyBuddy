package com.studybuddy.v2.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.BackRow

@Composable
fun StatsScreen(
    onBack: () -> Unit = {},
    onOpenDay: (ymd: String) -> Unit = {},
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors
    androidx.activity.compose.BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(horizontal = ClaudeSpacing.pageHorizontal)
            .padding(top = ClaudeSpacing.lg, bottom = ClaudeSpacing.xxl)
    ) {
        BackRow(onBack = onBack)
        Spacer(Modifier.height(ClaudeSpacing.md))
        Text("STATS", style = ClaudeType.CaptionUppercase, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.sm))
        Text("数据统计", style = ClaudeType.DisplayLg, color = colors.ink)
        Spacer(Modifier.height(ClaudeSpacing.xs))
        Text(
            "${state.totalMinutes} 分钟 · 连续 ${state.streakDays} 天",
            style = ClaudeType.BodyMd, color = colors.muted
        )

        Spacer(Modifier.height(ClaudeSpacing.lg))
        RangeTabs(state.range, viewModel::setRange)
        Spacer(Modifier.height(ClaudeSpacing.lg))
        BarChartCard(state.dailyMinutes, onOpenDay = { idx ->
            // idx 是 dailyMinutes 数组下标，倒数第 idx 天 = today - (size-1-idx)
            val daysAgo = state.dailyMinutes.size - 1 - idx
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DATE, -daysAgo)
            val ymd = String.format(
                java.util.Locale.US, "%04d-%02d-%02d",
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            )
            onOpenDay(ymd)
        })
        Spacer(Modifier.height(ClaudeSpacing.lg))
        StreakCalendar(state.dailyMinutes)
        if (state.topicSlices.isNotEmpty()) {
            Spacer(Modifier.height(ClaudeSpacing.lg))
            TopicBreakdownCard(state.topicSlices)
        }
    }
}

@Composable
private fun TopicBreakdownCard(slices: List<com.studybuddy.v2.ui.stats.TopicSlice>) {
    val colors = MaterialTheme.appColors
    val total = slices.sumOf { it.minutes }.coerceAtLeast(1)
    com.studybuddy.v2.ui.component.AppCard.Feature(
        modifier = Modifier.fillMaxWidth(),
        padding = ClaudeSpacing.lg
    ) {
        Column {
            Text("主题分布", style = ClaudeType.Caption, color = colors.muted)
            Spacer(Modifier.height(ClaudeSpacing.sm))
            // 横向堆叠条：每段宽度按比例
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            ) {
                slices.forEach { s ->
                    Box(
                        modifier = Modifier
                            .weight(s.minutes.toFloat().coerceAtLeast(1f) / total)
                            .fillMaxHeight()
                            .background(parseHexSafe(s.colorHex))
                    )
                }
            }
            Spacer(Modifier.height(ClaudeSpacing.md))
            // 图例
            slices.forEach { s ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                            .background(parseHexSafe(s.colorHex))
                    )
                    Spacer(Modifier.width(ClaudeSpacing.sm))
                    Text(s.name, style = ClaudeType.BodySm, color = colors.body, modifier = Modifier.weight(1f))
                    val pct = (s.minutes * 100f / total).toInt()
                    Text("${s.minutes}m · $pct%", style = ClaudeType.Caption, color = colors.muted)
                }
            }
        }
    }
}

private fun parseHexSafe(hex: String): androidx.compose.ui.graphics.Color = try {
    val cleaned = hex.removePrefix("#")
    val argb = cleaned.toLong(16) or 0xFF000000L
    androidx.compose.ui.graphics.Color(argb.toInt())
} catch (_: Exception) {
    androidx.compose.ui.graphics.Color(0xFFCC785C.toInt())
}

@Composable
private fun RangeTabs(current: StatsRange, onChange: (StatsRange) -> Unit) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(ClaudeRadius.md))
            .background(colors.surfaceCard)
            .padding(2.dp)
    ) {
        StatsRange.values().forEach { r ->
            val selected = r == current
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(ClaudeRadius.sm))
                    .background(if (selected) colors.canvas else Color.Transparent)
                    .clickable { onChange(r) }
                    .padding(horizontal = ClaudeSpacing.md, vertical = ClaudeSpacing.xs)
            ) {
                Text(r.label, style = ClaudeType.Caption, color = if (selected) colors.ink else colors.muted)
            }
        }
    }
}

@Composable
private fun BarChartCard(daily: List<Int>, onOpenDay: (Int) -> Unit = {}) {
    val colors = MaterialTheme.appColors
    val selectedIdxState = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(-1)
    }
    val selectedIdx = selectedIdxState.value
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ClaudeRadius.lg))
            .background(colors.surfaceCard)
            .padding(ClaudeSpacing.lg)
    ) {
        Text("DAILY MINUTES", style = ClaudeType.CaptionUppercase, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.md))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .pointerInput(daily.size) {
                    detectTapGestures { offset ->
                        if (daily.isEmpty()) return@detectTapGestures
                        val gap = 4.dp.toPx()
                        val barW = (size.width - gap * (daily.size - 1)) / daily.size
                        val idx = (offset.x / (barW + gap)).toInt()
                        if (idx in daily.indices) {
                            // 第一次点：选中；第二次点同一格：跳详情
                            if (selectedIdxState.value == idx && daily[idx] > 0) {
                                onOpenDay(idx)
                            } else {
                                selectedIdxState.value = idx
                            }
                        }
                    }
                }
        ) {
            if (daily.isEmpty()) return@Canvas
            val maxMin = daily.max().coerceAtLeast(1)
            val gap = 4.dp.toPx()
            val barW = (size.width - gap * (daily.size - 1)) / daily.size
            daily.forEachIndexed { idx, m ->
                val h = (m.toFloat() / maxMin) * size.height
                val x = idx * (barW + gap)
                val y = size.height - h
                drawRect(
                    color = ClaudeColors.SurfaceCreamStrong,
                    topLeft = Offset(x, 0f),
                    size = Size(barW, size.height)
                )
                if (m > 0) {
                    drawRect(
                        color = if (idx == selectedIdx) ClaudeColors.PrimaryActive else ClaudeColors.Primary,
                        topLeft = Offset(x, y),
                        size = Size(barW, h)
                    )
                }
            }
        }
        if (selectedIdx >= 0 && selectedIdx < daily.size) {
            Spacer(Modifier.height(ClaudeSpacing.md))
            val daysAgo = daily.size - 1 - selectedIdx
            val label = when (daysAgo) {
                0 -> "今天"
                1 -> "昨天"
                else -> "${daysAgo} 天前"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$label · ${daily[selectedIdx]} 分钟",
                    style = ClaudeType.BodyMd,
                    color = colors.ink
                )
                if (daily[selectedIdx] > 0) {
                    Spacer(Modifier.fillMaxWidth(0.7f).height(0.dp))
                    Text(
                        "再点一次看详细 →",
                        style = ClaudeType.Caption,
                        color = ClaudeColors.Primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakCalendar(daily: List<Int>) {
    val colors = MaterialTheme.appColors
    if (daily.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ClaudeRadius.lg))
            .background(colors.surfaceCard)
            .padding(ClaudeSpacing.lg)
    ) {
        Text("STREAK", style = ClaudeType.CaptionUppercase, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.md))
        // 显示最近 30 天，按 7 列宽布局
        val show = daily.takeLast(30)
        val rows = show.chunked(7)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { m ->
                        val intensity = when {
                            m == 0 -> 0f
                            m < 25 -> 0.4f
                            m < 60 -> 0.7f
                            else -> 1f
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(ClaudeRadius.xs))
                                .background(
                                    if (intensity == 0f) colors.hairlineSoft
                                    else ClaudeColors.Primary.copy(alpha = intensity)
                                )
                        )
                    }
                }
            }
        }
    }
}
