package com.studybuddy.v2.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors

/** 二级页统一的左上返回行。hairline 触摸区 + 左箭头 + 文字。 */
@Composable
fun BackRow(label: String = "返回", onBack: () -> Unit) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(ClaudeRadius.md))
            .clickable { onBack() }
            .padding(vertical = ClaudeSpacing.xs, horizontal = ClaudeSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(name = "arrow_left", size = 18.dp, tint = colors.muted)
        Spacer(Modifier.width(ClaudeSpacing.xs))
        Text(label, style = ClaudeType.BodySm, color = colors.muted)
    }
}
