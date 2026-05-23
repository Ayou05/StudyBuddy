package com.studybuddy.v2.data.repo

import com.studybuddy.v2.data.model.Meeting
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 见面仓库 —— 异地党核心情感容器。
 *
 * 判定逻辑放在 [com.studybuddy.v2.util.MeetingDetector]（P6 接入），
 * 本 Repo 仅提供 CRUD 给 detector 和 UI 用。
 *
 * 见面阈值 60 分钟 —— 一起吃饭 + 散步算一次见面，避免擦肩而过被误判。
 */
@Singleton
class MeetingRepo @Inject constructor(
    private val pb: PbClient,
    private val userRepo: UserRepo
) {
    suspend fun list(): List<Meeting> {
        val rel = userRepo.getRelationship() ?: return emptyList()
        return try {
            pb.listRecords<Meeting>(
                collection = PbConfig.MEETINGS,
                filter = "pairId='${rel.id}'",
                sort = "-startedAt",
                perPage = 100
            ).items
        } catch (_: PbException) { emptyList() }
    }

    /** 拉当前进行中的 meeting（endedAt 为 null）。 */
    suspend fun currentOpen(): Meeting? {
        val rel = userRepo.getRelationship() ?: return null
        return try {
            pb.listRecords<Meeting>(
                collection = PbConfig.MEETINGS,
                filter = "pairId='${rel.id}' && endedAt=null",
                sort = "-startedAt",
                perPage = 1
            ).items.firstOrNull()
        } catch (_: PbException) { null }
    }

    suspend fun get(id: String): Meeting? = try {
        pb.getRecord<Meeting>(PbConfig.MEETINGS, id)
    } catch (_: PbException) { null }

    suspend fun open(centerLat: Double, centerLng: Double): Meeting? {
        val rel = userRepo.getRelationship() ?: return null
        val now = System.currentTimeMillis()
        return try {
            pb.createRecord<Meeting>(PbConfig.MEETINGS, mapOf(
                "pairId" to rel.id,
                "startedAt" to now,
                "centerLat" to centerLat,
                "centerLng" to centerLng,
                "durationMs" to 0L
            ))
        } catch (_: PbException) { null }
    }

    suspend fun close(id: String): Meeting? {
        val now = System.currentTimeMillis()
        return try {
            val current = get(id) ?: return null
            val duration = now - current.startedAt
            pb.updateRecord<Meeting>(
                collection = PbConfig.MEETINGS,
                id = id,
                fields = mapOf("endedAt" to now, "durationMs" to duration)
            )
        } catch (_: PbException) { null }
    }
}
