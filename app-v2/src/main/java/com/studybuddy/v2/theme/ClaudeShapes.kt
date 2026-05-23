package com.studybuddy.v2.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val ClaudeShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(ClaudeRadius.xs),  // 4dp 标签
    small      = RoundedCornerShape(ClaudeRadius.md),  // 8dp button/input/category-tab
    medium     = RoundedCornerShape(ClaudeRadius.lg),  // 12dp 内容卡
    large      = RoundedCornerShape(ClaudeRadius.lg),
    extraLarge = RoundedCornerShape(ClaudeRadius.xl)   // 16dp hero illustration
)
