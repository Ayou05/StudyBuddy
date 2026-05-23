package com.studybuddy.v2.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * AppSnackbar —— Claude 设计语言的 dark-mockup 风格全局提示。
 *
 * 用法：
 *   1. V2NavGraph 顶层包一层 SnackbarHost
 *   2. 任何 Composable / ViewModel 通过 LocalSnackbarController.current.show("xxx") 触发
 *   3. 4 秒自动隐藏，最多排队 1 条（新消息覆盖旧消息）
 */
data class SnackbarMessage(
    val text: String,
    val tone: SnackbarTone = SnackbarTone.Default,
    val durationMs: Long = 3000L
)

enum class SnackbarTone { Default, Warning, Success }

class SnackbarController(
    private val scope: CoroutineScope = MainScope()
) {
    private val channel = Channel<SnackbarMessage>(capacity = Channel.CONFLATED)

    fun show(text: String, tone: SnackbarTone = SnackbarTone.Default) {
        scope.launch { channel.send(SnackbarMessage(text, tone)) }
    }

    fun show(message: SnackbarMessage) {
        scope.launch { channel.send(message) }
    }

    internal val flow = channel.receiveAsFlow()
}

val LocalSnackbarController = compositionLocalOf<SnackbarController> {
    error("SnackbarController not provided. Wrap content with SnackbarHost.")
}

@Composable
fun SnackbarHost(controller: SnackbarController, content: @Composable () -> Unit) {
    var current by remember { mutableStateOf<SnackbarMessage?>(null) }
    LaunchedEffect(controller) {
        controller.flow.collect { msg ->
            current = msg
            kotlinx.coroutines.delay(msg.durationMs)
            // 仅当未被新消息覆盖时清空
            if (current === msg) current = null
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        content()

        AnimatedVisibility(
            visible = current != null,
            enter = fadeIn(tween(200)) + slideInVertically(tween(220)) { it / 2 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            current?.let { msg -> SnackbarBubble(msg, onDismiss = { current = null }) }
        }
    }
}

@Composable
private fun SnackbarBubble(message: SnackbarMessage, onDismiss: () -> Unit) {
    val accent = when (message.tone) {
        SnackbarTone.Default -> ClaudeColors.Primary
        SnackbarTone.Warning -> ClaudeColors.Warning
        SnackbarTone.Success -> ClaudeColors.Success
    }
    GlassPresets.Dark(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = ClaudeSpacing.lg, vertical = ClaudeSpacing.md)
            .widthIn(max = 480.dp)
            .clickable { onDismiss() },
        shape = RoundedCornerShape(ClaudeRadius.lg)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = ClaudeSpacing.lg, vertical = ClaudeSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // 左侧 4dp 强调色细条
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
                    .padding(vertical = 2.dp)
            )
            Spacer(Modifier.width(ClaudeSpacing.sm))
            Text(
                text = message.text,
                style = ClaudeType.BodySm,
                color = ClaudeColors.OnDark
            )
        }
    }
}
