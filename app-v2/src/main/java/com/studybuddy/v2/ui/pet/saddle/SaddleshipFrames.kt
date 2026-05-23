package com.studybuddy.v2.ui.pet.saddle

/**
 * 鞍部猫的小飞船 —— INTERIOR 模式专用。
 *
 * 飞船像素矩阵：9×3，是个薄薄的"扁碟"造型，刚好接住 HEAD_ONLY 的 10 列宽度。
 * HEAD_ONLY 本身没有脚 —— 视觉上是鞍部猫"从飞船舱口探出半身"，脖子那条边压在飞船上。
 *
 * ```
 *  0 1 1 1 1 1 1 1 0
 *  1 1 0 1 0 1 0 1 1
 *  0 0 1 0 1 0 1 0 0
 * ```
 *
 * 第 1 行：碟身上沿
 * 第 2 行：碟身舷窗（4 个 1×1 透光点）
 * 第 3 行：底部 3 个尖锐 thruster
 *
 * 渲染时颜色用 coral（区别于鞍部猫的 ink），让"骑乘"关系一眼看穿。
 */
object SaddleshipFrames {
    const val ROWS = 3
    const val COLS = 11

    // 比 HeadOnly(10列) 多 1 列两侧各扩 0.5 → 整体比头宽，托得稳，不头重脚轻
    val IDLE: IntArray = intArrayOf(
        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
        1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1,
        0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0,
    )

    /** thruster 闪烁帧（底部 3 个点错位） */
    val THRUST_A: IntArray = intArrayOf(
        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
        1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1,
        0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1,
    )

    val THRUST_B: IntArray = intArrayOf(
        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
        1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1,
        1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1,
    )

    /** 在 INTERIOR 模式下循环显示 thruster 闪烁 */
    val thrustFrames: List<IntArray> = listOf(IDLE, THRUST_A, IDLE, THRUST_B)
}
