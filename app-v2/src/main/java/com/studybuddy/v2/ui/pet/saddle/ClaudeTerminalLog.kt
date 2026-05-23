package com.studybuddy.v2.ui.pet.saddle

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeType

/**
 * 假终端命令日志 —— 滚动 5 行历史。
 *
 * 调用方传入 lines 列表。最新一行显示得最亮（OnDark），历史行往上越淡。
 */
@Composable
fun ClaudeTerminalLog(
    lines: List<String>,
    modifier: Modifier = Modifier
) {
    val display = lines.takeLast(5)
    Column(modifier = modifier) {
        display.forEachIndexed { idx, line ->
            val alpha = 0.4f + (idx.toFloat() / display.size.coerceAtLeast(1)) * 0.6f
            Text(
                text = line,
                style = ClaudeType.Code,
                color = ClaudeColors.OnDark.copy(alpha = alpha)
            )
        }
    }
}

/**
 * 默认 idle 时的"自言自语"行池，TYPING 状态下随机循环。
 */
val ClaudeIdleLogPool = listOf(
    "> tail -f study.log",
    "> [ok] mood synced",
    "> sudo make focus",
    "> compiling focus.k...",
    "> $ pet saddle-cat",
    "> [warn] hunger < 30",
    "> syscall: meow()",
    "> zsh: command not found: distract",
    "> reading /proc/calm/now",
    "> [ok] partner online",
    "> grep -r '专注' .",
    "> echo \"在的\""
)
