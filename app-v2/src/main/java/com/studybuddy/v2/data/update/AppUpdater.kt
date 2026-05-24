package com.studybuddy.v2.data.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自更新下载器。
 *
 * 用 Android 系统 DownloadManager 后台下载，
 * 下载完成后用 FileProvider 弹安装界面（用户需在系统授权"未知来源应用"权限）。
 *
 * 下载位置：app-private external files dir / updates / studybuddy-{versionCode}.apk
 * 这样卸载 app 也会清掉，不留垃圾。
 */
@Singleton
class AppUpdater @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    sealed class State {
        object Idle : State()
        data class Downloading(val downloaded: Long, val total: Long) : State() {
            val progress: Float get() = if (total > 0) downloaded.toFloat() / total else 0f
        }
        data class Failed(val reason: String) : State()
        data class Ready(val apkFile: File) : State()
    }

    private val downloadManager: DownloadManager
        get() = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private fun targetFile(versionCode: Int): File {
        val dir = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "studybuddy-$versionCode.apk")
    }

    /**
     * 开始下载。返回进度 Flow。完成后 State.Ready 携带 apk 路径。
     */
    fun download(downloadUrl: String, versionCode: Int, versionName: String): Flow<State> = callbackFlow {
        val target = targetFile(versionCode)
        if (target.exists() && target.length() > 0) {
            // 已经下载过，直接 Ready
            trySend(State.Ready(target))
            close()
            return@callbackFlow
        }

        val req = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("StudyBuddy $versionName")
            setDescription("正在下载更新…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setDestinationInExternalFilesDir(
                ctx,
                Environment.DIRECTORY_DOWNLOADS,
                "updates/studybuddy-$versionCode.apk"
            )
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            setMimeType("application/vnd.android.package-archive")
        }
        val downloadId = downloadManager.enqueue(req)

        // 注册 BroadcastReceiver 监听完成
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                if (id != downloadId) return
                val status = queryStatus(downloadId)
                when (status.first) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        trySend(State.Ready(target))
                        close()
                    }
                    DownloadManager.STATUS_FAILED -> {
                        trySend(State.Failed("下载失败 reason=${status.second}"))
                        close()
                    }
                }
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= 33) {
            ctx.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            ctx.registerReceiver(receiver, filter)
        }

        // 轮询进度（每 500ms）
        val progressJob = scope.launch {
            while (true) {
                val (status, downloaded, total) = queryProgress(downloadId)
                if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING) {
                    trySend(State.Downloading(downloaded, total))
                }
                if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                    break
                }
                delay(500)
            }
        }

        awaitClose {
            progressJob.cancel()
            try { ctx.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }.distinctUntilChanged()

    private fun queryStatus(id: Long): Pair<Int, Int> {
        val q = DownloadManager.Query().setFilterById(id)
        downloadManager.query(q).use { c: Cursor ->
            if (c.moveToFirst()) {
                val statusIdx = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                val reasonIdx = c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                return c.getInt(statusIdx) to c.getInt(reasonIdx)
            }
        }
        return -1 to -1
    }

    private fun queryProgress(id: Long): Triple<Int, Long, Long> {
        val q = DownloadManager.Query().setFilterById(id)
        downloadManager.query(q).use { c: Cursor ->
            if (c.moveToFirst()) {
                val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val downloaded = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                return Triple(status, downloaded, total)
            }
        }
        return Triple(-1, 0L, 0L)
    }

    /**
     * 启动 APK 安装界面。需在用户授权"未知来源"后才能成功安装。
     * 调用方应在按钮点击后调，并捕获可能的 ActivityNotFoundException。
     */
    fun install(apk: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(intent)
    }
}
