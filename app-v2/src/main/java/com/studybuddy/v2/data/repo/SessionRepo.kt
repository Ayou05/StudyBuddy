package com.studybuddy.v2.data.repo

import com.studybuddy.v2.data.model.FocusSession
import com.studybuddy.v2.data.model.FocusStatus
import com.studybuddy.v2.data.model.FocusType
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbException
import com.studybuddy.v2.data.room.SessionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 专注会话仓库。
 *
 * 双写策略：每次状态变化 → 立刻写 Room（保证 UI 响应）→ 异步写 PB（失败容忍，下次启动 PB 拉取覆盖）。
 * PB 是 source of truth；Room 仅作为离线缓存 + 本地查询。
 */
@Singleton
class SessionRepo @Inject constructor(
    private val pb: PbClient,
    private val dao: SessionDao,
    private val prefs: PreferencesStore,
    private val topicRepo: FocusTopicRepo
) {

    suspend fun startSession(
        userId: String,
        type: FocusType = FocusType.SINGLE,
        partnerId: String? = null,
        goal: String = "",
        plannedDurationMs: Long? = null,
        topicId: String? = null
    ): FocusSession {
        val now = System.currentTimeMillis()
        val effectivePlanned = plannedDurationMs ?: (prefs.focusDurationMin.first() * 60_000L)
        // 1) 先尝试在 PB 上创建（拿真实 id）
        val pbRecord: FocusSession? = try {
            pb.createRecord<FocusSession>(
                collection = PbConfig.SESSIONS,
                fields = mapOf(
                    "userId" to userId,
                    "partnerId" to partnerId,
                    "type" to type.name,
                    "status" to FocusStatus.ACTIVE.name,
                    "goal" to goal,
                    "startedAt" to now,
                    "totalPausedMs" to 0,
                    "plannedDurationMs" to effectivePlanned,
                    "breakCount" to 0,
                    "topicId" to topicId
                )
            )
        } catch (_: PbException) { null } catch (_: Exception) { null }

        val session = pbRecord ?: FocusSession(
            id = "local_$now",
            userId = userId, partnerId = partnerId,
            type = type.name, status = FocusStatus.ACTIVE.name,
            goal = goal, startedAt = now,
            plannedDurationMs = effectivePlanned,
            topicId = topicId
        )
        dao.upsert(session)
        return session
    }

    suspend fun pauseSession(session: FocusSession): FocusSession {
        val now = System.currentTimeMillis()
        val updated = session.copy(status = FocusStatus.PAUSED.name, pausedAt = now)
        dao.upsert(updated)
        tryPbUpdate(updated.id, mapOf("status" to FocusStatus.PAUSED.name, "pausedAt" to now))
        return updated
    }

    suspend fun resumeSession(session: FocusSession): FocusSession {
        val now = System.currentTimeMillis()
        val extraPaused = session.pausedAt?.let { now - it } ?: 0L
        val updated = session.copy(
            status = FocusStatus.ACTIVE.name,
            pausedAt = null,
            totalPausedMs = session.totalPausedMs + extraPaused
        )
        dao.upsert(updated)
        tryPbUpdate(updated.id, mapOf(
            "status" to FocusStatus.ACTIVE.name,
            "pausedAt" to null,
            "totalPausedMs" to updated.totalPausedMs
        ))
        return updated
    }

    suspend fun completeSession(session: FocusSession): FocusSession {
        val now = System.currentTimeMillis()
        val actual = (now - session.startedAt - session.totalPausedMs).coerceAtLeast(0)
        // 简单积分公式：每完整 25 分钟 +10 点，比例累计
        val points = ((actual / 60_000.0) / 25.0 * 10).toInt().coerceAtLeast(0)
        val updated = session.copy(
            status = FocusStatus.COMPLETED.name,
            endedAt = now,
            actualDurationMs = actual,
            pointsEarned = points
        )
        dao.upsert(updated)
        tryPbUpdate(updated.id, mapOf(
            "status" to FocusStatus.COMPLETED.name,
            "endedAt" to now,
            "actualDurationMs" to actual,
            "pointsEarned" to points
        ))
        // 累加到主题（如果有绑定）
        session.topicId?.let { tid -> topicRepo.bumpSession(tid, actual) }
        return updated
    }

    suspend fun abortSession(session: FocusSession): FocusSession {
        val now = System.currentTimeMillis()
        val actual = (now - session.startedAt - session.totalPausedMs).coerceAtLeast(0)
        val updated = session.copy(
            status = FocusStatus.ABORTED.name,
            endedAt = now,
            actualDurationMs = actual
        )
        dao.upsert(updated)
        tryPbUpdate(updated.id, mapOf(
            "status" to FocusStatus.ABORTED.name,
            "endedAt" to now,
            "actualDurationMs" to actual
        ))
        return updated
    }

    suspend fun getActiveSession(userId: String): FocusSession? = dao.getActive(userId)

    fun observeActive(userId: String): Flow<List<FocusSession>> = dao.observeActive(userId)

    /** 今日累计专注分钟数（来自本地 Room）。 */
    suspend fun getTodayMinutes(userId: String): Int {
        val startOfDayMs = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val ms = dao.getCompletedDurationSince(userId, startOfDayMs)
        return (ms / 60_000L).toInt()
    }

    /** 拉某一天内的所有 session（详情页用）。ymd 格式 "yyyy-MM-dd"。 */
    suspend fun sessionsForDay(userId: String, ymd: String): List<FocusSession> {
        val date = LocalDate.parse(ymd)
        val startMs = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMs = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return dao.sessionsForDay(userId, startMs, endMs)
    }

    private suspend fun tryPbUpdate(id: String, fields: Map<String, Any?>) {
        if (id.startsWith("local_")) return  // 本地占位 id 没法 update PB
        try { pb.updateRecord<FocusSession>(PbConfig.SESSIONS, id, fields) } catch (_: Exception) {}
    }
}
