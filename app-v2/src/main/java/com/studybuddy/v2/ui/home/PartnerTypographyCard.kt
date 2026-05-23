package com.studybuddy.v2.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.data.model.RealtimeStatus
import com.studybuddy.v2.data.model.UserProfile
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.AppCard

// ═════════════════════════════════════════════════════════════════════════════
// PartnerTypographyCard ──「字号即数据」备选样式
//
// 设计意图：用衬线字号的强弱表达"谁今天更投入"。无任何图形元素 —— 完全靠
//   Claude 设计语言的杂志双栏对比。谁数字大字号就大，下面 caption 一行
//   交代状态。空间很满但呼吸感很好。
//
// 在 设置 → 主页样式 里可切换。
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun PartnerTypographyCard(
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
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                TypoColumn(
                    name = me?.nickname?.takeIf { it.isNotBlank() } ?: "你",
                    minutes = meTodayMin,
                    state = if (meIsFocusing) "专注中" else "在线",
                    dotColor = if (meIsFocusing) colors.partnerA else colors.success,
                    bigger = meTodayMin >= partnerTodayMin
                )
                Spacer(modifier = Modifier.width(ClaudeSpacing.lg))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(80.dp)
                        .background(colors.hairline)
                )
                Spacer(modifier = Modifier.width(ClaudeSpacing.lg))
                TypoColumn(
                    name = partner?.nickname?.takeIf { it.isNotBlank() } ?: "等搭档",
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
                    bigger = partner != null && partnerTodayMin > meTodayMin
                )
            }
        }
    }
}

@Composable
private fun TypoColumn(
    name: String,
    minutes: Int,
    state: String,
    dotColor: androidx.compose.ui.graphics.Color,
    bigger: Boolean,
    placeholder: Boolean = false
) {
    val colors = MaterialTheme.appColors
    Column(verticalArrangement = Arrangement.Top) {
        Text(name, style = ClaudeType.Caption, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.xs))
        // 衬线大数字 —— 谁数据大谁字号大（display-md vs display-sm）
        Text(
            text = if (placeholder) "—" else "$minutes",
            style = if (bigger) ClaudeType.DisplayMd else ClaudeType.DisplaySm,
            color = if (placeholder) colors.mutedSoft else colors.ink
        )
        if (!placeholder) {
            Text(
                "分钟",
                style = ClaudeType.Caption,
                color = colors.muted
            )
        }
        Spacer(Modifier.height(ClaudeSpacing.xs))
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
    }
}
