package com.studybuddy.v2.data.model

import kotlinx.serialization.Serializable

/**
 * App 版本信息（PB collection: app_versions）。
 *
 * 后台手动发布新版本时插入一条记录，客户端定期拉取最新一条比对 versionCode。
 *
 * @property versionCode 整数版本号，单调递增
 * @property versionName 显示用版本名 "v2.7.0-beta"
 * @property minSupportedCode 最低支持版本，低于此则强制升级
 * @property downloadUrl APK 下载地址（可以是 PB file 链接、CDN、GitHub release）
 * @property apkSize 字节数（仅展示）
 * @property sha256 校验和（可选）
 * @property releaseNotes 更新说明
 * @property channel 渠道（"stable" / "beta" / "internal"）
 * @property force 是否强制升级（不可关闭弹窗）
 * @property publishedAt 发布时间戳
 */
@Serializable
data class AppVersion(
    val id: String = "",
    val versionCode: Int = 0,
    val versionName: String = "",
    val minSupportedCode: Int = 0,
    val downloadUrl: String = "",
    val apkSize: Long = 0,
    val sha256: String = "",
    val releaseNotes: String = "",
    val channel: String = "stable",
    val force: Boolean = false,
    val publishedAt: Long = 0
)
