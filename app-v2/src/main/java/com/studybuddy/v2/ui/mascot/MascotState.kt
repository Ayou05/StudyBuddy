package com.studybuddy.v2.ui.mascot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局鞍部猫状态，跨页持久。
 *
 * 设计立场：
 * - 单例：MascotDock、ClockSideMascot、PetScreen 共享同一份状态
 * - 不打扰：状态变化只反映在 UI（情绪/姿态），不主动追踪用户
 * - 设计宗旨"它就那样，但它在"
 *
 * 用法：直接 @Inject 注入并读 / 改 emotion / spooked。
 */
@Singleton
class MascotState @Inject constructor() {
    /** 情绪：idle / happy / sleeping / spooked */
    var emotion by mutableStateOf("idle")
        private set

    /** 受惊计数（被拖拽次数），到 3 装死一会 */
    var spookedCount by mutableStateOf(0)
        private set

    /** 上次互动时间（用于触发首页睡眠：超过 5min 没动就 sleeping） */
    var lastInteractAt by mutableStateOf(System.currentTimeMillis())
        private set

    fun touch() {
        lastInteractAt = System.currentTimeMillis()
        if (emotion == "sleeping") emotion = "idle"
    }

    fun happy() {
        emotion = "happy"
        lastInteractAt = System.currentTimeMillis()
    }

    fun setIdle() {
        emotion = "idle"
    }

    fun setSleeping() {
        emotion = "sleeping"
    }

    fun spook() {
        spookedCount += 1
        if (spookedCount >= 3) {
            emotion = "spooked"
        }
        lastInteractAt = System.currentTimeMillis()
    }

    fun resetSpook() {
        spookedCount = 0
        if (emotion == "spooked") emotion = "idle"
    }

    /** 距离上次互动多久（毫秒） */
    fun idleMs(now: Long = System.currentTimeMillis()): Long = now - lastInteractAt
}
