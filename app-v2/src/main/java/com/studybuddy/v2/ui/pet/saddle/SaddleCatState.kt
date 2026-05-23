package com.studybuddy.v2.ui.pet.saddle

/**
 * 鞍部猫状态机。
 *
 * 形象用户原创：纯几何方头猫，关键标识是第 6-7 行的"鞍部"双竖眼。
 * 渲染走 Compose Canvas 画 0/1 像素矩阵（见 [PixelMatrix]），不走 PNG 也不走字符画。
 *
 * Pose 是宠物当下的表现形态。
 * - IDLE / TYPING / SLEEPING 由 FocusViewModel 状态驱动
 * - HAPPY / SAD 由 Focus 完成 / 放弃事件触发，定时回 IDLE
 * - EATING / PLAYING / CLEANING / PETTING 由互动按钮短暂触发
 * - DIRTY 由四维 cleanliness 阈值触发
 * - WATCHING 由全局点击事件触发，朝向被点位置（L3 感知）
 */
enum class Pose {
    IDLE, HAPPY, SAD, SLEEPING, TYPING, DIRTY,
    EATING, PLAYING, CLEANING, PETTING, WATCHING,
    STARTLED  // 受惊（凑近点击触发）—— 用 SAD 帧 + 短暂跳起来
}

enum class GazeDirection { LEFT, CENTER, RIGHT, UP }

data class SaddleCatState(
    val pose: Pose = Pose.IDLE,
    val gaze: GazeDirection = GazeDirection.CENTER,
    val hunger: Float = 80f,
    val mood: Float = 80f,
    val cleanliness: Float = 80f,
    val intimacy: Float = 80f,
    val isFocusing: Boolean = false
)
