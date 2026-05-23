package com.studybuddy.v2.data.moment

import com.studybuddy.v2.data.repo.PreferencesStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Moment 频率门控 + 用户主动 dismiss 持久化。
 *
 * 同 type 24h 内不重复发射；用户 dismiss 立刻标记 24h 抑制。
 * 状态以 JSON `{type: lastEmittedEpochMs}` 写入 PreferencesStore，App 重启后仍然生效。
 *
 * 注意：MeetingEnded / WeekdayBreakNoticed 等"事件性"Moment 用 24h 是合理的；
 * MeetingStarted 这种持续状态用 6h 让长见面期间还能再被提醒一次。
 */
@Singleton
class MomentSuppressor @Inject constructor(
    private val prefs: PreferencesStore
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** 同 type 抑制窗口（毫秒）。MeetingStarted 短一点，其它 24h。 */
    private fun windowMsFor(type: String): Long = when (type) {
        "meeting_started" -> 6 * 60 * 60 * 1000L
        else -> 24 * 60 * 60 * 1000L
    }

    /** 是否应该抑制 —— disabled 直接 true；否则距离上次 emit 不到窗口期就 true。 */
    suspend fun shouldSuppress(type: String, now: Long = System.currentTimeMillis()): Boolean {
        // 用户在 Settings 关掉了这种 type
        val disabledCsv = prefs.momentDisabledTypes.first()
        if (disabledCsv.split(",").any { it.trim() == type }) return true
        // 娱乐模式：抑制专注向 banner（断连入金库 / TA 开始专注）
        if (prefs.appMode.first() == "LEISURE" &&
            (type == "weekday_break_noticed" || type == "partner_started_focus")) {
            return true
        }
        val map = readMap()
        val last = map[type] ?: return false
        return now - last < windowMsFor(type)
    }

    /** 标记此 type 已 emit / dismiss，开始计窗。 */
    suspend fun markShown(type: String, now: Long = System.currentTimeMillis()) {
        val map = readMap().toMutableMap()
        map[type] = now
        write(map)
    }

    private suspend fun readMap(): Map<String, Long> {
        val raw = prefs.momentSuppressionJson.first()
        return runCatching { json.decodeFromString<Map<String, Long>>(raw) }.getOrDefault(emptyMap())
    }

    private suspend fun write(map: Map<String, Long>) {
        val raw = json.encodeToString(map)
        prefs.setMomentSuppressionJson(raw)
    }
}
