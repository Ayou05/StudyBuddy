package com.studybuddy.v2.ui.pet.saddle

/**
 * 鞍部猫帧矩阵库。
 *
 * # 设计宗旨
 *
 * > **"它就那样，但它在。"**
 *
 * 鞍部猫的酷不在卖萌、不在表情丰富、不在动来动去。它的酷是稳定的存在感 ——
 * 一个潦草的几何图腾，长在屏幕角落或终端里，只要你看它一眼，它就在那。
 *
 * 所以本库的所有动画规则都服从这条宗旨：
 * - 不做"内部像素重排"式呼吸（会把眼睛和上半张脸的连接处弄断）
 * - 整体浮动放在 [SaddleCatSprite] 用 graphicsLayer 做亚像素 translationY，不动矩阵
 * - 表情差分只动嘴 / 眼睛区，不动脸轮廓
 * - 大小眼是它的二级辨识特征，做成专门的 SQUINT 差分
 *
 * # 两个形态
 * - [HeadOnly]: 7×10，只有头，给"角落探出"场景（MascotDock 之类）
 * - [FullBody]: 13×15 / 14×15，有腿有脚有嘴，给终端展示位
 *
 * 用户原创矩阵。HeadOnly 是最早 7×10 版本，FullBody 是用户后画的有腿版（眼睛已按反馈拉长到 3 格）。
 */
object SaddleCatFrames {

    // ───────────────────────────────────────────────────────────
    // HeadOnly 7×10
    // ───────────────────────────────────────────────────────────
    object HeadOnly {
        const val ROWS = 7
        const val COLS = 10

        /** IDLE — 用户原创 base */
        val IDLE: IntArray = intArrayOf(
            1, 0, 0, 0, 0, 0, 0, 0, 0, 1,
            1, 1, 0, 0, 0, 0, 0, 0, 1, 1,
            1, 0, 1, 0, 0, 0, 0, 1, 0, 1,
            1, 0, 0, 1, 1, 1, 1, 0, 0, 1,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 1,
            1, 0, 0, 1, 0, 0, 1, 0, 0, 1,
            1, 0, 0, 1, 0, 0, 1, 0, 0, 1
        )

        fun framesFor(pose: Pose): List<IntArray> = listOf(IDLE)
    }

    // ───────────────────────────────────────────────────────────
    // FullBody 14×15
    // ───────────────────────────────────────────────────────────
    object FullBody {
        const val ROWS = 13
        const val COLS = 15

        /** IDLE — 用户原创全身版，眼睛已按反馈拉长到 3 格 */
        val IDLE: IntArray = intArrayOf(
            // row 0: 脸轮廓两端
            1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
            // row 1: 耳根
            1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1,
            // row 2: 耳尖内缩
            1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1,
            // row 3: 眉/鼻梁横梁
            1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1,
            // row 4: 脸中段空白
            1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
            // row 5-7: 鞍部双眼（3 格长，居中分布）
            1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1,
            1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1,
            1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1,
            // row 8-10: 身体空白
            1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
            // row 11: 尾巴单点
            1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1,
            // row 12: 脚底，中间留 1 格分缝以显出两条腿
            1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1
        )

        /** HAPPY — 嘴打开（行 8-10 框出一个倒梯形）+ 眼睛保持竖线 */
        val HAPPY: IntArray = IDLE.copyOf().apply {
            // row 7 (嘴顶横线，与 row 3 平行)
            val r7 = 7 * COLS
            for (c in 3..11) this[r7 + c] = 1
            // row 8 嘴左右壁
            val r8 = 8 * COLS
            this[r8 + 3] = 1
            this[r8 + 11] = 1
            // row 9 嘴左右壁
            val r9 = 9 * COLS
            this[r9 + 3] = 1
            this[r9 + 11] = 1
            // row 10 嘴底（与 row 3 对称的横线）
            val r10 = 10 * COLS
            for (c in 4..10) this[r10 + c] = 1
        }

        /** SQUINT — 大小眼，左 3 格 / 右 4 格（鞍部猫的潦草特色） */
        val SQUINT: IntArray = IDLE.copyOf().apply {
            // 右眼额外加一格（row 8 col 9）
            this[8 * COLS + 9] = 1
        }

        /** SLEEPING — 双眼闭合成横线（替换 row 6 那一行的两个点为横线） */
        val SLEEPING: IntArray = IDLE.copyOf().apply {
            val r6 = 6 * COLS
            // 把左眼竖线（row 5/6/7 col 5）改成 row 6 的横线 4-6
            for (r in intArrayOf(5, 7)) { this[r * COLS + 5] = 0 }
            for (c in 4..6) this[r6 + c] = 1
            // 右眼同理
            for (r in intArrayOf(5, 7)) { this[r * COLS + 9] = 0 }
            for (c in 8..10) this[r6 + c] = 1
        }

        /**
         * XD — 紧闭双眼（两个 X 字交叉形），用户原创：
         *   左眼：row 5 col 4 / row 5 col 9 / row 6 col 5..8 / row 7 col 4 / row 7 col 9
         *   按用户图示等比放大到 15 列布局
         */
        val XD: IntArray = IDLE.copyOf().apply {
            // 清掉默认竖眼
            for (r in 5..7) {
                this[r * COLS + 5] = 0
                this[r * COLS + 9] = 0
            }
            // 左眼 X
            this[5 * COLS + 4] = 1
            this[5 * COLS + 9] = 1
            this[6 * COLS + 5] = 1; this[6 * COLS + 6] = 1; this[6 * COLS + 7] = 1; this[6 * COLS + 8] = 1
            this[7 * COLS + 4] = 1
            this[7 * COLS + 9] = 1
        }

        /** 眨眼帧 1 — 第 5 行眼睛点已熄（从上往下缩） */
        val BLINK_1: IntArray = IDLE.copyOf().apply {
            this[5 * COLS + 5] = 0
            this[5 * COLS + 9] = 0
        }

        /** 眨眼帧 2 — 第 5/6 行已熄，只剩第 7 行 */
        val BLINK_2: IntArray = IDLE.copyOf().apply {
            this[5 * COLS + 5] = 0; this[5 * COLS + 9] = 0
            this[6 * COLS + 5] = 0; this[6 * COLS + 9] = 0
        }

        /** 眨眼帧 3 — 全闭（眼区清空） */
        val BLINK_3: IntArray = IDLE.copyOf().apply {
            for (r in 5..7) {
                this[r * COLS + 5] = 0
                this[r * COLS + 9] = 0
            }
        }

        /** TYPING — IDLE 不变，光标由 Sprite 层叠加 */
        val TYPING: IntArray = IDLE

        /** DIRTY — 身体中段散布几个污点 */
        val DIRTY: IntArray = IDLE.copyOf().apply {
            this[8 * COLS + 7] = 1
            this[9 * COLS + 4] = 1
            this[9 * COLS + 11] = 1
            this[10 * COLS + 6] = 1
        }

        fun framesFor(pose: Pose): List<IntArray> = when (pose) {
            Pose.IDLE -> listOf(IDLE)  // 眨眼由 Sprite 层走 [blinkFrames] 单独驱动
            Pose.HAPPY -> listOf(HAPPY)
            Pose.SAD -> listOf(SQUINT)  // 没单独画，复用 SQUINT 当难过表情
            Pose.SLEEPING -> listOf(SLEEPING)
            Pose.TYPING -> listOf(TYPING)
            Pose.DIRTY -> listOf(DIRTY)
            Pose.EATING, Pose.PLAYING, Pose.CLEANING, Pose.PETTING -> listOf(XD)  // 互动 = XD 笑眼
            Pose.WATCHING -> listOf(SQUINT)  // 看东西时眯眼
            Pose.STARTLED -> listOf(SQUINT)  // 受惊 = 眯眼（jump 动作由 Sprite 层 translationY 实现）
        }

        /**
         * 眨眼序列：3 帧渐次闭合，每帧 ~70ms。从外面看就像"啪—眨"。
         * Sprite 层在 IDLE 状态时每隔 4-6s 触发一次（带随机抖动避免节拍感）。
         */
        val blinkFrames: List<IntArray> = listOf(BLINK_1, BLINK_2, BLINK_3)
    }
}

enum class BodyType { HEAD_ONLY, FULL_BODY }
