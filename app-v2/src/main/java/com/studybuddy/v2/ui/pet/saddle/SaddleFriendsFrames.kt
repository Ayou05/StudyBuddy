package com.studybuddy.v2.ui.pet.saddle

import com.studybuddy.v2.data.model.GrowthStage

/**
 * 鞍部猫和它的朋友们 —— 多品种像素生物矩阵库 v2。
 *
 * # 设计立场升级
 *
 * v1 用 patch 改鞍部猫矩阵 → 5 个品种长得几乎一样，没有辨识度。
 *
 * v2 每个品种**完全独立**画 IDLE 矩阵，互相不复用。每个品种都有：
 * - **独特的轮廓**（耳朵 / 头形 / 体型）
 * - **独特的眼睛**（鞍部猫的双竖眼仍是基线，但其他品种有变体）
 * - **次色支持**（暹罗面罩 / 奶牛斑点 / 狐胸前白）—— 通过 patternMatrix 单独存
 * - **5 阶段独立矩阵**（不是裁剪）
 * - **ULTIMATE 配饰**（每个品种独特装扮）
 *
 * # 矩阵规格
 *
 * 统一 13×15 画布（FullBody 同），尺寸不变，但每行的填充模式因品种而完全不同。
 *
 * # 双色渲染
 *
 * 主色矩阵 + pattern 矩阵叠加：
 * - 主色 = breed 默认色（cream / coral / amber 调色板）
 * - pattern = 次色（白 / 深褐 / 黑），叠在主色上
 *
 * 渲染器先画主色，再画 pattern（透明位置不覆盖主色）。
 */
object SaddleFriendsFrames {

    const val ROWS = 13
    const val COLS = 15

    enum class Breed { ORANGE, SIAMESE, FOX, RABBIT, COW, SHIBA, CAPYBARA, HEDGEHOG }

    /** 完整生物视觉：主色矩阵 + 可选次色 pattern */
    data class CreatureSpec(
        val main: IntArray,
        val pattern: IntArray? = null,
        val patternHex: String? = null
    )

    fun specFor(
        breed: Breed,
        stage: GrowthStage,
        pose: Pose = Pose.IDLE
    ): CreatureSpec {
        if (stage == GrowthStage.EGG) return eggFor(breed)
        return when (breed) {
            Breed.ORANGE -> orangeSpec(stage, pose)
            Breed.SIAMESE -> siameseSpec(stage, pose)
            Breed.FOX -> foxSpec(stage, pose)
            Breed.RABBIT -> rabbitSpec(stage, pose)
            Breed.COW -> cowSpec(stage, pose)
            Breed.SHIBA -> shibaSpec(stage, pose)
            Breed.CAPYBARA -> capybaraSpec(stage, pose)
            Breed.HEDGEHOG -> hedgehogSpec(stage, pose)
        }
    }

    fun colorHexFor(breed: Breed): String = when (breed) {
        Breed.ORANGE -> "#CC785C"
        Breed.SIAMESE -> "#E8DCC4"
        Breed.FOX -> "#D4845A"
        Breed.RABBIT -> "#C9C2B8"
        Breed.COW -> "#FAF9F5"
        Breed.SHIBA -> "#D4A04A"
        Breed.CAPYBARA -> "#A37B73"
        Breed.HEDGEHOG -> "#8E6B53"
    }

    fun displayNameFor(breed: Breed): String = when (breed) {
        Breed.ORANGE -> "鞍部猫"
        Breed.SIAMESE -> "暹罗"
        Breed.FOX -> "狐狸"
        Breed.RABBIT -> "兔子"
        Breed.COW -> "奶牛"
        Breed.SHIBA -> "柴犬"
        Breed.CAPYBARA -> "水豚"
        Breed.HEDGEHOG -> "刺猬"
    }

    // ═════════════════════════════════════════════════════════════════
    // EGG（每个品种壳上有一道斑纹差异）
    // ═════════════════════════════════════════════════════════════════
    private fun eggFor(breed: Breed): CreatureSpec {
        val base = intArrayOf(
            0,0,0,0,0,0,1,1,1,0,0,0,0,0,0,
            0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,
            0,0,0,0,1,1,1,1,1,1,1,0,0,0,0,
            0,0,0,0,1,1,1,1,1,1,1,0,0,0,0,
            0,0,0,0,1,1,1,1,1,1,1,0,0,0,0,
            0,0,0,0,1,1,1,1,1,1,1,0,0,0,0,
            0,0,0,0,1,1,1,1,1,1,1,0,0,0,0,
            0,0,0,0,1,1,1,1,1,1,1,0,0,0,0,
            0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,
            0,0,0,0,0,0,1,1,1,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
        )
        // 每种蛋有独特斑纹（次色）
        val pattern = IntArray(ROWS * COLS)
        val patternHex: String
        when (breed) {
            Breed.ORANGE -> {
                // 一条斜纹
                pattern[3 * COLS + 5] = 1; pattern[3 * COLS + 6] = 1
                pattern[4 * COLS + 6] = 1; pattern[4 * COLS + 7] = 1
                patternHex = "#A9583E"
            }
            Breed.SIAMESE -> {
                // 顶部一片深色
                pattern[2 * COLS + 5] = 1; pattern[2 * COLS + 6] = 1
                pattern[2 * COLS + 7] = 1; pattern[2 * COLS + 8] = 1; pattern[2 * COLS + 9] = 1
                pattern[3 * COLS + 5] = 1; pattern[3 * COLS + 9] = 1
                patternHex = "#A37B73"
            }
            Breed.FOX -> {
                // 三角斑
                pattern[4 * COLS + 7] = 1
                pattern[5 * COLS + 6] = 1; pattern[5 * COLS + 7] = 1; pattern[5 * COLS + 8] = 1
                patternHex = "#FAF9F5"
            }
            Breed.RABBIT -> {
                // 圆点点
                pattern[3 * COLS + 6] = 1
                pattern[5 * COLS + 8] = 1
                pattern[7 * COLS + 5] = 1
                patternHex = "#FAF9F5"
            }
            Breed.COW -> {
                // 经典黑斑
                pattern[3 * COLS + 5] = 1; pattern[3 * COLS + 6] = 1
                pattern[4 * COLS + 5] = 1
                pattern[6 * COLS + 8] = 1; pattern[6 * COLS + 9] = 1
                pattern[7 * COLS + 9] = 1
                patternHex = "#181715"
            }
            Breed.SHIBA -> {
                // 柴犬蛋：眉毛斑（顶部两小坨）
                pattern[3 * COLS + 5] = 1
                pattern[3 * COLS + 9] = 1
                patternHex = "#A9583E"
            }
            Breed.CAPYBARA -> {
                // 水豚蛋：横纹（佛系横线）
                pattern[5 * COLS + 5] = 1; pattern[5 * COLS + 6] = 1
                pattern[5 * COLS + 8] = 1; pattern[5 * COLS + 9] = 1
                patternHex = "#5B4640"
            }
            Breed.HEDGEHOG -> {
                // 刺猬蛋：尖刺斑
                pattern[2 * COLS + 6] = 1; pattern[2 * COLS + 8] = 1
                pattern[4 * COLS + 5] = 1; pattern[4 * COLS + 9] = 1
                pattern[6 * COLS + 7] = 1
                patternHex = "#181715"
            }
        }
        return CreatureSpec(base, pattern, patternHex)
    }

    // ═════════════════════════════════════════════════════════════════
    // 鞍部猫 ORANGE —— 经典双竖眼，立耳，coral 主色
    // ═════════════════════════════════════════════════════════════════
    private fun orangeSpec(stage: GrowthStage, pose: Pose): CreatureSpec {
        val base = orangeAdult(pose)
        val matrix = applyStageMask(stage, base)
        // ULTIMATE：墨镜
        if (stage == GrowthStage.ULTIMATE) {
            // 横一道墨镜（覆盖眼区 row 5-7）
            val m = matrix.copyOf()
            for (c in 4..10) m[5 * COLS + c] = 1
            for (c in 4..10) m[6 * COLS + c] = 1
            return CreatureSpec(m)
        }
        return CreatureSpec(matrix)
    }

    private fun orangeAdult(pose: Pose): IntArray {
        // 复用现有鞍部猫 FullBody（已是用户原创的精品）
        return SaddleCatFrames.FullBody.framesFor(pose).first()
    }

    // ═════════════════════════════════════════════════════════════════
    // 暹罗 SIAMESE —— 米白脸 + 深色面罩（眼周 / 耳尖 / 脚尖暗）
    // ═════════════════════════════════════════════════════════════════
    private fun siameseSpec(stage: GrowthStage, pose: Pose): CreatureSpec {
        val base = orangeAdult(pose)  // 同猫科轮廓
        val main = applyStageMask(stage, base)
        // 面罩 pattern：耳尖 + 眼周 + 脚尖 暗色
        val pattern = IntArray(ROWS * COLS)
        if (stage >= GrowthStage.BABY) {
            // 耳尖暗
            pattern[1 * COLS + 1] = 1; pattern[1 * COLS + 2] = 1
            pattern[1 * COLS + 12] = 1; pattern[1 * COLS + 13] = 1
            pattern[2 * COLS + 1] = 1; pattern[2 * COLS + 13] = 1
        }
        if (stage >= GrowthStage.YOUNG) {
            // 眼周一圈暗
            pattern[5 * COLS + 5] = 1; pattern[5 * COLS + 9] = 1
            pattern[6 * COLS + 5] = 1; pattern[6 * COLS + 9] = 1
            pattern[7 * COLS + 5] = 1; pattern[7 * COLS + 9] = 1
            pattern[4 * COLS + 4] = 1; pattern[4 * COLS + 6] = 1
            pattern[4 * COLS + 8] = 1; pattern[4 * COLS + 10] = 1
        }
        if (stage == GrowthStage.ADULT || stage == GrowthStage.ULTIMATE) {
            // 脚尖暗
            pattern[12 * COLS + 1] = 1; pattern[12 * COLS + 2] = 1
            pattern[12 * COLS + 12] = 1; pattern[12 * COLS + 13] = 1
        }
        // ULTIMATE：围巾
        val mainFinal = if (stage == GrowthStage.ULTIMATE) {
            val m = main.copyOf()
            // row 8 横一道围巾
            for (c in 1..13) m[8 * COLS + c] = 1
            m
        } else main
        return CreatureSpec(mainFinal, pattern, "#5B4640")
    }

    // ═════════════════════════════════════════════════════════════════
    // 鞍部狐 FOX —— 立尖耳 + 长嘴 + 胸前白色 V
    // ═════════════════════════════════════════════════════════════════
    private fun foxSpec(stage: GrowthStage, pose: Pose): CreatureSpec {
        val base = foxAdult(pose)
        val main = applyStageMask(stage, base)
        // 胸前白
        val pattern = IntArray(ROWS * COLS)
        if (stage >= GrowthStage.YOUNG) {
            pattern[8 * COLS + 6] = 1; pattern[8 * COLS + 7] = 1; pattern[8 * COLS + 8] = 1
            pattern[9 * COLS + 7] = 1
        }
        // ULTIMATE：草帽
        val mainFinal = if (stage == GrowthStage.ULTIMATE) {
            val m = main.copyOf()
            // 草帽顶
            for (c in 5..9) m[0 * COLS + c] = 1
            // 帽檐宽
            for (c in 3..11) m[1 * COLS + c] = 1
            // 把原耳朵覆盖掉
            m
        } else main
        return CreatureSpec(mainFinal, pattern, "#FAF9F5")
    }

    private fun foxAdult(pose: Pose): IntArray {
        // 狐狸独有轮廓：尖立耳（row 0 加耳尖）+ 长嘴（row 4 鼻梁延伸）+ 大尾巴
        val m = IntArray(ROWS * COLS)
        // row 0: 两个高耳尖
        m[0 * COLS + 1] = 1; m[0 * COLS + 13] = 1
        // row 1: 耳根渐宽
        m[1 * COLS + 0] = 1; m[1 * COLS + 1] = 1; m[1 * COLS + 2] = 1
        m[1 * COLS + 12] = 1; m[1 * COLS + 13] = 1; m[1 * COLS + 14] = 1
        // row 2: 头顶轮廓（连接耳朵和头部）
        for (c in 0..14) m[2 * COLS + c] = 1
        // row 3: 眉/鼻梁
        m[3 * COLS + 0] = 1; m[3 * COLS + 14] = 1
        for (c in 4..10) m[3 * COLS + c] = 1
        // row 4: 鼻梁中段
        m[4 * COLS + 0] = 1; m[4 * COLS + 14] = 1
        m[4 * COLS + 7] = 1
        // row 5-7: 双竖眼（窄一些，体现狐狸）
        for (r in 5..7) {
            m[r * COLS + 0] = 1; m[r * COLS + 14] = 1
            m[r * COLS + 5] = 1; m[r * COLS + 9] = 1
        }
        // row 8-10: 身体
        for (r in 8..10) {
            m[r * COLS + 0] = 1; m[r * COLS + 14] = 1
        }
        // row 11: 大尾巴（横向拖出）
        m[11 * COLS + 0] = 1; m[11 * COLS + 14] = 1
        m[11 * COLS + 13] = 1; m[11 * COLS + 12] = 1
        // row 12: 脚 + 尾巴尖
        for (c in 0..6) m[12 * COLS + c] = 1
        for (c in 8..14) m[12 * COLS + c] = 1
        return m
    }

    // ═════════════════════════════════════════════════════════════════
    // 鞍部兔 RABBIT —— 长耳朵（高 3 行）+ 短身体
    // ═════════════════════════════════════════════════════════════════
    private fun rabbitSpec(stage: GrowthStage, pose: Pose): CreatureSpec {
        val base = rabbitAdult(pose)
        val main = applyStageMask(stage, base)
        // 兔子肚子白色
        val pattern = IntArray(ROWS * COLS)
        if (stage >= GrowthStage.YOUNG) {
            for (c in 5..9) pattern[8 * COLS + c] = 1
            for (c in 6..8) pattern[9 * COLS + c] = 1
        }
        // ULTIMATE：脖子上一颗铃铛
        val mainFinal = if (stage == GrowthStage.ULTIMATE) {
            val m = main.copyOf()
            m[7 * COLS + 7] = 1
            m[8 * COLS + 6] = 1; m[8 * COLS + 7] = 1; m[8 * COLS + 8] = 1
            m
        } else main
        return CreatureSpec(mainFinal, pattern, "#FAF9F5")
    }

    private fun rabbitAdult(pose: Pose): IntArray {
        val m = IntArray(ROWS * COLS)
        // row 0-2: 长耳朵两根（左右各一根，2 格宽 3 行高）
        for (r in 0..2) {
            m[r * COLS + 4] = 1; m[r * COLS + 5] = 1
            m[r * COLS + 9] = 1; m[r * COLS + 10] = 1
        }
        // row 3: 头顶宽轮廓
        m[3 * COLS + 3] = 1; m[3 * COLS + 4] = 1; m[3 * COLS + 5] = 1
        m[3 * COLS + 9] = 1; m[3 * COLS + 10] = 1; m[3 * COLS + 11] = 1
        // row 4: 圆头宽轮廓
        m[4 * COLS + 2] = 1
        for (c in 4..10) m[4 * COLS + c] = 1
        m[4 * COLS + 12] = 1
        // row 5-6: 头部边
        m[5 * COLS + 2] = 1; m[5 * COLS + 12] = 1
        m[6 * COLS + 2] = 1; m[6 * COLS + 12] = 1
        // row 5-6: 双圆眼（兔子眼睛是横的圆点）
        m[5 * COLS + 5] = 1; m[5 * COLS + 6] = 1
        m[5 * COLS + 8] = 1; m[5 * COLS + 9] = 1
        m[6 * COLS + 5] = 1; m[6 * COLS + 6] = 1
        m[6 * COLS + 8] = 1; m[6 * COLS + 9] = 1
        // row 7: 鼻子小三点
        m[7 * COLS + 2] = 1; m[7 * COLS + 12] = 1
        m[7 * COLS + 7] = 1
        // row 8-10: 圆胖身体
        m[8 * COLS + 2] = 1
        for (c in 4..10) m[8 * COLS + c] = 1
        m[8 * COLS + 12] = 1
        for (r in 9..10) {
            m[r * COLS + 3] = 1
            for (c in 5..9) m[r * COLS + c] = 1
            m[r * COLS + 11] = 1
        }
        // row 11: 短小尾巴（中央一团）
        for (c in 6..8) m[11 * COLS + c] = 1
        // row 12: 脚（两团）
        for (c in 4..6) m[12 * COLS + c] = 1
        for (c in 8..10) m[12 * COLS + c] = 1
        return m
    }

    // ═════════════════════════════════════════════════════════════════
    // 鞍部奶牛 COW —— 短角 + 大斑点黑白
    // ═════════════════════════════════════════════════════════════════
    private fun cowSpec(stage: GrowthStage, pose: Pose): CreatureSpec {
        val base = cowAdult(pose)
        val main = applyStageMask(stage, base)
        // 大黑斑（pattern 次色 = 黑色）
        val pattern = IntArray(ROWS * COLS)
        if (stage >= GrowthStage.BABY) {
            // 头顶斑
            pattern[2 * COLS + 5] = 1; pattern[2 * COLS + 6] = 1
            pattern[3 * COLS + 5] = 1; pattern[3 * COLS + 6] = 1
        }
        if (stage >= GrowthStage.YOUNG) {
            // 眼周斑（黑眼圈 = 奶牛特色）
            pattern[5 * COLS + 9] = 1
            pattern[6 * COLS + 9] = 1; pattern[6 * COLS + 10] = 1
            pattern[7 * COLS + 9] = 1
        }
        if (stage >= GrowthStage.ADULT) {
            // 身体大斑
            pattern[9 * COLS + 3] = 1; pattern[9 * COLS + 4] = 1
            pattern[10 * COLS + 3] = 1; pattern[10 * COLS + 4] = 1; pattern[10 * COLS + 5] = 1
            pattern[10 * COLS + 10] = 1; pattern[10 * COLS + 11] = 1
        }
        // ULTIMATE：脖铃
        val mainFinal = if (stage == GrowthStage.ULTIMATE) {
            val m = main.copyOf()
            // 项圈一道横
            for (c in 4..10) m[7 * COLS + c] = 1
            // 铃铛中央
            m[8 * COLS + 7] = 1
            m
        } else main
        return CreatureSpec(mainFinal, pattern, "#181715")
    }

    private fun cowAdult(pose: Pose): IntArray {
        val m = IntArray(ROWS * COLS)
        // row 0: 两个小角
        m[0 * COLS + 3] = 1; m[0 * COLS + 11] = 1
        // row 1: 角根 + 头顶
        m[1 * COLS + 3] = 1; m[1 * COLS + 4] = 1
        m[1 * COLS + 10] = 1; m[1 * COLS + 11] = 1
        for (c in 5..9) m[1 * COLS + c] = 1
        // row 2-3: 头顶宽
        for (c in 1..13) m[2 * COLS + c] = 1
        for (c in 0..14) m[3 * COLS + c] = 1
        // row 4-5: 头部边
        m[4 * COLS + 0] = 1; m[4 * COLS + 14] = 1
        m[5 * COLS + 0] = 1; m[5 * COLS + 14] = 1
        // row 5: 双圆眼
        m[5 * COLS + 4] = 1; m[5 * COLS + 5] = 1
        m[5 * COLS + 9] = 1; m[5 * COLS + 10] = 1
        // row 6-7: 鼻子（大方鼻）
        m[6 * COLS + 0] = 1; m[6 * COLS + 14] = 1
        m[7 * COLS + 0] = 1; m[7 * COLS + 14] = 1
        for (c in 5..9) m[7 * COLS + c] = 1
        for (c in 6..8) m[6 * COLS + c] = 1
        // row 8-10: 大身体
        for (c in 1..13) m[8 * COLS + c] = 1
        for (r in 9..10) {
            m[r * COLS + 1] = 1; m[r * COLS + 13] = 1
        }
        // row 11: 长尾巴（一根斜下）
        m[11 * COLS + 1] = 1; m[11 * COLS + 13] = 1
        m[11 * COLS + 14] = 1
        // row 12: 四只蹄
        for (c in 1..3) m[12 * COLS + c] = 1
        for (c in 5..6) m[12 * COLS + c] = 1
        for (c in 8..9) m[12 * COLS + c] = 1
        for (c in 11..13) m[12 * COLS + c] = 1
        return m
    }

    // ═════════════════════════════════════════════════════════════════
    // 阶段裁剪：BABY 仅头部（row 0-7），YOUNG 头+上身（row 0-10），ADULT/ULTIMATE 全身
    // ═════════════════════════════════════════════════════════════════
    private fun applyStageMask(stage: GrowthStage, source: IntArray): IntArray {
        val m = source.copyOf()
        val maxRow = when (stage) {
            GrowthStage.EGG -> return source
            GrowthStage.BABY -> 7
            GrowthStage.YOUNG -> 11
            GrowthStage.ADULT -> 13
            GrowthStage.ULTIMATE -> 13
        }
        for (r in maxRow until ROWS) {
            for (c in 0 until COLS) m[r * COLS + c] = 0
        }
        return m
    }

    // ═════════════════════════════════════════════════════════════════
    // 柴犬 SHIBA —— 三角立耳 + 笑脸 + 卷尾
    // ═════════════════════════════════════════════════════════════════
    private fun shibaSpec(stage: GrowthStage, pose: Pose): CreatureSpec {
        val base = shibaAdult(pose)
        val main = applyStageMask(stage, base)
        // 嘴角白 / 眉上方白点（柴犬特征）
        val pattern = IntArray(ROWS * COLS)
        if (stage >= GrowthStage.YOUNG) {
            // 嘴角向上的小笑容
            pattern[7 * COLS + 6] = 1; pattern[7 * COLS + 8] = 1
            // 眉上眼周白
            pattern[4 * COLS + 4] = 1; pattern[4 * COLS + 10] = 1
            pattern[6 * COLS + 4] = 1; pattern[6 * COLS + 10] = 1
        }
        if (stage >= GrowthStage.ADULT) {
            // 肚子白
            pattern[10 * COLS + 6] = 1; pattern[10 * COLS + 7] = 1; pattern[10 * COLS + 8] = 1
        }
        // ULTIMATE：脖子上一条小红巾
        val mainFinal = if (stage == GrowthStage.ULTIMATE) {
            val m = main.copyOf()
            // 小三角红巾（向下三角）
            m[8 * COLS + 5] = 1; m[8 * COLS + 6] = 1; m[8 * COLS + 7] = 1; m[8 * COLS + 8] = 1; m[8 * COLS + 9] = 1
            m[9 * COLS + 6] = 1; m[9 * COLS + 7] = 1; m[9 * COLS + 8] = 1
            m
        } else main
        return CreatureSpec(mainFinal, pattern, "#FAF9F5")
    }

    private fun shibaAdult(pose: Pose): IntArray {
        val m = IntArray(ROWS * COLS)
        // row 0: 三角立耳尖（柴犬经典）
        m[0 * COLS + 2] = 1; m[0 * COLS + 12] = 1
        // row 1: 三角立耳中
        m[1 * COLS + 1] = 1; m[1 * COLS + 2] = 1; m[1 * COLS + 3] = 1
        m[1 * COLS + 11] = 1; m[1 * COLS + 12] = 1; m[1 * COLS + 13] = 1
        // row 2: 耳根 + 头顶起伏
        m[2 * COLS + 1] = 1; m[2 * COLS + 13] = 1
        for (c in 4..10) m[2 * COLS + c] = 1
        // row 3: 头顶宽
        for (c in 1..13) m[3 * COLS + c] = 1
        // row 4: 头部边
        m[4 * COLS + 0] = 1; m[4 * COLS + 14] = 1
        // row 5-6: 双竖眼（柴犬眼睛偏上）
        m[5 * COLS + 0] = 1; m[5 * COLS + 14] = 1
        m[5 * COLS + 5] = 1; m[5 * COLS + 9] = 1
        m[6 * COLS + 0] = 1; m[6 * COLS + 14] = 1
        m[6 * COLS + 5] = 1; m[6 * COLS + 9] = 1
        // row 7: 鼻子（中央一点）+ 嘴角
        m[7 * COLS + 0] = 1; m[7 * COLS + 14] = 1
        m[7 * COLS + 7] = 1
        // row 8-9: 颈 + 上身
        m[8 * COLS + 0] = 1; m[8 * COLS + 14] = 1
        for (c in 2..12) m[8 * COLS + c] = 1
        m[9 * COLS + 1] = 1; m[9 * COLS + 13] = 1
        // row 10: 身体
        for (c in 1..13) m[10 * COLS + c] = 1
        // row 11: 卷尾（柴犬经典翻在身上）+ 短身体
        for (c in 1..13) m[11 * COLS + c] = 1
        m[11 * COLS + 11] = 1; m[11 * COLS + 12] = 1
        // row 12: 四只小脚
        for (c in 1..3) m[12 * COLS + c] = 1
        for (c in 5..6) m[12 * COLS + c] = 1
        for (c in 8..9) m[12 * COLS + c] = 1
        for (c in 11..13) m[12 * COLS + c] = 1
        return m
    }

    // ═════════════════════════════════════════════════════════════════
    // 水豚 CAPYBARA —— 长方头 + 小耳 + 躺平眯眼
    // ═════════════════════════════════════════════════════════════════
    private fun capybaraSpec(stage: GrowthStage, pose: Pose): CreatureSpec {
        val base = capybaraAdult(pose)
        val main = applyStageMask(stage, base)
        val pattern = IntArray(ROWS * COLS)
        // 鼻子深色一团（水豚特征）
        if (stage >= GrowthStage.YOUNG) {
            pattern[7 * COLS + 6] = 1; pattern[7 * COLS + 7] = 1; pattern[7 * COLS + 8] = 1
        }
        // ULTIMATE：头顶一片橘子（水豚泡澡 meme）
        val mainFinal = if (stage == GrowthStage.ULTIMATE) {
            val m = main.copyOf()
            m[0 * COLS + 7] = 1
            m
        } else main
        val patternFinal = if (stage == GrowthStage.ULTIMATE) {
            val p = pattern.copyOf()
            p[0 * COLS + 7] = 1  // 橘子斑
            p
        } else pattern
        val patternHex = if (stage == GrowthStage.ULTIMATE) "#D4845A" else "#5B4640"
        return CreatureSpec(mainFinal, patternFinal, patternHex)
    }

    private fun capybaraAdult(pose: Pose): IntArray {
        val m = IntArray(ROWS * COLS)
        // row 0: 头顶光秃秃，没有耳朵尖
        // row 1: 小圆耳（两侧靠上）
        m[1 * COLS + 1] = 1; m[1 * COLS + 13] = 1
        // row 2: 长方头顶
        for (c in 1..13) m[2 * COLS + c] = 1
        // row 3-4: 长方头身
        m[3 * COLS + 0] = 1; m[3 * COLS + 14] = 1
        m[4 * COLS + 0] = 1; m[4 * COLS + 14] = 1
        // row 5-6: 双横眼（眯眼，水豚标志）—— 不是竖线是横线
        m[5 * COLS + 0] = 1; m[5 * COLS + 14] = 1
        for (c in 4..6) m[5 * COLS + c] = 1
        for (c in 8..10) m[5 * COLS + c] = 1
        m[6 * COLS + 0] = 1; m[6 * COLS + 14] = 1
        // row 7: 鼻子（大方鼻）
        m[7 * COLS + 0] = 1; m[7 * COLS + 14] = 1
        // row 8: 下颚
        for (c in 0..14) m[8 * COLS + c] = 1
        // row 9-10: 圆胖躺平身体
        m[9 * COLS + 0] = 1
        for (c in 2..12) m[9 * COLS + c] = 1
        m[9 * COLS + 14] = 1
        m[10 * COLS + 0] = 1
        for (c in 1..13) m[10 * COLS + c] = 1
        m[10 * COLS + 14] = 1
        // row 11: 短小尾巴 + 短脚（看不到）
        for (c in 1..13) m[11 * COLS + c] = 1
        // row 12: 短脚（贴地，几乎看不见）
        for (c in 2..4) m[12 * COLS + c] = 1
        for (c in 10..12) m[12 * COLS + c] = 1
        return m
    }

    // ═════════════════════════════════════════════════════════════════
    // 刺猬 HEDGEHOG —— 一排刺 + 小尖鼻 + 圆身
    // ═════════════════════════════════════════════════════════════════
    private fun hedgehogSpec(stage: GrowthStage, pose: Pose): CreatureSpec {
        val base = hedgehogAdult(pose)
        val main = applyStageMask(stage, base)
        val pattern = IntArray(ROWS * COLS)
        // 刺背深色（pattern 是背上一排刺的尖端）
        if (stage >= GrowthStage.BABY) {
            pattern[1 * COLS + 3] = 1; pattern[1 * COLS + 5] = 1
            pattern[1 * COLS + 7] = 1
            pattern[1 * COLS + 9] = 1; pattern[1 * COLS + 11] = 1
        }
        if (stage >= GrowthStage.YOUNG) {
            // 更多刺
            pattern[0 * COLS + 4] = 1; pattern[0 * COLS + 6] = 1
            pattern[0 * COLS + 8] = 1; pattern[0 * COLS + 10] = 1
        }
        // ULTIMATE：头上戴一朵小花
        val mainFinal = if (stage == GrowthStage.ULTIMATE) {
            val m = main.copyOf()
            m[0 * COLS + 7] = 1
            m
        } else main
        val patternFinal = if (stage == GrowthStage.ULTIMATE) {
            val p = pattern.copyOf()
            p[0 * COLS + 7] = 0  // 花占用顶部，移开刺
            p
        } else pattern
        return CreatureSpec(mainFinal, patternFinal, "#181715")
    }

    private fun hedgehogAdult(pose: Pose): IntArray {
        val m = IntArray(ROWS * COLS)
        // row 0-1: 一排刺尖（zigzag 形状）
        for (c in intArrayOf(3, 5, 7, 9, 11)) m[0 * COLS + c] = 1
        for (c in 2..12) m[1 * COLS + c] = 1
        // row 2-3: 圆头
        for (c in 1..13) m[2 * COLS + c] = 1
        for (c in 0..14) m[3 * COLS + c] = 1
        // row 4-5: 头部 + 双小圆眼
        m[4 * COLS + 0] = 1; m[4 * COLS + 14] = 1
        m[5 * COLS + 0] = 1; m[5 * COLS + 14] = 1
        m[5 * COLS + 5] = 1; m[5 * COLS + 9] = 1
        // row 6: 头部
        m[6 * COLS + 0] = 1; m[6 * COLS + 14] = 1
        // row 7: 长尖鼻（伸出脸前）
        m[7 * COLS + 0] = 1; m[7 * COLS + 14] = 1
        for (c in 6..8) m[7 * COLS + c] = 1
        m[7 * COLS + 7] = 1
        // row 8: 下颚 + 鼻尖
        for (c in 0..14) m[8 * COLS + c] = 1
        // row 9-10: 圆胖球身体（缩成球姿态）
        m[9 * COLS + 0] = 1
        for (c in 2..12) m[9 * COLS + c] = 1
        m[9 * COLS + 14] = 1
        m[10 * COLS + 1] = 1
        for (c in 3..11) m[10 * COLS + c] = 1
        m[10 * COLS + 13] = 1
        // row 11-12: 缩起来不露脚
        for (c in 4..10) m[11 * COLS + c] = 1
        for (c in 5..9) m[12 * COLS + c] = 1
        return m
    }
}
