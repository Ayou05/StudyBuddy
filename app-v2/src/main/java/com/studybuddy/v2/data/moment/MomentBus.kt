package com.studybuddy.v2.data.moment

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 情境（Moment）—— 替代姿态系统的"事件驱动模态层"。
 *
 * 设计立场：
 * - 不做"模式切换"，做"事件驱动的临时浮卡"
 * - app 平时是稳定 4 tab，特定事件浮出 Banner，用户处理或滑掉
 * - 频率门控：同种 24h 不重复
 * - 可禁用：Settings 单独关每种
 * - 不发系统通知：app 内浮起，不打扰
 */
sealed class Moment {
    abstract val id: String
    abstract val createdAt: Long
    abstract val type: String

    /** 见面开始 */
    data class MeetingStarted(
        override val id: String,
        override val createdAt: Long = System.currentTimeMillis(),
        val durationMin: Long,
        val locationName: String? = null
    ) : Moment() {
        override val type = "meeting_started"
    }

    /** 见面结束（离别）—— 异地党最浓情绪点 */
    data class MeetingEnded(
        override val id: String,
        override val createdAt: Long = System.currentTimeMillis(),
        val totalMs: Long,
        val meetingId: String
    ) : Moment() {
        override val type = "meeting_ended"
    }

    /** 共同停留（同地点 > 1h，回到常驻后浮提示） */
    data class StayDetected(
        override val id: String,
        override val createdAt: Long = System.currentTimeMillis(),
        val placeName: String,
        val durationMin: Long,
        val landmarkId: String? = null
    ) : Moment() {
        override val type = "stay_detected"
    }

    /** TA 开始专注 */
    data class PartnerStartedFocus(
        override val id: String,
        override val createdAt: Long = System.currentTimeMillis(),
        val partnerName: String,
        val plannedMin: Int
    ) : Moment() {
        override val type = "partner_started_focus"
    }

    /** 工作日断连 → 已入金库（"昨天偷懒了"） */
    data class WeekdayBreakNoticed(
        override val id: String,
        override val createdAt: Long = System.currentTimeMillis(),
        val ymd: String,
        val actualMin: Int,
        val threshold: Int,
        val penalizedYuan: Int
    ) : Moment() {
        override val type = "weekday_break_noticed"
    }
}

@Singleton
class MomentBus @Inject constructor(
    private val suppressor: MomentSuppressor
) {
    private val _events = MutableSharedFlow<Moment>(
        replay = 1,
        extraBufferCapacity = 16
    )
    val events: SharedFlow<Moment> = _events.asSharedFlow()

    /**
     * 发射一个 Moment。同 type 24h 内自动抑制（MeetingStarted 6h），不会到达 collect 端。
     * 发射成功时记录 lastShown 时间。
     */
    suspend fun emit(moment: Moment) {
        if (suppressor.shouldSuppress(moment.type)) return
        suppressor.markShown(moment.type)
        _events.emit(moment)
    }

    /**
     * 用户 dismiss 时调用，立即标记 24h 抑制（避免 dismiss 后 banner 又被立刻 emit 出来）。
     */
    suspend fun markDismissed(moment: Moment) {
        suppressor.markShown(moment.type)
    }
}
