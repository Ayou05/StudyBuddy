package com.studybuddy.v2.ui.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors

/**
 * P0 占位 —— P1/P2 时各 tab 替换为真实 Screen。
 */
@Composable
fun PlaceholderScreen(title: String, subtitle: String) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ClaudeSpacing.pageHorizontal)
                .padding(top = ClaudeSpacing.xxl)
        ) {
            Text(title.uppercase(), style = ClaudeType.CaptionUppercase, color = colors.muted)
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Text(title, style = ClaudeType.DisplayMd, color = colors.ink)
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Text(subtitle, style = ClaudeType.BodyMd, color = colors.body)
        }
    }
}
