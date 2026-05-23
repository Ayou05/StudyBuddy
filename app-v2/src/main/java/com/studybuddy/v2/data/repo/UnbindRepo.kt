package com.studybuddy.v2.data.repo

import com.studybuddy.v2.data.model.UnbindRequest
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbException
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 解绑请求仓库 —— 7 天冷静期机制。
 *
 * 流程：
 * 1. 任一方 [request] → 创建 UnbindRequest，cooldownEndsAt = now + 7 天
 * 2. 7 天内任一方都可 [cancel] → 撤回，关系恢复
 * 3. 7 天后另一方/系统执行真解绑（这块 P5 不做，留 backlog）
 *
 * 期间宠物冬眠：[PetRepo.applyDecay] 检查到 [hasActive] 时跳过衰减。
 */
@Singleton
class UnbindRepo @Inject constructor(
    private val pb: PbClient,
    private val prefs: PreferencesStore,
    private val userRepo: UserRepo
) {
    private val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000

    /** 当前是否有未撤回的解绑请求。 */
    suspend fun hasActive(pairId: String): Boolean = activeFor(pairId) != null

    suspend fun activeFor(pairId: String): UnbindRequest? {
        return try {
            pb.listRecords<UnbindRequest>(
                collection = PbConfig.UNBIND_REQUESTS,
                filter = "pairId='$pairId' && cancelled=false",
                sort = "-createdAt",
                perPage = 1
            ).items.firstOrNull()
        } catch (_: PbException) { null }
    }

    suspend fun request(): UnbindRequest? {
        val me = prefs.currentUserId.first() ?: return null
        val rel = userRepo.getRelationship() ?: return null
        // 已有未撤回请求 → 不重复创建
        activeFor(rel.id)?.let { return it }
        val now = System.currentTimeMillis()
        return try {
            pb.createRecord<UnbindRequest>(PbConfig.UNBIND_REQUESTS, mapOf(
                "pairId" to rel.id,
                "byUserId" to me,
                "createdAt" to now,
                "cooldownEndsAt" to (now + SEVEN_DAYS_MS),
                "cancelled" to false
            ))
        } catch (_: PbException) { null }
    }

    suspend fun cancel(requestId: String): Boolean {
        val me = prefs.currentUserId.first() ?: return false
        return try {
            pb.updateRecord<UnbindRequest>(
                collection = PbConfig.UNBIND_REQUESTS,
                id = requestId,
                fields = mapOf(
                    "cancelled" to true,
                    "cancelledAt" to System.currentTimeMillis(),
                    "cancelledBy" to me
                )
            )
            true
        } catch (_: PbException) { false }
    }
}
