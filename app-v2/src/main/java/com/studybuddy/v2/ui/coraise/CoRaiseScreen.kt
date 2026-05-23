package com.studybuddy.v2.ui.coraise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.SwipeableSubTabs
import com.studybuddy.v2.ui.notes.NoteWallScreen
import com.studybuddy.v2.ui.pet.PetScreen
import com.studybuddy.v2.ui.vault.VaultScreen

/**
 * 共养 tab —— 我们共有的三件事。
 *
 * 三个 sub-tab 横滑切换 + 底部文字指示器（拇指可达）。
 * 顶部不放任何切换控件（违反单手 UX）。
 *
 * - 鞍部猫（PetScreen）：宠物养成
 * - 金库（VaultScreen）：账本+基金合并
 * - 便签墙（NoteWallScreen）：双人共建的"墙"
 */
@Composable
fun CoRaiseScreen(
    onOpenSettings: () -> Unit = {},
    onOpenCodex: () -> Unit = {}
) {
    val colors = MaterialTheme.appColors
    Box(modifier = Modifier.fillMaxSize().background(colors.canvas)) {
        SwipeableSubTabs(
            tabs = listOf("宠物", "金库", "便签墙"),
            pageCount = 3
        ) { page ->
            when (page) {
                0 -> PetScreen(onOpenCodex = onOpenCodex)
                1 -> VaultScreen()
                2 -> NoteWallScreen()
            }
        }
    }
}
