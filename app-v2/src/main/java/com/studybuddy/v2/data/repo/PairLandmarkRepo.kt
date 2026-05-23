package com.studybuddy.v2.data.repo

import com.studybuddy.v2.data.model.PairLandmark
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbException
import com.studybuddy.v2.util.PoseCalculator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 共享地标仓库 —— 你们一起去过的地方（咖啡馆 / 电影院 / 旅游地）。
 *
 * 跟私有 [LandmarkRepo] 区分：
 * - 私有 = user 维度，专注加成判定
 * - 共享 = pair 维度，"探索 tab 共在态展示" + "便签的容器"
 *
 * 添加方式 = 苹果手记式延迟提示（不当场打扰，等用户回家再问），见 StayEventDetector
 */
@Singleton
class PairLandmarkRepo @Inject constructor(
    private val pb: PbClient,
    private val userRepo: UserRepo
) {
    suspend fun list(): List<PairLandmark> {
        val rel = userRepo.getRelationship() ?: return emptyList()
        return try {
            pb.listRecords<PairLandmark>(
                collection = PbConfig.PAIR_LANDMARKS,
                filter = "pairId='${rel.id}'",
                sort = "-lastVisitedAt",
                perPage = 100
            ).items
        } catch (_: PbException) { emptyList() }
    }

    /** 找 [lat, lng] 附近 200m 内的已有共享地标，没找到返回 null。 */
    suspend fun findNearby(lat: Double, lng: Double, radiusM: Int = 200): PairLandmark? {
        return list().firstOrNull { lm ->
            PoseCalculator.distanceMeters(lat, lng, lm.lat, lm.lng) <= radiusM
        }
    }

    suspend fun create(name: String, lat: Double, lng: Double): PairLandmark? {
        val rel = userRepo.getRelationship() ?: return null
        val now = System.currentTimeMillis()
        return try {
            pb.createRecord<PairLandmark>(PbConfig.PAIR_LANDMARKS, mapOf(
                "pairId" to rel.id,
                "name" to name,
                "lat" to lat,
                "lng" to lng,
                "firstVisitedAt" to now,
                "lastVisitedAt" to now,
                "visitCount" to 1
            ))
        } catch (_: PbException) { null }
    }

    suspend fun bumpVisit(id: String) {
        try {
            val current = pb.getRecord<PairLandmark>(PbConfig.PAIR_LANDMARKS, id)
            pb.updateRecord<PairLandmark>(
                collection = PbConfig.PAIR_LANDMARKS,
                id = id,
                fields = mapOf(
                    "visitCount" to (current.visitCount + 1),
                    "lastVisitedAt" to System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {}
    }
}
