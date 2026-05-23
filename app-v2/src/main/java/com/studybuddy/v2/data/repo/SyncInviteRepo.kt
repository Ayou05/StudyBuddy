package com.studybuddy.v2.data.repo

import com.studybuddy.v2.data.model.SyncInvite
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbException
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 同步专注邀请。60s 默认有效期，超时自动 EXPIRED 由对端轮询/SSE 兜底。
 */
@Singleton
class SyncInviteRepo @Inject constructor(
    private val pb: PbClient,
    private val prefs: PreferencesStore
) {
    suspend fun sendInvite(
        toUserId: String,
        plannedDurationMs: Long,
        mode: String,
        goal: String
    ): SyncInvite? {
        val me = prefs.currentUserId.first() ?: return null
        val now = System.currentTimeMillis()
        val fields = mapOf(
            "fromUserId" to me,
            "toUserId" to toUserId,
            "plannedDurationMs" to plannedDurationMs,
            "mode" to mode,
            "goal" to goal,
            "status" to "PENDING",
            "createdAt" to now,
            "expiresAt" to now + 60_000L
        )
        return try {
            pb.createRecord<SyncInvite>(PbConfig.SYNC_INVITES, fields)
        } catch (_: PbException) { null }
    }

    suspend fun accept(inviteId: String, sessionId: String?): Boolean {
        return try {
            val fields = mutableMapOf<String, Any?>("status" to "ACCEPTED")
            if (sessionId != null) fields["sessionId"] = sessionId
            pb.updateRecord<SyncInvite>(PbConfig.SYNC_INVITES, inviteId, fields)
            true
        } catch (_: PbException) { false }
    }

    suspend fun decline(inviteId: String): Boolean {
        return try {
            pb.updateRecord<SyncInvite>(
                PbConfig.SYNC_INVITES, inviteId,
                mapOf("status" to "DECLINED")
            )
            true
        } catch (_: PbException) { false }
    }

    suspend fun cancel(inviteId: String): Boolean {
        return try {
            pb.updateRecord<SyncInvite>(
                PbConfig.SYNC_INVITES, inviteId,
                mapOf("status" to "CANCELLED")
            )
            true
        } catch (_: PbException) { false }
    }

    /** 拉一次最近未处理的对我的邀请（启动时补漏） */
    suspend fun fetchPendingForMe(): SyncInvite? {
        val me = prefs.currentUserId.first() ?: return null
        return try {
            val list = pb.listRecords<SyncInvite>(
                collection = PbConfig.SYNC_INVITES,
                filter = "toUserId='$me' && status='PENDING'",
                sort = "-createdAt",
                perPage = 1
            )
            list.items.firstOrNull()?.takeIf {
                it.expiresAt > System.currentTimeMillis()
            }
        } catch (_: PbException) { null }
    }
}
