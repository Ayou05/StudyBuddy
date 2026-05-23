package com.studybuddy.v2.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.data.model.RealtimeStatus
import com.studybuddy.v2.data.model.UserProfile
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.AppButton
import com.studybuddy.v2.ui.component.AppCard
import com.studybuddy.v2.ui.component.AppIcon
import com.studybuddy.v2.ui.component.mascotAvoid
import kotlinx.coroutines.launch

/**
 * 主页 —— 严格走 Claude 三层节奏：
 *   canvas hero 问候 → dark-mockup 今日专注卡 → cream-card 双人状态 → 4 connector-tile
 *   未开始今日专注时，底部加一张 coral-callout
 *
 * 不出现 emoji、不出现非 token 颜色、不出现 PlatformTextStyle。
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onStartFocus: () -> Unit = {},
    onStartSyncFocus: () -> Unit = {},
    onOpenFund: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenNoteWall: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    // 每次回到前台 / 重新入栈时刷新数据
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showSetupSheet by remember { mutableStateOf(false) }
    if (showSetupSheet) {
        FocusSetupSheet(
            currentMode = state.focusMode,
            currentDurationMin = state.focusDurationMin,
            onDismiss = { showSetupSheet = false },
            onConfirm = { mode, dur -> viewModel.saveFocusPreference(mode, dur) }
        )
    }

    // 收到搭档邀请 → 弹 SyncInviteSheet
    val incomingInvite = state.incomingInvite
    if (incomingInvite != null) {
        SyncInviteSheet(
            invite = incomingInvite,
            fromNickname = state.partner?.nickname ?: "TA",
            onAccept = {
                viewModel.acceptInvite()
                onStartSyncFocus()
            },
            onDecline = { viewModel.declineInvite() }
        )
    }

    // 页面入场 92% scale + alpha 0 → 1, 500ms EaseOut
    val entryScale = remember { Animatable(0.96f) }
    val entryAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entryScale.animateTo(1f, tween(500, easing = EaseOutCubic))
    }
    LaunchedEffect(Unit) {
        entryAlpha.animateTo(1f, tween(500, easing = EaseOutCubic))
    }

    val scrollState = rememberScrollState()

    val homePagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = 0) { 2 }
    val homeScope = androidx.compose.runtime.rememberCoroutineScope()

    androidx.compose.foundation.pager.HorizontalPager(
        state = homePagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
    when (page) {
        1 -> com.studybuddy.v2.ui.quote.QuoteScreen(onBack = {
            homeScope.launch { homePagerState.animateScrollToPage(0) }
        })
        else -> Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .scale(entryScale.value)
            .alpha(entryAlpha.value)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = ClaudeSpacing.pageHorizontal)
                .padding(top = ClaudeSpacing.xxl, bottom = ClaudeSpacing.xxl)
        ) {
            // 顶栏：右上角小齿轮 → Settings 全屏（Me 已合并到 Settings）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Greeting(nickname = state.nickname)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .clickable { onOpenSettings() },
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    com.studybuddy.v2.ui.component.AppIcon(
                        name = "settings",
                        size = 22.dp,
                        tint = MaterialTheme.appColors.muted
                    )
                }
            }
            // 姿态视觉锚 —— 12sp 灰字，用户隐约感知"app 在跟随我"
            if (state.poseCaption.isNotBlank()) {
                Spacer(Modifier.height(ClaudeSpacing.xs))
                Text(
                    state.poseCaption,
                    style = ClaudeType.Caption,
                    color = MaterialTheme.appColors.muted
                )
            }
            Spacer(Modifier.height(ClaudeSpacing.xl))

            // 今天专注什么 —— 横滑切主题（鞍部风铭牌主区版）
            com.studybuddy.v2.ui.topic.TodayTopicStrip()

            Spacer(Modifier.height(ClaudeSpacing.xl))

            TodayFocusCard(
                todayMinutes = state.todayMinutes,
                isActive = state.activeSession != null,
                focusMode = state.focusMode,
                focusDurationMin = state.focusDurationMin,
                onTap = onStartFocus,
                onConfigClick = { showSetupSheet = true }
            )
            Spacer(Modifier.height(ClaudeSpacing.lg))
            // 双人卡 —— WAVE 默认（同步呼吸双色波）/ TYPOGRAPHY 备选（字号即数据）
            if (state.partnerWidgetStyle == "TYPOGRAPHY") {
                PartnerTypographyCard(
                    me = state.me,
                    partner = state.partner,
                    partnerStatus = state.partnerStatus,
                    meTodayMin = state.todayMinutes,
                    partnerTodayMin = state.partnerTodayMinutes,
                    meIsFocusing = state.activeSession != null
                )
            } else {
                PartnerWaveCard(
                    me = state.me,
                    partner = state.partner,
                    partnerStatus = state.partnerStatus,
                    meTodayMin = state.todayMinutes,
                    partnerTodayMin = state.partnerTodayMinutes,
                    meIsFocusing = state.activeSession != null
                )
            }
            Spacer(Modifier.height(ClaudeSpacing.lg))
            ConnectorGrid(
                onFund = onOpenFund,
                onStats = onOpenStats,
                onHistory = onOpenHistory,
                onSettings = onOpenSettings
            )
            Spacer(Modifier.height(ClaudeSpacing.xl))
        }

        // 情境层浮卡（事件驱动，向下滑关）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(androidx.compose.ui.Alignment.TopCenter)
        ) {
            com.studybuddy.v2.ui.moment.MomentBanner(
                moment = state.currentMoment,
                onAction = { m ->
                    // 路由由 Banner 触发：见面/停留 → 便签墙；TA 开始 → 同步专注；断连 → 金库
                    when (m) {
                        is com.studybuddy.v2.data.moment.Moment.MeetingStarted,
                        is com.studybuddy.v2.data.moment.Moment.MeetingEnded,
                        is com.studybuddy.v2.data.moment.Moment.StayDetected -> onOpenNoteWall()
                        is com.studybuddy.v2.data.moment.Moment.PartnerStartedFocus -> onStartSyncFocus()
                        is com.studybuddy.v2.data.moment.Moment.WeekdayBreakNoticed -> onOpenFund()
                    }
                    viewModel.dismissMoment()
                },
                onDismiss = viewModel::dismissMoment
            )
        }
    }
    }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 1. Greeting —— 衬线 display-md "Hi, 禾木。"  + body-md 副标
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun Greeting(nickname: String) {
    val colors = MaterialTheme.appColors
    var now by remember { mutableStateOf(java.util.Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = java.util.Calendar.getInstance()
            kotlinx.coroutines.delay(60_000L)
        }
    }
    val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
    val greet = when (hour) {
        in 5..10 -> "早上好"
        in 11..12 -> "中午好"
        in 13..17 -> "下午好"
        in 18..23 -> "晚上好"
        else -> "凌晨好"
    }
    // 自然语言时间感 —— 不重复系统状态栏的精确时间，但暗示"现在是这个点"
    val timeHint = when (hour) {
        in 5..7 -> "天刚亮"
        in 8..10 -> "上午时光"
        in 11..12 -> "临近正午"
        in 13..14 -> "午后"
        in 15..17 -> "傍晚将至"
        in 18..19 -> "暮色四合"
        in 20..21 -> "夜色温柔"
        in 22..23 -> "夜深了"
        in 0..2 -> "深夜"
        else -> "黎明前"
    }
    val name = nickname.takeIf { it.isNotBlank() } ?: "同学"

    Column {
        Text(
            "$greet，$name。",
            style = ClaudeType.DisplayLg,
            color = colors.ink,
            modifier = Modifier.mascotAvoid()
        )
        Spacer(Modifier.height(ClaudeSpacing.xs))
        Text(
            "$timeHint · 今天也要专注呀。",
            style = ClaudeType.BodyMd,
            color = colors.muted,
            modifier = Modifier.mascotAvoid()
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. TodayFocusCard —— Claude code-window-card 风格的 dark mockup
//    顶部 caption 标签 + 中央 JetBrains Mono 大数字 + 主按钮（直进 Focus）
//    + 下方一行 muted caption 配置入口（"倒计时 · 25 分钟 →" 整行可点）
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun TodayFocusCard(
    todayMinutes: Int,
    isActive: Boolean,
    focusMode: String,
    focusDurationMin: Int,
    onTap: () -> Unit,
    onConfigClick: () -> Unit
) {
    val colors = MaterialTheme.appColors
    AppCard.Dark(
        modifier = Modifier.fillMaxWidth(),
        padding = ClaudeSpacing.lg
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "TODAY · FOCUS",
                    style = ClaudeType.CaptionUppercase,
                    color = colors.onDarkSoft
                )
                Spacer(Modifier.weight(1f))
                if (isActive) {
                    LiveDot(color = ClaudeColors.Primary)
                    Spacer(Modifier.width(ClaudeSpacing.xxs))
                    Text("正在专注", style = ClaudeType.Caption, color = ClaudeColors.Primary)
                }
            }
            Spacer(Modifier.height(ClaudeSpacing.md))
            Text(
                formatHmm(todayMinutes),
                style = ClaudeType.TimerLg,
                color = colors.onDark
            )
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(
                if (todayMinutes == 0) "把今天的注意力交给一件事。"
                else "已经投入了 ${todayMinutes} 分钟。继续。",
                style = ClaudeType.BodySm,
                color = colors.onDarkSoft
            )
            Spacer(Modifier.height(ClaudeSpacing.lg))
            // 按钮放在 dark 卡里 —— 用 Primary（coral）填充 + on-primary 白字
            AppButton.Primary(
                text = if (isActive) "回到专注" else "开始专注",
                leadingIcon = if (isActive) "arrow_right" else "play",
                onClick = onTap,
                fullWidth = true
            )
            // ── 配置入口：muted caption 行，整行可点弹 SetupSheet ──
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ClaudeRadius.sm))
                    .clickable(onClick = onConfigClick)
                    .padding(vertical = ClaudeSpacing.xs),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        focusModeSummary(focusMode, focusDurationMin),
                        style = ClaudeType.Caption,
                        color = colors.onDarkSoft
                    )
                    Spacer(Modifier.width(ClaudeSpacing.xxs))
                    AppIcon(
                        name = "arrow_right",
                        size = 12.dp,
                        tint = colors.onDarkSoft
                    )
                }
            }
        }
    }
}

private fun focusModeSummary(mode: String, durationMin: Int): String = when (mode) {
    "STOPWATCH" -> "正计时"
    else -> "倒计时 · $durationMin 分钟"
}

@Composable
private fun LiveDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. PartnerCard 已被 PartnerWaveCard / PartnerTypographyCard 取代
//    （见 PartnerWaveCard.kt / PartnerTypographyCard.kt）
// ═════════════════════════════════════════════════════════════════════════════

// ═════════════════════════════════════════════════════════════════════════════
// 4. ConnectorGrid —— 2x2 connector-tile（cream-card 小卡 + 12dp 圆角 + 图标 + 标题）
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun ConnectorGrid(
    onFund: () -> Unit,
    onStats: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(ClaudeSpacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(ClaudeSpacing.sm)) {
            ConnectorTile("wallet", "金库", "一起攒", onFund, Modifier.weight(1f))
            ConnectorTile("chart", "数据统计", "详细报告", onStats, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ClaudeSpacing.sm)) {
            ConnectorTile("history", "专注历史", "回顾记录", onHistory, Modifier.weight(1f))
            ConnectorTile("settings", "设置", "偏好配置", onSettings, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ConnectorTile(
    iconName: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(ClaudeRadius.lg))
            .background(colors.canvas)
            .border(1.dp, colors.hairline, RoundedCornerShape(ClaudeRadius.lg))
            .clickable(onClick = onClick)
            .padding(ClaudeSpacing.md)
    ) {
        Column {
            AppIcon(name = iconName, size = 22.dp, tint = ClaudeColors.Primary)
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Text(title, style = ClaudeType.TitleSm, color = colors.ink)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = ClaudeType.Caption, color = colors.muted)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 5. PartnerPresenceCard —— 替换原冗余 coral callout
//    四态：未绑定 / 已绑定离线 / 已绑定空闲 / 已绑定专注中
//    避免与顶部主 CTA 重复，用"陪伴感信息"填空间。
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun PartnerPresenceCard(state: HomeUiState, onStartFocus: () -> Unit) {
    val partner = state.partner
    val ps = state.partnerStatus

    when {
        partner == null -> UnboundPartnerCard()
        ps?.focusStatus == "ACTIVE" -> PartnerFocusingCard(
            partnerName = partner.nickname.ifBlank { "搭档" },
            currentSec = ps.currentFocusSeconds,
            onJoin = onStartFocus
        )
        ps?.online == true -> PartnerIdleCard(
            partnerName = partner.nickname.ifBlank { "搭档" },
            todayMin = state.partnerTodayMinutes
        )
        else -> PartnerOfflineCard(partnerName = partner.nickname.ifBlank { "搭档" })
    }
}

@Composable
private fun UnboundPartnerCard() {
    val colors = MaterialTheme.appColors
    AppCard.Outline(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
        Column {
            Text("一个人也很好。", style = ClaudeType.TitleMd, color = colors.ink)
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(
                "绑定一个搭档之后，你们的蛋会从专注里慢慢孵化。",
                style = ClaudeType.BodySm,
                color = colors.muted
            )
        }
    }
}

@Composable
private fun PartnerOfflineCard(partnerName: String) {
    val colors = MaterialTheme.appColors
    AppCard.Outline(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.mutedSoft)
                )
                Spacer(Modifier.width(ClaudeSpacing.xs))
                Text("$partnerName 暂时离线", style = ClaudeType.Caption, color = colors.muted)
            }
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(
                "TA 不在的时候，你的专注会先记下来。",
                style = ClaudeType.BodySm,
                color = colors.muted
            )
        }
    }
}

@Composable
private fun PartnerIdleCard(partnerName: String, todayMin: Int) {
    val colors = MaterialTheme.appColors
    AppCard.Feature(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.success)
                )
                Spacer(Modifier.width(ClaudeSpacing.xs))
                Text("$partnerName 在线", style = ClaudeType.Caption, color = colors.muted)
            }
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(
                if (todayMin == 0) "TA 今天还没开始。"
                else "TA 今天专注了 ${todayMin} 分钟。",
                style = ClaudeType.TitleMd, color = colors.ink
            )
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text("一会儿可能会和你一起。", style = ClaudeType.BodySm, color = colors.muted)
        }
    }
}

@Composable
private fun PartnerFocusingCard(partnerName: String, currentSec: Long, onJoin: () -> Unit) {
    val mm = currentSec / 60
    val ss = currentSec % 60
    AppCard.Coral(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ClaudeColors.OnPrimary)
                )
                Spacer(Modifier.width(ClaudeSpacing.xs))
                Text(
                    "$partnerName 正在专注",
                    style = ClaudeType.Caption,
                    color = ClaudeColors.OnPrimary.copy(alpha = 0.85f)
                )
            }
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text("%02d:%02d".format(mm, ss), style = ClaudeType.TimerMd, color = ClaudeColors.OnPrimary)
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(
                "和 TA 一起，会更专注。",
                style = ClaudeType.BodySm,
                color = ClaudeColors.OnPrimary.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(ClaudeSpacing.md))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(ClaudeRadius.md))
                    .background(ClaudeColors.Canvas)
                    .clickable(onClick = onJoin)
                    .padding(horizontal = ClaudeSpacing.lg, vertical = ClaudeSpacing.sm)
            ) {
                Text("加入 TA", style = ClaudeType.Button, color = ClaudeColors.Ink)
            }
        }
    }
}

// ─── helpers ─────────────────────────────────────────────────────────────────
private fun formatHmm(min: Int): String {
    val h = min / 60
    val m = min % 60
    return "%02d:%02d".format(h, m)
}

