package com.studybuddy.v2.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studybuddy.v2.data.model.FocusSession
import com.studybuddy.v2.data.model.Pet
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: FocusSession)

    @Update
    suspend fun update(session: FocusSession)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: String): FocusSession?

    @Query("SELECT * FROM sessions WHERE userId = :userId AND status IN ('ACTIVE','PAUSED') ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActive(userId: String): FocusSession?

    @Query("SELECT * FROM sessions WHERE userId = :userId AND status IN ('ACTIVE','PAUSED')")
    fun observeActive(userId: String): Flow<List<FocusSession>>

    @Query("SELECT COALESCE(SUM(actualDurationMs), 0) FROM sessions WHERE userId = :userId AND status = 'COMPLETED' AND startedAt >= :sinceMs")
    suspend fun getCompletedDurationSince(userId: String, sinceMs: Long): Long

    /** 返回 [sinceMs, untilMs) 内每天的累计专注毫秒数（按 startedAt 落入哪天）。 */
    @Query("""
        SELECT startedAt, actualDurationMs FROM sessions
        WHERE userId = :userId AND status = 'COMPLETED'
        AND startedAt >= :sinceMs AND startedAt < :untilMs
        ORDER BY startedAt ASC
    """)
    suspend fun getCompletedSessionsBetween(userId: String, sinceMs: Long, untilMs: Long): List<SessionTimeRow>

    /** 拉某一天内的所有 session（含 ACTIVE/PAUSED/COMPLETED/ABORTED），按时间倒序，给详情页用 */
    @Query("""
        SELECT * FROM sessions
        WHERE userId = :userId
        AND startedAt >= :sinceMs AND startedAt < :untilMs
        ORDER BY startedAt DESC
    """)
    suspend fun sessionsForDay(userId: String, sinceMs: Long, untilMs: Long): List<FocusSession>

    /** 按 topicId 聚合时长，给 Stats 多主题分布饼图用 */
    @Query("""
        SELECT topicId, COALESCE(SUM(actualDurationMs), 0) as totalMs FROM sessions
        WHERE userId = :userId AND status = 'COMPLETED'
        AND startedAt >= :sinceMs AND startedAt < :untilMs
        GROUP BY topicId
    """)
    suspend fun topicDistribution(userId: String, sinceMs: Long, untilMs: Long): List<TopicTimeRow>
}

data class TopicTimeRow(
    val topicId: String?,
    val totalMs: Long
)

data class SessionTimeRow(
    val startedAt: Long,
    val actualDurationMs: Long?
)

@Dao
interface PetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pet: Pet)

    @Query("SELECT * FROM pets WHERE pairId = :pairId LIMIT 1")
    suspend fun getByPair(pairId: String): Pet?

    @Query("SELECT * FROM pets WHERE id = :id")
    fun observeById(id: String): Flow<Pet?>
}
