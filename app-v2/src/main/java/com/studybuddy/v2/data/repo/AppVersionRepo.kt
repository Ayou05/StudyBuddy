package com.studybuddy.v2.data.repo

import android.content.Context
import android.content.pm.PackageManager
import com.studybuddy.v2.data.model.AppVersion
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自更新检查仓库。
 *
 * 简化策略（封测期够用）：
 * - 拉 channel="beta" 的最新一条（按 publishedAt 倒序）
 * - 比 versionCode 决定是否提示
 * - listRule = "" 即任何已登录用户都能读
 *
 * Backlog：
 * - 强制升级（minSupportedCode > 本机）
 * - sha256 校验
 * - 增量更新
 */
@Singleton
class AppVersionRepo @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val pb: PbClient
) {
    val currentVersionCode: Int
        get() = try {
            val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            @Suppress("DEPRECATION")
            (if (android.os.Build.VERSION.SDK_INT >= 28) pi.longVersionCode.toInt() else pi.versionCode)
        } catch (e: PackageManager.NameNotFoundException) { 0 }

    val currentVersionName: String
        get() = try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "0.0.0"
        } catch (e: PackageManager.NameNotFoundException) { "0.0.0" }

    /**
     * 拉最新版本。当前模式 = 拉 channel="beta" 第一条（按 publishedAt 倒序）。
     * 找不到则 null。
     */
    suspend fun latest(channel: String = "beta"): AppVersion? {
        return try {
            pb.listRecords<AppVersion>(
                collection = PbConfig.APP_VERSIONS,
                filter = "channel='$channel'",
                sort = "-publishedAt",
                perPage = 1
            ).items.firstOrNull()
        } catch (e: PbException) {
            android.util.Log.e("AppVersionRepo", "latest failed: ${e.message}", e)
            null
        }
    }

    /** 检查是否有可用更新。返回最新版（仅当 versionCode > 本机时），否则 null。 */
    suspend fun checkUpdate(channel: String = "beta"): AppVersion? {
        val latest = latest(channel) ?: return null
        return if (latest.versionCode > currentVersionCode) latest else null
    }
}
