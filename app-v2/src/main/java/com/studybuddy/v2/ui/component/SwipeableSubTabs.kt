package com.studybuddy.v2.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 横向滑动 sub-tab 容器。
 *
 * 单手 UX 准则：
 * - tab 之间可左右滑切换（HorizontalPager）
 * - 顶部不放任何切换控件
 * - 底部三个圆点 + 当前页小字（极简态，参考 iOS 控件）
 * - 点圆点 = 跳到该 sub-tab
 *
 * 切页动画：iOS 自定义壁纸式"覆盖卡片" —— 进入页从右滑入并加阴影，
 * 离开页保持原位 + 轻微缩小变暗，呈现"上一张被压住"的层级感。
 */
@OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.animation.ExperimentalAnimationApi::class
)
@Composable
fun SwipeableSubTabs(
    tabs: List<String>,
    pageCount: Int = tabs.size,
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit = {},
    content: @Composable (page: Int) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.appColors

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 内容区 — 占满剩余空间，可左右滑；覆盖卡片体感
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            // offset: 当前页 = 0；右邻 = +1（在右边等着进来）；左邻 = -1（已被覆盖）
            val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            // offset > 0：本页在左侧（被覆盖），offset < 0：本页在右侧（即将覆盖进来）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (offset <= 0f) {
                            // 即将进入或当前页：translationX = -offset * width，自带 8dp 阴影
                            translationX = -offset * size.width
                            scaleX = 1f
                            scaleY = 1f
                            alpha = 1f
                            shadowElevation = if (offset < 0f) 12.dp.toPx() else 0f
                        } else {
                            // 被覆盖（在左下方）：保持原位，轻缩 + 轻闇
                            val depth = offset.coerceIn(0f, 1f)
                            translationX = 0f
                            scaleX = 1f - depth * 0.06f
                            scaleY = 1f - depth * 0.06f
                            alpha = 1f - depth * 0.35f
                            shadowElevation = 0f
                        }
                    }
                    .background(colors.canvas)
            ) {
                content(page)
            }
        }

        // 底部三圆点指示器 + 当前页小字（极简）
        SubTabDots(
            tabs = tabs,
            currentPage = pagerState.currentPage,
            onJump = { idx ->
                scope.launch { pagerState.animateScrollToPage(idx) }
            }
        )
    }
}

@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
private fun SubTabDots(
    tabs: List<String>,
    currentPage: Int,
    onJump: (Int) -> Unit
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ClaudeSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 三个圆点 row
        Row(
            horizontalArrangement = Arrangement.spacedBy(ClaudeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { idx, _ ->
                val isActive = currentPage == idx
                val dotSize by animateDpAsState(
                    targetValue = if (isActive) 8.dp else 6.dp,
                    animationSpec = tween(180),
                    label = "dotSize"
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { onJump(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(
                                if (isActive) colors.ink
                                else colors.hairline
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        // 当前页小字 caption（crossfade）
        AnimatedContent(
            targetState = tabs.getOrNull(currentPage) ?: "",
            transitionSpec = {
                (fadeIn(tween(180)) togetherWith fadeOut(tween(120)))
            },
            label = "subTabLabel"
        ) { label ->
            Text(
                label,
                style = ClaudeType.Caption,
                color = colors.muted
            )
        }
    }
}
