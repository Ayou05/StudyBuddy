package com.studybuddy.v2.data.moment

import com.studybuddy.v2.data.model.RealtimeStatus
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbRealtime
import com.studybuddy.v2.data.repo.UserRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局监听 partner status SSE，在"非 ACTIVE → ACTIVE"边沿 emit Moment.PartnerStartedFocus。
 *
 * 设计原因：FocusViewModel 里的订阅只在 FocusScreen 前台时活；要让 Home 上也能收到
 * "TA 开始了"的 banner，必须有一个 app 生命周期内常驻的订阅。
 *
 * 单例，由 V2App 启动后调用 [start]。token 失效 / 重新登录由 PbRealtime 自己处理，
 * 我们只需要 collect SSE events。
 */
@Singleton
class PartnerFocusWatcher @Inject constructor(
    private val realtime: PbRealtime,
    private val userRepo: UserRepo,
    private val momentBus: MomentBus
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var job: Job? = null
    @Volatile private var lastFocusStatus: String = ""  // partner 上次 focusStatus

    fun start(scope: CoroutineScope) {
        job?.cancel()
        job = scope.launch {
            val partner = userRepo.getPartner() ?: return@launch
            realtime.events.collect { ev ->
                if (ev.topic != PbConfig.STATUS) return@collect
                val record = ev.record
                val pid = record["userId"]?.jsonPrimitive?.contentOrNull
                if (pid != partner.id) return@collect
                val status = runCatching {
                    json.decodeFromJsonElement(RealtimeStatus.serializer(), record)
                }.getOrNull() ?: return@collect
                handleEdge(status)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        lastFocusStatus = ""
    }

    private suspend fun handleEdge(status: RealtimeStatus) {
        val prev = lastFocusStatus
        val cur = status.focusStatus
        lastFocusStatus = cur

        // 非 ACTIVE → ACTIVE 是上升沿
        if (prev != "ACTIVE" && cur == "ACTIVE") {
            val partner = userRepo.getPartner() ?: return
            // plannedMin 暂时给 25 兜底（status 表里没有，未来可扩字段）
            momentBus.emit(Moment.PartnerStartedFocus(
                id = "pstart_${status.userId}_${status.lastHeartbeat}",
                partnerName = partner.nickname.ifBlank { "TA" },
                plannedMin = 25
            ))
        }
    }
}
