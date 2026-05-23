package com.studybuddy.v2

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.amap.api.maps.MapsInitializer
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbRealtime
import com.studybuddy.v2.data.repo.AuthRepo
import com.studybuddy.v2.data.repo.FundRepo
import com.studybuddy.v2.data.repo.PreferencesStore
import com.studybuddy.v2.data.repo.SessionRepo
import com.studybuddy.v2.worker.WeekdayBreakWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class V2App : Application() {

    @Inject lateinit var authRepo: AuthRepo
    @Inject lateinit var realtime: PbRealtime
    @Inject lateinit var prefs: PreferencesStore
    @Inject lateinit var fundRepo: FundRepo
    @Inject lateinit var sessionRepo: SessionRepo
    @Inject lateinit var momentHub: com.studybuddy.v2.data.moment.MomentTriggerHub
    @Inject lateinit var partnerFocusWatcher: com.studybuddy.v2.data.moment.PartnerFocusWatcher

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initAMap()
        scheduleWeekdayBreakWorker()
        appScope.launch {
            authRepo.restoreFromCache()
            // App 启动补算遗漏的工作日（系统重启 / 电池优化导致 Worker 没跑的兜底）
            runCatching { WeekdayBreakWorker.runOnDemand(prefs, fundRepo, sessionRepo) }
            // 启动后跑一次完整 tick（位置上报 + 见面 + 停留判定）
            runCatching { momentHub.tick() }
            // 监听 currentUserId 变化：登入 → 启动 SSE 并订阅；登出 → 停止
            authRepo.currentUserId.distinctUntilChanged().collect { uid ->
                if (uid.isNullOrBlank()) {
                    realtime.stop()
                    partnerFocusWatcher.stop()
                } else {
                    realtime.setSubscriptions(buildSubscriptions(uid))
                    realtime.start()
                    partnerFocusWatcher.start(appScope)
                }
            }
        }
    }

    private fun scheduleWeekdayBreakWorker() {
        val req = PeriodicWorkRequestBuilder<WeekdayBreakWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(2, TimeUnit.HOURS)  // 避免一启动就跑
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WeekdayBreakWorker.NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }

    private fun buildSubscriptions(uid: String): List<String> = listOf(
        "${PbConfig.USERS}/$uid",
        PbConfig.STATUS,
        "${PbConfig.SYNC_INVITES}",
        PbConfig.FUND_TRANSACTIONS,
        PbConfig.UNBIND_REQUESTS,
        PbConfig.RELATIONSHIPS
    )

    private fun initAMap() {
        // 高德 9.x+ 强制：隐私合规接口必须在任何 AMap API 调用前完成
        // 不调 → 地图灰底，不报错。
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
        MapsInitializer.setApiKey(AMAP_API_KEY)
    }

    companion object {
        private const val AMAP_API_KEY = "10a4f3621373c6bd50e71367d97452c6"
    }
}
