package com.studybuddy.v2.data.repo

import android.content.Context
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.studybuddy.v2.data.model.Landmark
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLng(val lat: Double, val lng: Double) {
    fun distanceMeters(other: LatLng): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(other.lat - lat)
        val dLng = Math.toRadians(other.lng - lng)
        val a = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) *
            sin(dLng / 2).let { it * it }
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}

@Singleton
class LocationRepo @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val landmarkRepo: LandmarkRepo
) {
    /**
     * 单次精确定位。失败/超时返回 null。
     * 不申请权限，调用方需保证 ACCESS_FINE_LOCATION 已授权；未授权会直接 onLocationChanged
     * 给一个 errorCode != 0 的结果，我们当 null 处理。
     */
    suspend fun currentOnce(timeoutMs: Long = 6_000): LatLng? = suspendCancellableCoroutine { cont ->
        val client = try { AMapLocationClient(ctx) } catch (_: Exception) {
            if (cont.isActive) cont.resume(null); return@suspendCancellableCoroutine
        }
        client.setLocationOption(AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            isOnceLocation = true
            httpTimeOut = timeoutMs
            isNeedAddress = false
        })
        client.setLocationListener { loc ->
            if (cont.isActive) {
                if (loc != null && loc.errorCode == 0) {
                    cont.resume(LatLng(loc.latitude, loc.longitude))
                } else {
                    cont.resume(null)
                }
            }
            client.stopLocation()
            client.onDestroy()
        }
        client.startLocation()
        cont.invokeOnCancellation {
            try { client.stopLocation(); client.onDestroy() } catch (_: Exception) {}
        }
    }

    /** 当前定位最近且在半径内的地标。没命中返回 null。 */
    suspend fun nearestLandmark(): Landmark? {
        val here = currentOnce() ?: return null
        val landmarks = landmarkRepo.myLandmarks()
        return landmarks.minByOrNull { LatLng(it.lat, it.lng).distanceMeters(here) }
            ?.takeIf { LatLng(it.lat, it.lng).distanceMeters(here) <= it.radiusM }
    }
}
