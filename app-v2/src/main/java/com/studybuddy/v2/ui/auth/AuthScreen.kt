package com.studybuddy.v2.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.AppButton
import com.studybuddy.v2.ui.component.AppIcon

/**
 * 登录 / 注册（双 mode 共用一个 Screen + ViewModel）。
 *
 * 设计语言：cream canvas + 衬线大标题 + hairline 包裹的输入框 + coral 主 CTA。
 * 错误状态用 warning 黄色 inline 文字（白皮书要求避免抖动 dialog）。
 */
@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors

    LaunchedEffect(Unit) {
        viewModel.events.collect { e -> if (e is AuthEvent.LoggedIn) onAuthenticated() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ClaudeSpacing.pageHorizontal)
                .padding(top = 80.dp, bottom = ClaudeSpacing.xxl),
            horizontalAlignment = Alignment.Start
        ) {
            // 顶部品牌区 — 标题用 display-md 衬线，副标题 body-md
            Text("StudyBuddy", style = ClaudeType.CaptionUppercase, color = colors.muted)
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Text(
                if (state.mode == AuthMode.LOGIN) "欢迎回来。" else "开始你的专注。",
                style = ClaudeType.DisplayLg,
                color = colors.ink
            )
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(
                if (state.mode == AuthMode.LOGIN) "和搭档一起，把今天的注意力交给一件事。"
                else "用邮箱注册，再绑定你的搭档。",
                style = ClaudeType.BodyMd,
                color = colors.muted
            )

            Spacer(Modifier.height(ClaudeSpacing.xl))

            // 表单 — 衬线副标题 + 输入框列
            if (state.mode == AuthMode.REGISTER) {
                FormField(
                    label = "昵称",
                    value = state.nickname,
                    onValueChange = viewModel::onNicknameChange,
                    placeholder = "起个名字"
                )
                Spacer(Modifier.height(ClaudeSpacing.md))
            }
            FormField(
                label = "邮箱",
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                placeholder = "you@example.com",
                keyboardType = KeyboardType.Email
            )
            Spacer(Modifier.height(ClaudeSpacing.md))
            FormField(
                label = "密码",
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = "至少 8 位",
                keyboardType = KeyboardType.Password,
                isPassword = true
            )

            // 错误提示 — 只在有错误时显示，不抖动 / 不弹窗
            if (state.errorMessage != null) {
                Spacer(Modifier.height(ClaudeSpacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon("close", size = 14.dp, tint = ClaudeColors.Error)
                    Spacer(Modifier.width(ClaudeSpacing.xxs))
                    Text(state.errorMessage!!, style = ClaudeType.BodySm, color = ClaudeColors.Error)
                }
            }

            Spacer(Modifier.height(ClaudeSpacing.xl))

            AppButton.Primary(
                text = when {
                    state.loading && state.mode == AuthMode.LOGIN -> "登录中…"
                    state.loading -> "注册中…"
                    state.mode == AuthMode.LOGIN -> "登录"
                    else -> "创建账号"
                },
                onClick = viewModel::submit,
                fullWidth = true,
                enabled = !state.loading
            )

            Spacer(Modifier.height(ClaudeSpacing.md))

            // 切换 mode 的文字 link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (state.mode == AuthMode.LOGIN) "还没有账号？" else "已经有账号？",
                    style = ClaudeType.BodySm,
                    color = colors.muted
                )
                Spacer(Modifier.width(ClaudeSpacing.xxs))
                AppButton.Text(
                    text = if (state.mode == AuthMode.LOGIN) "注册" else "登录",
                    onClick = {
                        viewModel.setMode(if (state.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN)
                    }
                )
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    val colors = MaterialTheme.appColors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = ClaudeType.Caption, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.xxs))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(ClaudeColors.Primary),
            textStyle = ClaudeType.BodyMd.copy(color = colors.ink),
            interactionSource = interaction,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(ClaudeRadius.md))
                .background(colors.canvas)
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = if (focused) ClaudeColors.Primary else colors.hairline,
                    shape = RoundedCornerShape(ClaudeRadius.md)
                )
                .padding(horizontal = ClaudeSpacing.md),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = ClaudeType.BodyMd, color = colors.mutedSoft)
                    }
                    inner()
                }
            }
        )
    }
}
