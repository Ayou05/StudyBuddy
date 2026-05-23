package com.studybuddy.v2.data.moment

import com.studybuddy.v2.data.repo.LocationRepo
import com.studybuddy.v2.data.repo.StatusRepo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 情境事件触发中枢。
 *
 * 把"位置上报 + detector 检查"打包，让外部（lifecycle / SSE / Worker / 用户操作）
 * 不需要分别拼这些步骤，调一个 [tick] 就行。
 *
 * Detector 只看数据，不知道是谁触发的。Hub 只编排不判定。
 *
 * 调用时机（参见 [V2App] / [HomeViewModel.refresh] / [MeetingDetector]）：
 * - app onResume / 进入前台
 * - Focus 完成 / 暂停结束
 * - SSE 收到对端 status 变化
 * - 周期心跳（暂时由 Home onResume 兜底，未来可加 Worker）
 */
@Singleton
class MomentTriggerHub @Inject constructor(
    private val locationRepo: LocationRepo,
    private val statusRepo: StatusRepo,
    private val meetingDetector: MeetingDetector,
    private val stayDetector: StayEventDetector
) {
    /**
     * 一次完整的"上报 + 判定" tick。
     * 失败任何一步都吞掉异常，避免拖累调用方。
     */
    suspend fun tick() {
        // 1. 报位置（高德 SDK 单次定位 ~1-3s）
        runCatching {
            locationRepo.currentOnce()?.let { latLng ->
                statusRepo.setCoarseLocation(latLng.lat, latLng.lng)
            }
        }
        // 2. 见面判定（写 PB meetings + emit Moment.MeetingStarted/Ended）
        runCatching { meetingDetector.check() }
        // 3. 共同停留判定（emit Moment.StayDetected，苹果手记式延迟）
        runCatching { stayDetector.check() }
    }

    /**
     * 仅 detector，不重新定位（用于 SSE 触发：partner 位置已通过 SSE 更新，本地不需重报）。
     */
    suspend fun checkOnly() {
        runCatching { meetingDetector.check() }
        runCatching { stayDetector.check() }
    }
}
