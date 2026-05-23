package com.studybuddy.v2.data.repo

import com.studybuddy.v2.util.Behavior
import com.studybuddy.v2.util.Mode
import com.studybuddy.v2.util.PoseCalculator
import com.studybuddy.v2.util.PoseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 姿态仓库 —— 计算 + 缓存。
 *
 * 调用时机（仅两个事件，绝不后台监听）：
 * 1. App 打开瞬间 → [refresh]
 * 2. App 内动作完成后（专注完成 / 寄信 / 喂猫）→ [refresh] with Behavior
 *
 * 缓存 30 分钟。其他界面读 [pose] 即可。
 */
@Singleton
class PoseRepo @Inject constructor(
    private val statusRepo: StatusRepo,
    private val landmarkRepo: LandmarkRepo,
    private val locationRepo: LocationRepo,
    private val userRepo: UserRepo
) {
    private val _pose = MutableStateFlow<PoseResult?>(null)
    val pose: StateFlow<PoseResult?> = _pose.asStateFlow()

    @Volatile private var lastComputeMs: Long = 0L
    private val CACHE_TTL_MS = 30 * 60 * 1000L  // 30 分钟

    suspend fun refresh(behavior: Behavior = Behavior.NONE, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastComputeMs < CACHE_TTL_MS && _pose.value != null) return

        val myLatLng = runCatching { locationRepo.currentOnce() }.getOrNull()
        val myLoc = myLatLng?.let { it.lat to it.lng }
        if (myLoc != null) {
            runCatching { statusRepo.setCoarseLocation(myLoc.first, myLoc.second) }
        }
        val landmarks = runCatching { landmarkRepo.myLandmarks() }.getOrDefault(emptyList())

        // 拉对方 lastKnownLoc（30 分钟内有效）
        val partner = userRepo.getPartner()
        val partnerLoc = if (partner != null) {
            val ps = runCatching { statusRepo.getPartnerStatus(partner.id) }.getOrNull()
            val freshEnough = ps != null &&
                    ps.lastLocAt > 0 &&
                    (now - ps.lastLocAt) < 30 * 60 * 1000L
            if (freshEnough && ps?.coarseLat != null && ps.coarseLng != null) {
                ps.coarseLat to ps.coarseLng
            } else null
        } else null

        val result = PoseCalculator.compute(
            myLoc = myLoc,
            myLandmarks = landmarks,
            partnerLoc = partnerLoc,
            recentSpeedKmh = null,  // P6+ 接入两次定位算速度
            lastBehavior = behavior
        )
        _pose.value = result
        lastComputeMs = now
    }

    fun current(): PoseResult? = _pose.value
}
