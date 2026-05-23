package com.studybuddy.v2.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * 文字避让登记表 —— 鞍部猫 INTERIOR 飞行时绕开的"禁飞区"。
 *
 * # 设计哲学
 * 鞍部猫不应该飞过文字（那会盖住用户在读的内容），但**可以**飞过卡片底色块、
 * 装饰色块、空白区域。所以我们用"opt-in"的注册模式：
 * - 所有 Text/Title/重要图标加上 `Modifier.mascotAvoid()`，注册自己的 bounds
 * - 飞船路径规划时把这些 Rect 当成障碍物绕开
 * - 没注册的视觉元素（Box 背景、装饰条）= 鞍部猫可以飞过去
 *
 * 这样视觉上像它"走在景上"而不是"穿过文字"。
 */
@Stable
class MascotAvoidRegistry {
    private val regions = mutableMapOf<Any, Rect>()

    fun register(key: Any, rect: Rect) {
        regions[key] = rect
    }

    fun unregister(key: Any) {
        regions.remove(key)
    }

    /** 当前所有禁飞区 */
    fun snapshot(): List<Rect> = regions.values.toList()

    /** 给定屏幕坐标 [point] 是否落在任何禁飞区里（含 [pad] 像素安全外扩） */
    fun isBlocked(point: androidx.compose.ui.geometry.Offset, pad: Float = 8f): Boolean =
        regions.values.any { r ->
            point.x in (r.left - pad)..(r.right + pad) &&
                point.y in (r.top - pad)..(r.bottom + pad)
        }
}

val LocalMascotAvoidRegistry = compositionLocalOf<MascotAvoidRegistry?> { null }

/**
 * 把当前 Composable 的屏幕 bounds 注册为鞍部猫禁飞区。Composable 离开组合时自动注销。
 *
 * 用法：`Text("标题", modifier = Modifier.mascotAvoid())`
 */
@Composable
fun Modifier.mascotAvoid(): Modifier {
    val registry = LocalMascotAvoidRegistry.current ?: return this
    val key = remember { Any() }
    DisposableEffect(key) {
        onDispose { registry.unregister(key) }
    }
    return this.composed {
        onGloballyPositioned { coords ->
            registry.register(key, coords.boundsInWindow())
        }
    }
}
