package com.studybuddy.v2.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.data.model.RealtimeStatus
import com.studybuddy.v2.data.model.UserProfile
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.AppCard
import kotlin.math.PI
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════════════════════
// PartnerWaveCard ──「同步呼吸」双色波形
//
// 设计意图：避开"俩头像面对面"的廉价感。两条正弦波从屏幕两端出发往中间汇合，
//   - 上波 partnerA（你），下波 partnerB（TA）
//   - 振幅 = 当日专注分钟数缩放（0 时退化成直线，越多越饱满，最大 1.0）
//   - 正在专注的那条 2.5s 缓慢呼吸（振幅 ±15%），其它静止
//   - 波形相位轻微错开，形成"两条线在彼此周围荡漾"的语义
//
// 类比：心电图 / Fitbit 睡眠图。比头像更"陪伴"，比柱状图更"温柔"。
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun PartnerWaveCard(
    me: UserProfile?,
    partner: UserProfile?,
    partnerStatus: RealtimeStatus?,
    meTodayMin: Int,
    partnerTodayMin: Int,
    meIsFocusing: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.appColors
    AppCard.Feature(modifier = modifier.fillMaxWidth(), padding = ClaudeSpacing.lg) {
        Column {
            Text(
                "TODAY · TOGETHER",
                style = ClaudeType.CaptionUppercase,
                color = colors.muted
            )
            Spacer(Modifier.height(ClaudeSpacing.md))
            DualWave(
                meAmplitude = amplitudeFromMinutes(meTodayMin),
                partnerAmplitude = amplitudeFromMinutes(partnerTodayMin),
                meBreathing = meIsFocusing,
                partnerBreathing = partnerStatus?.focusStatus == "ACTIVE",
                meColor = colors.partnerA,
                partnerColor = colors.partnerB
            )
            Spacer(Modifier.height(ClaudeSpacing.md))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WaveLegend(
                    label = me?.nickname?.takeIf { it.isNotBlank() } ?: "你",
                    minutes = meTodayMin,
                    state = if (meIsFocusing) "专注中" else "在线",
                    dotColor = if (meIsFocusing) colors.partnerA else colors.success,
                    modifier = Modifier
                )
                Spacer(Modifier.width(ClaudeSpacing.lg))
                Spacer(modifier = Modifier.weight(1f))
                WaveLegend(
                    label = partner?.nickname?.takeIf { it.isNotBlank() } ?: "等搭档",
                    minutes = partnerTodayMin,
                    state = when {
                        partner == null -> "未绑定"
                        partnerStatus?.focusStatus == "ACTIVE" -> "专注中"
                        partnerStatus?.online == true -> "在线"
                        else -> "离线"
                    },
                    dotColor = when {
                        partner == null -> colors.mutedSoft
                        partnerStatus?.focusStatus == "ACTIVE" -> colors.partnerB
                        partnerStatus?.online == true -> colors.success
                        else -> colors.mutedSoft
                    },
                    placeholder = partner == null,
                    alignEnd = true,
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
private fun WaveLegend(
    label: String,
    minutes: Int,
    state: String,
    dotColor: Color,
    modifier: Modifier,
    placeholder: Boolean = false,
    alignEnd: Boolean = false
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(ClaudeSpacing.xxs))
            Text(state, style = ClaudeType.Caption, color = colors.muted)
        }
        Spacer(Modifier.height(ClaudeSpacing.xxs))
        Text(label, style = ClaudeType.Caption, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.xxs))
        Text(
            if (placeholder) "—" else "${minutes}m",
            style = ClaudeType.TitleLg,
            color = if (placeholder) colors.mutedSoft else colors.ink
        )
    }
}

@Composable
private fun DualWave(
    meAmplitude: Float,
    partnerAmplitude: Float,
    meBreathing: Boolean,
    partnerBreathing: Boolean,
    meColor: Color,
    partnerColor: Color
) {
    val transition = rememberInfiniteTransition(label = "wave")
    // 全局相位（缓慢流动 12s 一周期）
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    // 呼吸（专注中那条 2.5s ±15% 振幅缩放）
    val breath by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    val effectiveMeAmp = meAmplitude * if (meBreathing) breath else 1f
    val effectivePartnerAmp = partnerAmplitude * if (partnerBreathing) breath else 1f

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f
        val maxAmp = h * 0.32f      // 最大振幅 = 区域 32%
        val strokeW = 2.dp.toPx()

        // 上波（你）— 从左侧入场，相位 phase，正向
        drawWave(
            color = meColor,
            yCenter = midY - h * 0.05f,
            amplitude = maxAmp * effectiveMeAmp,
            phase = phase,
            width = w,
            strokeWidth = strokeW
        )
        // 下波（TA）— 从右侧入场（相位偏 PI），反向，让两线交错呼应
        drawWave(
            color = partnerColor,
            yCenter = midY + h * 0.05f,
            amplitude = maxAmp * effectivePartnerAmp,
            phase = phase + PI.toFloat(),
            width = w,
            strokeWidth = strokeW
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWave(
    color: Color,
    yCenter: Float,
    amplitude: Float,
    phase: Float,
    width: Float,
    strokeWidth: Float
) {
    if (amplitude <= 0.5f) {
        // 几乎无数据时退化成水平线（极淡）
        drawLine(
            color = color.copy(alpha = 0.45f),
            start = Offset(0f, yCenter),
            end = Offset(width, yCenter),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        return
    }
    val path = Path()
    val step = 4f                          // 4px 一段，平滑且不耗
    val cycles = 2.4f                      // 整张卡显示约 2.4 个完整正弦
    val k = (cycles * 2 * PI / width).toFloat()
    var x = 0f
    var first = true
    while (x <= width) {
        val y = yCenter + amplitude * sin(k * x + phase)
        if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
        x += step
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

/**
 * 专注分钟数 → 振幅比例 (0..1)。
 * 25min（一颗番茄）= 0.45；50min = 0.7；90min+ = 1.0；
 * 用 sqrt 缓和增长，让低数据也能有一点波形。
 */
private fun amplitudeFromMinutes(minutes: Int): Float {
    if (minutes <= 0) return 0f
    val ratio = (minutes / 90f).coerceAtMost(1f)
    return kotlin.math.sqrt(ratio)
}
