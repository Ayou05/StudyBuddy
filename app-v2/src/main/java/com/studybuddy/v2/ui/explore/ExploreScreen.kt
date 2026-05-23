package com.studybuddy.v2.ui.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.data.model.Landmark
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.util.Mode

/**
 * 探索 tab —— 按姿态自适应。
 *
 * 主入口（按 ExploreUiState.pose.mode 切换）：
 * - FOCUSED：常驻地标管理 + 加成历史
 * - TOGETHER：双人位置 + 共享地标 + 回忆钩子（P5 占位，P6 接共享地标完整）
 * - AWAY：你一个人远离常驻 + 寄飞机入口
 * - LONG_DISTANCE：异地双人地图 + 见面统计
 * - NEUTRAL：fallback 视图
 *
 * 切换动画：fadeOut + slideUp 200ms → fadeIn + slideUp 300ms（错位避免空白）
 */
@Composable
fun ExploreScreen(viewModel: ExploreViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(horizontal = ClaudeSpacing.pageHorizontal)
            .padding(top = ClaudeSpacing.xxl)
    ) {
        Text("EXPLORE", style = ClaudeType.CaptionUppercase, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.sm))

        val pose = state.pose
        val title = when (pose?.mode) {
            Mode.FOCUSED -> "在常驻地"
            Mode.TOGETHER -> "你们离得很近"
            Mode.AWAY -> "你今天出门了"
            Mode.LONG_DISTANCE -> "你们相距 ${pose.partnerDistanceKm?.toInt() ?: 0}km"
            Mode.NEUTRAL, null -> "外面的世界"
        }
        Text(title, style = ClaudeType.DisplayMd, color = colors.ink)
        Spacer(Modifier.height(ClaudeSpacing.xs))
        Text(
            pose?.displayCaption() ?: "正在感知…",
            style = ClaudeType.BodyMd,
            color = colors.muted
        )

        Spacer(Modifier.height(ClaudeSpacing.lg))

        // 切换动画：旧形态 fadeOut + slideUp，新形态 fadeIn + slideUp
        AnimatedContent(
            targetState = pose?.mode ?: Mode.NEUTRAL,
            transitionSpec = {
                (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 8 }) togetherWith
                    (fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 8 })
            },
            label = "exploreMode"
        ) { mode ->
            when (mode) {
                Mode.FOCUSED -> FocusedView(state.myLandmarks)
                Mode.TOGETHER -> TogetherView(partnerName = state.partner?.nickname ?: "TA", pose = pose)
                Mode.AWAY -> AwayView(partnerName = state.partner?.nickname ?: "TA", pose = pose)
                Mode.LONG_DISTANCE -> LongDistanceView(partnerName = state.partner?.nickname ?: "TA", pose = pose)
                Mode.NEUTRAL -> NeutralView()
            }
        }
    }
}

@Composable
private fun FocusedView(landmarks: List<Landmark>) {
    val colors = MaterialTheme.appColors
    if (landmarks.isEmpty()) {
        Column {
            EmptyTipCard("还没有标过常驻地", "在地图长按 → 把家 / 学校 / 出租屋记下来")
            Spacer(Modifier.height(ClaudeSpacing.md))
            AddLandmarkPlaceholder()
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(ClaudeSpacing.sm)) {
        items(landmarks, key = { it.id }) { lm ->
            LandmarkCard(lm)
        }
        item { AddLandmarkPlaceholder() }
    }
}

@Composable
private fun TogetherView(partnerName: String, pose: com.studybuddy.v2.util.PoseResult?) {
    val dist = pose?.partnerDistanceKm
    val distLabel = if (dist != null && dist < 1.0) "距离 ${(dist * 1000).toInt()}m"
                    else "距离 ${dist?.let { "%.1f".format(it) } ?: "?"}km"
    InfoCard {
        Column {
            Text("你们在一起", style = ClaudeType.TitleMd, color = MaterialTheme.appColors.ink)
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(distLabel, style = ClaudeType.BodyMd, color = MaterialTheme.appColors.muted)
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Text(
                "(P6 上线后会在这里显示一张极简地图 + 共享地标回忆)",
                style = ClaudeType.Caption,
                color = MaterialTheme.appColors.muted
            )
        }
    }
}

@Composable
private fun AwayView(partnerName: String, pose: com.studybuddy.v2.util.PoseResult?) {
    val dist = pose?.partnerDistanceKm
    InfoCard {
        Column {
            Text("你今天出门了", style = ClaudeType.TitleMd, color = MaterialTheme.appColors.ink)
            Spacer(Modifier.height(ClaudeSpacing.xs))
            if (dist != null) {
                Text("距离 $partnerName ${dist.toInt()}km",
                    style = ClaudeType.BodyMd, color = MaterialTheme.appColors.muted)
            }
            Spacer(Modifier.height(ClaudeSpacing.md))
            Text(
                "想 TA 了就去信件 tab 寄一架飞机 ✈",
                style = ClaudeType.BodyMd,
                color = ClaudeColors.Primary
            )
        }
    }
}

@Composable
private fun LongDistanceView(partnerName: String, pose: com.studybuddy.v2.util.PoseResult?) {
    val dist = pose?.partnerDistanceKm?.toInt() ?: 0
    InfoCard {
        Column {
            Text("你们相距 ${dist}km", style = ClaudeType.TitleMd, color = MaterialTheme.appColors.ink)
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text("但是 app 在替你们数着每一天",
                style = ClaudeType.BodyMd, color = MaterialTheme.appColors.muted)
            Spacer(Modifier.height(ClaudeSpacing.md))
            Text(
                "(P6 上线后这里会显示见面记录 + 累计在一起的小时数)",
                style = ClaudeType.Caption,
                color = MaterialTheme.appColors.muted
            )
        }
    }
}

@Composable
private fun NeutralView() {
    InfoCard {
        Column {
            Text("外面的世界", style = ClaudeType.TitleMd, color = MaterialTheme.appColors.ink)
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text("还没有足够的信号判断今天的氛围",
                style = ClaudeType.BodyMd, color = MaterialTheme.appColors.muted)
        }
    }
}

@Composable
private fun LandmarkCard(landmark: Landmark) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ClaudeRadius.md))
            .background(colors.surfaceCard)
            .padding(ClaudeSpacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(landmark.name.ifBlank { "未命名地标" },
                    style = ClaudeType.TitleSm, color = colors.ink)
                Text(landmark.type, style = ClaudeType.Caption, color = colors.muted)
            }
            Text("半径 ${landmark.radiusM}m",
                style = ClaudeType.Caption, color = colors.muted)
        }
    }
}

@Composable
private fun AddLandmarkPlaceholder() {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ClaudeRadius.md))
            .background(colors.canvas)
            .padding(ClaudeSpacing.md)
    ) {
        Text("+ 在地图长按添加新地标", style = ClaudeType.Caption,
            color = ClaudeColors.Primary)
    }
}

@Composable
private fun EmptyTipCard(title: String, body: String) {
    InfoCard {
        Column {
            Text(title, style = ClaudeType.TitleSm, color = MaterialTheme.appColors.ink)
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(body, style = ClaudeType.BodyMd, color = MaterialTheme.appColors.muted)
        }
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ClaudeRadius.lg))
            .background(colors.surfaceCard)
            .padding(ClaudeSpacing.lg)
    ) { content() }
}
