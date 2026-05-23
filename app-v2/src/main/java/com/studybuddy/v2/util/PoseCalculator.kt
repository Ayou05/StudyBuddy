package com.studybuddy.v2.util

import com.studybuddy.v2.data.model.Landmark
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.*

/**
 * 姿态系统纯函数。所有判定走加权 score → clamp → 落到具体姿态枚举。
 *
 * **核心立场**：没有"模式切换按钮"。app 自己感知你和 TA 的关系状态。
 * 用户只在 Home 顶部一行小灰字感受到 "app 在跟随我"，不打扰、不干涉。
 *
 * # 信号源
 * - 时间 + 日历（无成本）
 * - 当前位置 + 常驻地标（开 app 时单次定位）
 * - 双人距离（对方 lastKnownLoc + 我的 myLoc）
 * - 最近行为（开 app 前的 last 动作类型）
 *
 * 姿态值 ∈ [0, 1]：0 = 紧绷态（自习中），1 = 陪伴态（在一起出去玩）
 */
object PoseCalculator {

    /**
     * 主算法：多维加权 score 然后 clamp。
     *
     * @param now 时间戳（默认现在）
     * @param myLoc 自己当前 (lat, lng)，null 表示没拿到定位
     * @param myLandmarks 自己的常驻地标列表
     * @param partnerLoc 对方 lastKnownLoc，null 表示对方近期没开过 app（数据失效）
     * @param recentSpeedKmh 最近移动速度，null 表示无数据
     * @param lastBehavior 最近一次 app 内动作（影响 ±0.2）
     */
    fun compute(
        now: LocalDateTime = LocalDateTime.now(),
        myLoc: Pair<Double, Double>?,
        myLandmarks: List<Landmark>,
        partnerLoc: Pair<Double, Double>?,
        recentSpeedKmh: Double? = null,
        lastBehavior: Behavior = Behavior.NONE
    ): PoseResult {
        var score = 0.0
        val signals = mutableListOf<String>()

        // 时间维度
        val dow = now.dayOfWeek
        val hour = now.hour
        when {
            dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY -> {
                score += 0.3
                signals += "weekend"
            }
            hour in 8..18 -> {
                score -= 0.3
                signals += "weekday-day"
            }
            hour in 19..23 -> {
                score += 0.1
                signals += "weekday-evening"
            }
        }

        // 定位维度
        val inLandmark = myLoc != null && myLandmarks.any { lm ->
            distanceMeters(myLoc.first, myLoc.second, lm.lat, lm.lng) <= lm.radiusM
        }
        val nearestLandmarkM = myLoc?.let { (la, ln) ->
            myLandmarks.minOfOrNull { distanceMeters(la, ln, it.lat, it.lng) } ?: Double.MAX_VALUE
        } ?: Double.MAX_VALUE
        when {
            inLandmark -> { score -= 0.3; signals += "in-landmark" }
            nearestLandmarkM > 5000 -> { score += 0.3; signals += "far-from-landmark" }
        }
        if ((recentSpeedKmh ?: 0.0) > 50.0) { score += 0.2; signals += "moving-fast" }

        // 最近行为
        when (lastBehavior) {
            Behavior.JUST_FOCUSED -> { score -= 0.2; signals += "just-focused" }
            Behavior.JUST_SENT_LETTER -> { score += 0.2; signals += "just-letter" }
            Behavior.JUST_FED_PET -> { score += 0.1; signals += "just-pet" }
            Behavior.NONE -> {}
        }

        // 双人距离辅助
        val partnerDistKm = if (myLoc != null && partnerLoc != null) {
            distanceMeters(myLoc.first, myLoc.second, partnerLoc.first, partnerLoc.second) / 1000.0
        } else null
        when {
            partnerDistKm != null && partnerDistKm < 0.5 -> { score += 0.2; signals += "very-close" }
            partnerDistKm != null && partnerDistKm > 100 -> { score += 0.1; signals += "far-apart" }
        }

        val final = (0.5 + score).coerceIn(0.0, 1.0)
        val mode = when {
            partnerDistKm != null && partnerDistKm > 50 && (myLoc != null && partnerLoc != null) -> Mode.LONG_DISTANCE
            partnerDistKm != null && partnerDistKm < 0.5 -> Mode.TOGETHER
            inLandmark -> Mode.FOCUSED
            !inLandmark && nearestLandmarkM > 5000 -> Mode.AWAY
            else -> Mode.NEUTRAL
        }

        return PoseResult(
            score = final,
            mode = mode,
            partnerDistanceKm = partnerDistKm,
            signals = signals
        )
    }

    /**
     * Haversine 距离公式（米）。两个 (lat, lng) 算大圆弧距离。
     */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371000.0  // 地球半径
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}

enum class Behavior { NONE, JUST_FOCUSED, JUST_SENT_LETTER, JUST_FED_PET }

enum class Mode {
    /** 紧绷态：自习中 / 工作中。在常驻地标内 */
    FOCUSED,
    /** 陪伴态：两人物理上在一起 */
    TOGETHER,
    /** 出差态：你一个人远离常驻 */
    AWAY,
    /** 异地态：双人长期分两地（> 50km） */
    LONG_DISTANCE,
    /** 中性：信号都不强 */
    NEUTRAL
}

data class PoseResult(
    val score: Double,
    val mode: Mode,
    val partnerDistanceKm: Double?,
    val signals: List<String>
) {
    /** 给 Home 顶部 12sp 灰字用的诗化文案 */
    fun displayCaption(): String = when (mode) {
        Mode.FOCUSED -> "工作日 · 你们都在常驻地"
        Mode.TOGETHER -> "你们离得很近 · 走几步就能见到"
        Mode.AWAY -> "今天你一个人出门了"
        Mode.LONG_DISTANCE -> {
            val d = partnerDistanceKm?.toInt() ?: 0
            "TA 在 ${d}km 外的城市"
        }
        Mode.NEUTRAL -> "现在 · 想做点什么"
    }
}
