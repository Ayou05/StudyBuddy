package com.studybuddy.v2.ui.social

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.AppButton
import com.studybuddy.v2.ui.component.AppCard
import com.studybuddy.v2.ui.component.LocalSnackbarController
import com.studybuddy.v2.ui.component.SnackbarTone

@Composable
fun MeScreen(
    onNavigateToSettings: () -> Unit = {},
    onLoggedOut: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToLedger: () -> Unit = {},
    onNavigateToQuote: () -> Unit = {},
    onNavigateToFund: () -> Unit = {},
    viewModel: MeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors
    val ctx = LocalContext.current
    val snackbar = LocalSnackbarController.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is MeEvent.LoggedOut -> {
                    snackbar.show("已退出登录", SnackbarTone.Default)
                    onLoggedOut()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ClaudeSpacing.pageHorizontal)
            .padding(top = ClaudeSpacing.xxl, bottom = ClaudeSpacing.xxl)
    ) {
        Text("ME", style = ClaudeType.CaptionUppercase, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.sm))
        ProfileHeader(
            nickname = state.me?.nickname?.takeIf { it.isNotBlank() } ?: "同学",
            email = state.me?.email,
            partnerName = state.partner?.nickname,
            streakDays = state.relationship?.streakDays ?: 0
        )

        Spacer(Modifier.height(ClaudeSpacing.xl))

        // 搭档卡 / 邀请码
        if (state.partner == null) {
            InviteCard(
                code = state.inviteCode,
                onGenerate = viewModel::regenerateInviteCode,
                onCopy = { code ->
                    val cb = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("invite", code))
                    snackbar.show("邀请码已复制", SnackbarTone.Success)
                }
            )
        } else {
            BoundPartnerCard(
                name = state.partner?.nickname.orEmpty(),
                streakDays = state.relationship?.streakDays ?: 0
            )
        }

        Spacer(Modifier.height(ClaudeSpacing.lg))

        // 常用入口 —— 从 Me 直达统计 / 账本 / 话廊 / 基金，不用绕回 Home
        Text("ENTRIES", style = ClaudeType.CaptionUppercase, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.sm))
        EntryRow(label = "统计", subtitle = "看每日 / 每周专注趋势", onClick = onNavigateToStats)
        EntryRow(label = "账本", subtitle = "工作日断连记录", onClick = onNavigateToLedger)
        EntryRow(label = "话廊", subtitle = "你和搭档的句子", onClick = onNavigateToQuote)
        EntryRow(label = "基金", subtitle = "共同储蓄与心愿", onClick = onNavigateToFund)

        Spacer(Modifier.height(ClaudeSpacing.lg))
        Text("SYSTEM", style = ClaudeType.CaptionUppercase, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.sm))
        // 设置入口
        EntryRow(label = "设置", subtitle = "偏好、外观、关于", onClick = onNavigateToSettings)

        Spacer(Modifier.height(ClaudeSpacing.xl))

        // 退出登录 —— text-link warning
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            AppButton.Text(
                text = "退出登录",
                onClick = { showLogoutDialog = true },
                color = colors.warning
            )
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            confirmButton = {
                AppButton.Text("确认退出", { showLogoutDialog = false; viewModel.logout() }, color = colors.warning)
            },
            dismissButton = {
                AppButton.Text("取消", { showLogoutDialog = false })
            },
            title = { Text("退出登录？", style = ClaudeType.TitleMd) },
            text = { Text("退出后会回到登录页。本机数据会保留。", style = ClaudeType.BodySm, color = colors.muted) },
            containerColor = colors.canvas,
            shape = RoundedCornerShape(ClaudeRadius.lg)
        )
    }
}

@Composable
private fun ProfileHeader(nickname: String, email: String?, partnerName: String?, streakDays: Int) {
    val colors = MaterialTheme.appColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.partnerA.copy(alpha = 0.15f))
                .border(2.dp, colors.partnerA, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                nickname.firstOrNull()?.uppercase() ?: "?",
                style = ClaudeType.DisplaySm,
                color = colors.partnerA
            )
        }
        Spacer(Modifier.size(ClaudeSpacing.md))
        Column {
            Text(nickname, style = ClaudeType.DisplaySm, color = colors.ink)
            Spacer(Modifier.height(ClaudeSpacing.xxs))
            Text(
                if (partnerName != null) "和 $partnerName 同行 $streakDays 天" else email.orEmpty(),
                style = ClaudeType.BodySm,
                color = colors.muted
            )
        }
    }
}

@Composable
private fun InviteCard(code: String?, onGenerate: () -> Unit, onCopy: (String) -> Unit) {
    val colors = MaterialTheme.appColors
    AppCard.Feature(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
        Column {
            Text("INVITE", style = ClaudeType.CaptionUppercase, color = colors.muted)
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Text("邀请你的搭档", style = ClaudeType.TitleMd, color = colors.ink)
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(
                "把这个 6 位邀请码发给 TA，TA 输入后就能和你一起。",
                style = ClaudeType.BodySm, color = colors.muted
            )
            Spacer(Modifier.height(ClaudeSpacing.md))
            if (code != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ClaudeRadius.md))
                        .background(colors.canvas)
                        .border(1.5.dp, ClaudeColors.Primary, RoundedCornerShape(ClaudeRadius.md))
                        .clickable { onCopy(code) }
                        .padding(vertical = ClaudeSpacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Text(code, style = ClaudeType.TimerMd, color = ClaudeColors.Primary)
                }
                Spacer(Modifier.height(ClaudeSpacing.xs))
                Text("点击复制 · 24 小时有效", style = ClaudeType.Caption, color = colors.muted)
            }
            Spacer(Modifier.height(ClaudeSpacing.md))
            AppButton.Primary(
                text = if (code == null) "生成邀请码" else "重新生成",
                onClick = onGenerate,
                fullWidth = true
            )
        }
    }
}

@Composable
private fun BoundPartnerCard(name: String, streakDays: Int) {
    val colors = MaterialTheme.appColors
    AppCard.Feature(modifier = Modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
        Column {
            Text("PARTNER", style = ClaudeType.CaptionUppercase, color = colors.muted)
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Text(name, style = ClaudeType.TitleMd, color = colors.ink)
            Spacer(Modifier.height(ClaudeSpacing.xxs))
            Text("一起走过 $streakDays 天", style = ClaudeType.BodySm, color = colors.muted)
        }
    }
}

@Composable
private fun EntryRow(label: String, subtitle: String, onClick: () -> Unit) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ClaudeRadius.md))
            .clickable(onClick = onClick)
            .padding(vertical = ClaudeSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = ClaudeType.TitleSm, color = colors.ink)
            Text(subtitle, style = ClaudeType.Caption, color = colors.muted)
        }
    }
}
