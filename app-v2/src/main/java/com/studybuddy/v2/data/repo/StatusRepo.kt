package com.studybuddy.v2.data.repo

import com.studybuddy.v2.data.model.RealtimeStatus
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbException
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 实时状态仓库 —— "我现在在专注吗 / 今天累计多少 / 是否在图书馆"。
 * v2 P1 用 1Hz 简单 PATCH；P2/P3 接 SSE 双向同步。
 */
@Singleton
class StatusRepo @Inject constructor(
    private val pb: PbClient,
    private val prefs: PreferencesStore
) {
    suspend fun setFocusStatus(focusStatus: String, currentSec: Long, sessionId: String?) {
        val userId = prefs.currentUserId.first() ?: return
        upsertSelf(userId) { existing ->
            mapOf(
                "userId" to userId,
                "online" to true,
                "focusStatus" to focusStatus,
                "currentSessionId" to sessionId,
                "currentFocusSeconds" to currentSec,
                "todayFocusSeconds" to (existing?.todayFocusSeconds ?: 0),
                "lastHeartbeat" to System.currentTimeMillis()
            )
        }
    }

    suspend fun setOnline(online: Boolean) {
        val userId = prefs.currentUserId.first() ?: return
        upsertSelf(userId) { _ -> mapOf("userId" to userId, "online" to online, "lastHeartbeat" to System.currentTimeMillis()) }
    }

    /** 写入 500m 精度位置。每次开 app 拿一次定位时调用。 */
    suspend fun setCoarseLocation(lat: Double, lng: Double) {
        val userId = prefs.currentUserId.first() ?: return
        // 把坐标按 500m 精度网格化（约 0.0045°）
        val grid = 0.0045
        val gridLat = (Math.round(lat / grid) * grid)
        val gridLng = (Math.round(lng / grid) * grid)
        upsertSelf(userId) { _ ->
            mapOf(
                "userId" to userId,
                "coarseLat" to gridLat,
                "coarseLng" to gridLng,
                "lastLocAt" to System.currentTimeMillis(),
                "lastHeartbeat" to System.currentTimeMillis()
            )
        }
    }

    /** 拉自己当前的 status（含 coarse 位置）。null 表示还没有任何记录。 */
    suspend fun getMyStatus(): RealtimeStatus? {
        val userId = prefs.currentUserId.first() ?: return null
        return try {
            val list = pb.listRecords<RealtimeStatus>(
                collection = PbConfig.STATUS,
                filter = "userId='$userId'",
                perPage = 1
            )
            list.items.firstOrNull()
        } catch (_: PbException) { null }
    }

    suspend fun getPartnerStatus(partnerId: String): RealtimeStatus? {
        return try {
            val list = pb.listRecords<RealtimeStatus>(
                collection = PbConfig.STATUS,
                filter = "userId='$partnerId'",
                perPage = 1
            )
            list.items.firstOrNull()
        } catch (_: PbException) { null }
    }

    private suspend fun upsertSelf(userId: String, build: (RealtimeStatus?) -> Map<String, Any?>) {
        try {
            val list = pb.listRecords<RealtimeStatus>(
                collection = PbConfig.STATUS,
                filter = "userId='$userId'",
                perPage = 1
            )
            val existing = list.items.firstOrNull()
            val fields = build(existing)
            if (existing == null) {
                pb.createRecord<RealtimeStatus>(PbConfig.STATUS, fields)
            } else {
                pb.updateRecord<RealtimeStatus>(PbConfig.STATUS, existing.id, fields)
            }
        } catch (_: Exception) {
            // 心跳允许丢，下一秒再重试
        }
    }
}
