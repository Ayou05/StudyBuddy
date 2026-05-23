package com.studybuddy.v2.data.repo

import com.studybuddy.v2.data.model.FocusTopic
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbException
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 多主题专注仓库 —— "鞍部风铭牌"。
 *
 * 设计立场：
 * - 主题不衰减、不"养成"，只累计 totalFocusMs 和 sessionCount
 * - 用户可建多个主题，archivedAt 标记归档（不删，保留累计）
 * - FocusSession.topicId 关联，session 完成时累加 totalFocusMs
 *
 * 不做"伙伴拟人"——避免和鞍部猫"陪伴"语义冲突；只是事项标签。
 */
@Singleton
class FocusTopicRepo @Inject constructor(
    private val pb: PbClient,
    private val prefs: PreferencesStore
) {
    /** 拉所有未归档主题 */
    suspend fun list(includeArchived: Boolean = false): List<FocusTopic> {
        val uid = prefs.currentUserId.first() ?: return emptyList()
        val filter = if (includeArchived) "userId='$uid'"
                     else "userId='$uid' && archivedAt=null"
        return try {
            pb.listRecords<FocusTopic>(
                collection = PbConfig.FOCUS_TOPICS,
                filter = filter,
                sort = "-totalFocusMs",
                perPage = 50
            ).items
        } catch (_: PbException) { emptyList() }
    }

    suspend fun get(id: String): FocusTopic? = try {
        pb.getRecord<FocusTopic>(PbConfig.FOCUS_TOPICS, id)
    } catch (_: PbException) { null }

    suspend fun create(name: String, colorHex: String = "#CC785C"): FocusTopic? {
        val uid = prefs.currentUserId.first() ?: return null
        val cleaned = name.trim().take(16)
        if (cleaned.isEmpty()) return null
        return try {
            pb.createRecord<FocusTopic>(PbConfig.FOCUS_TOPICS, mapOf(
                "userId" to uid,
                "name" to cleaned,
                "colorHex" to colorHex,
                "totalFocusMs" to 0L,
                "sessionCount" to 0,
                "createdAt" to System.currentTimeMillis()
            ))
        } catch (_: PbException) { null }
    }

    suspend fun rename(id: String, newName: String): Boolean {
        val cleaned = newName.trim().take(16)
        if (cleaned.isEmpty()) return false
        return try {
            pb.updateRecord<FocusTopic>(
                collection = PbConfig.FOCUS_TOPICS,
                id = id,
                fields = mapOf("name" to cleaned)
            )
            true
        } catch (_: PbException) { false }
    }

    suspend fun setColor(id: String, colorHex: String): Boolean = try {
        pb.updateRecord<FocusTopic>(
            collection = PbConfig.FOCUS_TOPICS,
            id = id,
            fields = mapOf("colorHex" to colorHex)
        )
        true
    } catch (_: PbException) { false }

    suspend fun archive(id: String): Boolean = try {
        pb.updateRecord<FocusTopic>(
            collection = PbConfig.FOCUS_TOPICS,
            id = id,
            fields = mapOf("archivedAt" to System.currentTimeMillis())
        )
        true
    } catch (_: PbException) { false }

    /** session 完成时累加该主题的总时长 */
    suspend fun bumpSession(topicId: String, durationMs: Long) {
        if (topicId.isBlank() || durationMs <= 0) return
        try {
            val current = pb.getRecord<FocusTopic>(PbConfig.FOCUS_TOPICS, topicId)
            pb.updateRecord<FocusTopic>(
                collection = PbConfig.FOCUS_TOPICS,
                id = topicId,
                fields = mapOf(
                    "totalFocusMs" to (current.totalFocusMs + durationMs),
                    "sessionCount" to (current.sessionCount + 1)
                )
            )
        } catch (_: Exception) {}
    }
}
