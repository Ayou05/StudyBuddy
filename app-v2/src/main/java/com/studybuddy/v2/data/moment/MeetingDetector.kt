package com.studybuddy.v2.data.moment

import com.studybuddy.v2.data.repo.LocationRepo
import com.studybuddy.v2.data.repo.MeetingRepo
import com.studybuddy.v2.data.repo.PairLandmarkRepo
import com.studybuddy.v2.data.repo.StatusRepo
import com.studybuddy.v2.data.repo.UserRepo
import com.studybuddy.v2.util.PoseCalculator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 见面 + 共同停留 detector。
 *
 * App 打开时调 [check]。规则：
 *
 * 见面：
 * - 双方 lastKnownLoc 都在 30 分钟内且距离 < 500m
 *   → 没开放 meeting 则创建；有则不动
 * - 距离 > 1000m
 *   → 关闭最近开放 meeting，emit MeetingEnded
 *
 * 共同停留（苹果手记式延迟提示）：
 * - 双方某地停留 > 60 分钟（基于 lastKnownLoc 时间）
 * - 用户当前回到了自己常驻地标内
 * - 该地点在共享地标里没有近期记录
 * - emit StayDetected
 *
 * 见面阈值 60 分钟（吃饭 + 散步级别），由 MeetingRepo 统一管理。
 */
@Singleton
class MeetingDetector @Inject constructor(
    private val statusRepo: StatusRepo,
    private val meetingRepo: MeetingRepo,
    private val locationRepo: LocationRepo,
    private val userRepo: UserRepo,
    private val pairLandmarkRepo: PairLandmarkRepo,
    private val momentBus: MomentBus
) {
    suspend fun check() {
        val partner = userRepo.getPartner() ?: return
        val rel = userRepo.getRelationship() ?: return
        val myStatus = statusRepo.getMyStatus()
        val partnerStatus = statusRepo.getPartnerStatus(partner.id)

        if (myStatus?.coarseLat == null || partnerStatus?.coarseLat == null) return
        val myLat = myStatus.coarseLat ?: return
        val myLng = myStatus.coarseLng ?: return
        val pLat = partnerStatus.coarseLat ?: return
        val pLng = partnerStatus.coarseLng ?: return

        val now = System.currentTimeMillis()
        val freshWindow = 30 * 60 * 1000L  // 30 分钟
        val myFresh = (now - myStatus.lastLocAt) < freshWindow
        val pFresh = (now - partnerStatus.lastLocAt) < freshWindow
        if (!myFresh || !pFresh) return

        val distM = PoseCalculator.distanceMeters(myLat, myLng, pLat, pLng)
        val open = meetingRepo.currentOpen()

        when {
            distM < 500.0 && open == null -> {
                // 创建新 meeting
                val centerLat = (myLat + pLat) / 2
                val centerLng = (myLng + pLng) / 2
                val created = meetingRepo.open(centerLat, centerLng)
                if (created != null) {
                    momentBus.emit(Moment.MeetingStarted(
                        id = "meet_${created.id}",
                        durationMin = 0,
                        locationName = null
                    ))
                }
            }
            distM < 500.0 && open != null -> {
                // 见面进行中，emit 一次更新（频率门控由 UI 层控制）
                val durMin = (now - open.startedAt) / 60_000L
                if (durMin >= 60) {
                    // 满 60 分钟才发情境（避免擦肩而过被认为见面）
                    momentBus.emit(Moment.MeetingStarted(
                        id = "meet_${open.id}_$durMin",
                        durationMin = durMin,
                        locationName = open.locationName
                    ))
                }
            }
            distM > 1000.0 && open != null -> {
                // 关闭见面
                val closed = meetingRepo.close(open.id)
                if (closed != null && closed.durationMs >= 60 * 60_000L) {
                    momentBus.emit(Moment.MeetingEnded(
                        id = "meet_end_${closed.id}",
                        totalMs = closed.durationMs,
                        meetingId = closed.id
                    ))
                }
            }
        }
    }
}
