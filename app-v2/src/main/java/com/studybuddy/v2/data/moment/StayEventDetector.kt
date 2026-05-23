package com.studybuddy.v2.data.moment

import com.studybuddy.v2.data.repo.LocationRepo
import com.studybuddy.v2.data.repo.PairLandmarkRepo
import com.studybuddy.v2.data.repo.StatusRepo
import com.studybuddy.v2.data.repo.UserRepo
import com.studybuddy.v2.util.PoseCalculator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 共同停留 detector —— 苹果手记式"延迟提示"。
 *
 * 设计立场（重要）：
 * - 不打扰人在停留中：不当场推 banner
 * - 用户回家后才提醒："今天去了 XX，要写一段记下来吗?"
 * - 频率门由 [MomentSuppressor] 控制（24h 同 type 不重发），所以这里不再做时间窗
 *
 * 状态机（in-memory + last-emitted 持久化都在 MomentSuppressor 里）：
 * - 双方都在某个共享地标 200m 内 + 持续 ≥ 60min  → 内部记下 "ongoingStay"
 * - 距离突变（双方位置变化 > 500m 离开地标）+ ongoingStay 存在
 *   → emit StayDetected(landmarkName, durationMin)，清 ongoingStay
 *
 * 简化：用单实例 in-memory state（app 杀掉就丢，没关系——见面/停留再触发就再算）。
 */
@Singleton
class StayEventDetector @Inject constructor(
    private val statusRepo: StatusRepo,
    private val userRepo: UserRepo,
    private val pairLandmarkRepo: PairLandmarkRepo,
    private val locationRepo: LocationRepo,
    private val momentBus: MomentBus
) {
    private data class OngoingStay(
        val landmarkId: String?,
        val landmarkName: String,
        val startedAt: Long
    )

    private val mutex = Mutex()
    private var ongoing: OngoingStay? = null

    suspend fun check() = mutex.withLock {
        val partner = userRepo.getPartner() ?: return
        val myStatus = statusRepo.getMyStatus() ?: return
        val partnerStatus = statusRepo.getPartnerStatus(partner.id) ?: return

        val myLat = myStatus.coarseLat ?: return
        val myLng = myStatus.coarseLng ?: return
        val pLat = partnerStatus.coarseLat ?: return
        val pLng = partnerStatus.coarseLng ?: return

        val now = System.currentTimeMillis()
        val freshWindow = 30 * 60 * 1000L
        val myFresh = (now - myStatus.lastLocAt) < freshWindow
        val pFresh = (now - partnerStatus.lastLocAt) < freshWindow
        if (!myFresh || !pFresh) return

        val pairDistM = PoseCalculator.distanceMeters(myLat, myLng, pLat, pLng)
        // 双方相距 > 1km 视作分开 → 如果有 ongoing stay 且持续 ≥ 60min，emit
        if (pairDistM > 1000.0) {
            ongoing?.let { st ->
                val durMin = (now - st.startedAt) / 60_000L
                if (durMin >= 60) {
                    momentBus.emit(Moment.StayDetected(
                        id = "stay_${st.landmarkId ?: "anon"}_${st.startedAt}",
                        placeName = st.landmarkName,
                        durationMin = durMin,
                        landmarkId = st.landmarkId
                    ))
                }
                ongoing = null
            }
            return
        }

        if (pairDistM > 500.0) return  // 双方还没真在一起，先不开 stay

        // 双方在 500m 内 → 看是否在某个共享地标里
        val centerLat = (myLat + pLat) / 2
        val centerLng = (myLng + pLng) / 2
        val nearby = pairLandmarkRepo.findNearby(centerLat, centerLng, radiusM = 200)

        if (nearby != null) {
            val cur = ongoing
            if (cur == null || cur.landmarkId != nearby.id) {
                ongoing = OngoingStay(
                    landmarkId = nearby.id,
                    landmarkName = nearby.name.ifBlank { "那里" },
                    startedAt = now
                )
            }
            return
        }

        // 不在已知地标但仍在 500m 内：算一个匿名 stay（landmarkName 暂时给 "在一起"）
        if (ongoing == null) {
            ongoing = OngoingStay(
                landmarkId = null,
                landmarkName = "你们在一起的地方",
                startedAt = now
            )
        }
    }
}
