package com.studybuddy.v2.ui.mascot

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.pet.saddle.ClaudeIdleLogPool
import com.studybuddy.v2.ui.pet.saddle.Pose
import com.studybuddy.v2.ui.pet.saddle.SaddleCatState
import com.studybuddy.v2.ui.pet.saddle.SaddleCatTerminal
import kotlinx.coroutines.delay

/**
 * 鞍部猫独立全屏页（路由 `/mascot`）。
 *
 * 进入方式：从 [MascotDock] 双击鞍部猫触发。**不挂 BottomBar**——它是彩蛋秘境，
 * 不该混进主流功能。返回键 / 顶部 ✕ 按钮 退出。
 *
 * 页面结构：
 * - canvas 米色全屏背景
 * - 顶部一行小 caption + ✕ 关闭按钮
 * - 中央 [SaddleCatTerminal] 终端窗口（dark-mockup）
 * - 终端日志每 8s 自动 push 一行新假命令，3 行一组循环
 */
@Composable
fun MascotPage(onBack: () -> Unit) {
    val colors = MaterialTheme.appColors
    BackHandler { onBack() }

    var log by remember { mutableStateOf(ClaudeIdleLogPool.take(5)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(8000L)
            log = ClaudeIdleLogPool.take(5)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = ClaudeSpacing.pageHorizontal)
                .padding(top = ClaudeSpacing.lg, bottom = ClaudeSpacing.xxl)
        ) {
            // 顶栏：caption + ✕
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("MASCOT", style = ClaudeType.CaptionUppercase, color = colors.muted)
                    Spacer(Modifier.height(ClaudeSpacing.xs))
                    Text("鞍部猫", style = ClaudeType.DisplayMd, color = colors.ink)
                }
                Box(
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(ClaudeSpacing.sm)
                ) {
                    Text("✕", style = ClaudeType.TitleLg, color = colors.muted)
                }
            }
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(
                "彩蛋形象 · 它就那样，但它在",
                style = ClaudeType.BodyMd,
                color = colors.muted
            )
            Spacer(Modifier.height(ClaudeSpacing.xl))

            SaddleCatTerminal(
                state = SaddleCatState(pose = Pose.IDLE),
                log = log,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
