package com.studybuddy.v2.ui.pet.saddle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType

/**
 * 鞍部猫专属"终端窗口"展示卡。
 *
 * 替代 PetScreen 默认的 cream-card + AppPetImage。组成：
 * - 顶部 24dp 标签栏：macOS 风 3 圆点 + 等宽小字 "claude — pet.sh"
 * - 主体上半：[SaddleCatSprite]
 * - 主体下半：[ClaudeTerminalLog]
 * - 底部 24dp 状态栏：1px hairline + 等宽小字 "> idle · mood 82"
 */
@Composable
fun SaddleCatTerminal(
    state: SaddleCatState,
    log: List<String>,
    modifier: Modifier = Modifier
) {
    // 入场：500ms 后从 alpha 0 + scale 0.92 渐入到 1.0，营造"鞍部猫降落到窝里"的感觉
    val alpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val scale = remember { androidx.compose.animation.core.Animatable(0.92f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(400)   // 等边缘鞍部猫先退场
        kotlinx.coroutines.coroutineScope {
            launch { alpha.animateTo(1f, androidx.compose.animation.core.tween(600)) }
            launch { scale.animateTo(1f, androidx.compose.animation.core.tween(600,
                easing = androidx.compose.animation.core.EaseOutBack)) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha.value
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(RoundedCornerShape(ClaudeRadius.lg))
            .background(ClaudeColors.SurfaceDark)
    ) {
        TerminalTitleBar()
        Spacer(Modifier.height(ClaudeSpacing.lg))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ClaudeSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            SaddleCatSprite(
                state = state,
                pixelSize = 6.dp,
                color = ClaudeColors.OnDark,
                bodyType = BodyType.FULL_BODY
            )
        }
        Spacer(Modifier.height(ClaudeSpacing.lg))
        ClaudeTerminalLog(
            lines = log,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ClaudeSpacing.md, vertical = ClaudeSpacing.xs)
                .height(80.dp)
        )
        TerminalStatusBar(state = state)
    }
}

@Composable
private fun TerminalTitleBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ClaudeSpacing.md, vertical = ClaudeSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Dot(ClaudeColors.Primary)
        Spacer(Modifier.width(6.dp))
        Dot(ClaudeColors.AccentAmber)
        Spacer(Modifier.width(6.dp))
        Dot(ClaudeColors.Success)
        Spacer(Modifier.width(ClaudeSpacing.md))
        Text(
            "claude — pet.sh",
            style = ClaudeType.Code,
            color = ClaudeColors.OnDarkSoft
        )
    }
}

@Composable
private fun Dot(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun TerminalStatusBar(state: SaddleCatState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ClaudeColors.HairlineSoft.copy(alpha = 0.12f))
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ClaudeSpacing.md, vertical = ClaudeSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "> ${state.pose.name.lowercase()} · mood ${state.mood.toInt()} · hunger ${state.hunger.toInt()}",
            style = ClaudeType.Code,
            color = ClaudeColors.OnDarkSoft
        )
    }
}
