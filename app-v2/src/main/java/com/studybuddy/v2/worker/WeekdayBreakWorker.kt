package com.studybuddy.v2.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studybuddy.v2.data.repo.FundRepo
import com.studybuddy.v2.data.repo.PreferencesStore
import com.studybuddy.v2.data.repo.SessionRepo
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * 工作日断连判定 Worker。每天本地零点过后跑一次，判定昨天工作日是否达标，
 * 未达标自动给共同金库入一笔（"工作日断连 · 2026-05-21 · 12/25 分钟"）。
 *
 * 替换原来写"欠条"的逻辑——账本概念已合并入金库，断连入金即可。
 *
 * 兜底：app 启动时也调一次 [runOnDemand]，处理"几天没开过 app"的情况——
 * 从 lastWeekdayCheckedYmd 一路补到昨天，避免 Worker 因系统重启 / 电池优化漏跑。
 */
class WeekdayBreakWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val ep = EntryPointAccessors.fromApplication(
                applicationContext,
                WeekdayBreakWorkerEntryPoint::class.java
            )
            checkBackfill(ep.preferencesStore(), ep.fundRepo(), ep.sessionRepo())
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val NAME = "weekday_break_check"

        suspend fun checkBackfill(
            prefs: PreferencesStore,
            fund: FundRepo,
            sessions: SessionRepo
        ) {
            if (!prefs.weekdayBreakEnabled.first()) return
            val today = LocalDate.now()
            val lastYmd = prefs.lastWeekdayCheckedYmd.first()
            val from = lastYmd?.let {
                runCatching { LocalDate.parse(it).plusDays(1) }.getOrNull()
            } ?: today.minusDays(1)
            var cursor = from
            while (cursor.isBefore(today)) {
                val ymd = cursor.toString()
                runCatching { fund.checkAndPenalizeWeekdayBreak(ymd, sessions) }
                prefs.setLastWeekdayCheckedYmd(ymd)
                cursor = cursor.plusDays(1)
            }
        }

        suspend fun runOnDemand(prefs: PreferencesStore, fund: FundRepo, sessions: SessionRepo) {
            checkBackfill(prefs, fund, sessions)
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WeekdayBreakWorkerEntryPoint {
    fun preferencesStore(): PreferencesStore
    fun fundRepo(): FundRepo
    fun sessionRepo(): SessionRepo
}
