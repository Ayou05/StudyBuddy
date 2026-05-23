package com.studybuddy.v2.data.pb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * PocketBase 实时通道（SSE）。
 *
 * 协议（PocketBase 0.22+）：
 *   1. GET /api/realtime 建立长连接，首条事件 `event: PB_CONNECT\ndata: {"clientId": "..."}`
 *   2. POST /api/realtime 带 clientId + subscriptions 注册订阅
 *   3. 后续 `event: <collection/id 或 collection>\ndata: {action, record}` 推送
 *
 * 设计原则：
 * - 单例，整个 app 一条长连接，订阅集合通过 [setSubscriptions] 动态调整
 * - 断线指数退避重连：1s → 2s → 4s → 8s → 30s 上限
 * - token 变化时自动 reset
 * - 未登录时不连接，登入后由调用方触发 [start]
 */
@Singleton
class PbRealtime @Inject constructor(
    private val pb: PbClient
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // SSE 不能用 readTimeout
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectJob: Job? = null

    @Volatile private var subscriptions: List<String> = emptyList()
    @Volatile private var clientId: String? = null
    @Volatile private var currentSource: EventSource? = null

    private val _state = MutableStateFlow(ConnectionState.IDLE)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PbRealtimeEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val events: SharedFlow<PbRealtimeEvent> = _events.asSharedFlow()

    private val json: Json = pb.json

    /** 设置订阅集合并重连。例 ["status", "sync_invites/u123"] */
    fun setSubscriptions(subs: List<String>) {
        subscriptions = subs.distinct()
        if (state.value != ConnectionState.IDLE) reconnect()
    }

    /** 启动连接。token 必须已经在 PbClient 上 set 好。 */
    fun start() {
        if (connectJob?.isActive == true) return
        connectJob = scope.launch { connectLoop() }
    }

    /** 重连（外部触发，比如订阅变化或 token 变化）。 */
    fun reconnect() {
        currentSource?.cancel()
        // connectLoop 会在 EventSource 关闭后自动重连
    }

    /** 停止连接（登出时）。 */
    fun stop() {
        connectJob?.cancel()
        connectJob = null
        currentSource?.cancel()
        currentSource = null
        clientId = null
        _state.value = ConnectionState.IDLE
    }

    private suspend fun connectLoop() {
        var backoff = 1000L
        while (true) {
            if (pb.currentToken().isNullOrBlank()) {
                _state.value = ConnectionState.IDLE
                return
            }
            try {
                _state.value = ConnectionState.CONNECTING
                runOneConnection()
                // 正常返回 = 服务端关闭，退避重连
            } catch (e: Exception) {
                // 异常断线
            }
            _state.value = ConnectionState.RECONNECTING
            delay(backoff)
            backoff = (backoff * 2).coerceAtMost(30_000L)
        }
    }

    /** 跑一次完整连接，直到断线返回。 */
    private suspend fun runOneConnection() = suspendCoroutine<Unit> { cont ->
        val req = Request.Builder()
            .url(PbConfig.BASE_URL + "api/realtime")
            .header("Accept", "text/event-stream")
            .apply { pb.currentToken()?.let { header("Authorization", it) } }
            .build()

        val factory = EventSources.createFactory(client)
        var resumed = false
        val source = factory.newEventSource(req, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                _state.value = ConnectionState.OPEN
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                // PocketBase: type 通常是 "PB_CONNECT" 或 "<collection>" 或 "<collection>/<id>"
                if (type == "PB_CONNECT") {
                    try {
                        val obj = json.parseToJsonElement(data) as? JsonObject
                        val cid = obj?.get("clientId")?.toString()?.trim('"')
                        if (cid != null) {
                            clientId = cid
                            // 异步发送订阅
                            scope.launch { runCatching { postSubscriptions(cid) } }
                        }
                    } catch (_: SerializationException) {}
                    return
                }
                // 业务事件
                try {
                    val payload = json.parseToJsonElement(data) as? JsonObject ?: return
                    val action = payload["action"]?.toString()?.trim('"') ?: "update"
                    val record = payload["record"] as? JsonObject ?: return
                    _events.tryEmit(
                        PbRealtimeEvent(
                            topic = type ?: "",
                            action = action,
                            record = record
                        )
                    )
                } catch (_: SerializationException) {}
            }

            override fun onClosed(eventSource: EventSource) {
                if (!resumed) { resumed = true; cont.resume(Unit) }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                if (!resumed) { resumed = true; cont.resume(Unit) }
            }
        })
        currentSource = source
    }

    /** 把订阅列表发回 PB，PB 才开始把对应事件推过来。 */
    private suspend fun postSubscriptions(cid: String) = withContext(Dispatchers.IO) {
        val subsArray = buildJsonArray {
            subscriptions.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
        }
        val body = buildJsonObject {
            put("clientId", cid)
            put("subscriptions", subsArray)
        }
        val req = Request.Builder()
            .url(PbConfig.BASE_URL + "api/realtime")
            .post(json.encodeToString(JsonObject.serializer(), body)
                .toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .apply { pb.currentToken()?.let { header("Authorization", it) } }
            .build()
        runCatching { client.newCall(req).execute().use { /* fire-and-forget */ } }
    }
}

enum class ConnectionState { IDLE, CONNECTING, OPEN, RECONNECTING }

data class PbRealtimeEvent(
    /** topic = collection 名 或 "collection/id"；空字符串表示未知 */
    val topic: String,
    /** "create" / "update" / "delete" */
    val action: String,
    /** PB 推送的 record JSON */
    val record: JsonObject
)
