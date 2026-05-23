package com.studybuddy.v2.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.model.FocusSession
import com.studybuddy.v2.data.model.FocusStatus
import com.studybuddy.v2.data.model.RealtimeStatus
import com.studybuddy.v2.data.pb.PbRealtime
import com.studybuddy.v2.data.repo.PetRepo
import com.studybuddy.v2.data.repo.PreferencesStore
import com.studybuddy.v2.data.repo.SessionRepo
import com.studybuddy.v2.data.repo.StatusRepo
import com.studybuddy.v2.data.repo.UserRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject

/**
 * 专注模式：
 *   COUNTDOWN —— 倒计时（默认 25 min），到时间自动 complete
 *   STOPWATCH —— 正计时，圆环消失，靠 milestone 文字递进；用户主动 complete
 *
 * 专注类型：
 *   SINGLE  —— 自己一个人专注（默认）
 *   SYNC    —— 双人同步专注，跑 [SyncFocusStateMachine]，订阅对端 status
 */
data class FocusUiState(
    val mode: String = "COUNTDOWN",            // "COUNTDOWN" / "STOPWATCH"
    val focusType: String = "SINGLE",          // "SINGLE" / "SYNC"
    val syncState: SyncFocusStateMachine.State = SyncFocusStateMachine.State.IDLE,
    val partner: PartnerPaneState = PartnerPaneState.Empty,
    val countdown321: Int = 0,                 // 3-2-1 当前数字（COUNTDOWN 态非 0）
    val session: FocusSession? = null,
    val status: FocusStatus = FocusStatus.ACTIVE,
    val elapsedSec: Long = 0,
    val plannedSec: Long = 25 * 60,
    val goal: String = "",
    val partnerNickname: String? = null,
    val landmarkName: String? = null,
    val multiplier: Float = 1f,
    val celebrating: Boolean = false,
    val errorMessage: String? = null,
    // P5 新增：多主题铭牌
    val topicName: String? = null,
    val topicColorHex: String? = null,
    // P5 新增：TA 信息条用
    val partnerElapsedSec: Long = 0,           // TA 当前正在专注多久（秒）
    val partnerFocusStatus: String = "IDLE",   // IDLE / ACTIVE / PAUSED
    val partnerInLibrary: Boolean = false
)

sealed class FocusEvent {
    object Dismiss : FocusEvent()
}

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val sessionRepo: SessionRepo,
    private val statusRepo: StatusRepo,
    private val petRepo: PetRepo,
    private val userRepo: UserRepo,
    private val prefs: PreferencesStore,
    private val realtime: PbRealtime,
    private val inviteRepo: com.studybuddy.v2.data.repo.SyncInviteRepo,
    private val locationRepo: com.studybuddy.v2.data.repo.LocationRepo,
    private val topicRepo: com.studybuddy.v2.data.repo.FocusTopicRepo
) : ViewModel() {

    private val _state = MutableStateFlow(FocusUiState())
    val state: StateFlow<FocusUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<FocusEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<FocusEvent> = _events.asSharedFlow()

    private var tickJob: Job? = null
    private var heartbeatJob: Job? = null
    private var partnerJob: Job? = null
    private var inviteJob: Job? = null
    private var currentInviteId: String? = null
    private var initialized = false

    /** UI 在导航时调用，决定 SINGLE 还是 SYNC。SYNC 入场后立即跑邀请发起逻辑（A3 接入）。 */
    fun setFocusType(type: String) {
        _state.update { it.copy(focusType = type) }
    }

    fun init() {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            val userId = userRepo.currentUserId.first() ?: return@launch
            val partner = userRepo.getPartner()
            val mode = prefs.focusMode.first()
            val durationMin = prefs.focusDurationMin.first()
            val plannedMs = (durationMin * 60_000L).coerceAtLeast(60_000L)

            val existing = sessionRepo.getActiveSession(userId)
            val topicId = prefs.lastSelectedTopicId.first()
            val topic = topicId?.let { topicRepo.get(it) }
            val session = existing ?: sessionRepo.startSession(
                userId = userId,
                plannedDurationMs = if (mode == "STOPWATCH") Long.MAX_VALUE / 2 else plannedMs,
                topicId = topicId
            )
            _state.update {
                it.copy(
                    mode = mode,
                    session = session,
                    status = FocusStatus.valueOf(session.status),
                    plannedSec = if (mode == "STOPWATCH") Long.MAX_VALUE / 2 else plannedMs / 1000,
                    goal = session.goal,
                    partnerNickname = partner?.nickname,
                    partner = it.partner.copy(nickname = partner?.nickname ?: ""),
                    topicName = topic?.name,
                    topicColorHex = topic?.colorHex
                )
            }
            if (FocusStatus.valueOf(session.status) == FocusStatus.ACTIVE) {
                startTicking()
                startHeartbeat()
            }
            // 查最近地标 —— 命中给一份温柔的"加持"
            viewModelScope.launch {
                val nearest = locationRepo.nearestLandmark()
                if (nearest != null) {
                    _state.update {
                        it.copy(
                            landmarkName = nearest.name,
                            multiplier = nearest.multiplier
                        )
                    }
                    statusRepo.setFocusStatus("ACTIVE", _state.value.elapsedSec, session.id)
                }
            }
            // SYNC 模式：订阅对端 status，并真正发出邀请
            if (_state.value.focusType == "SYNC" && partner != null) {
                startPartnerSubscription(partner.id)
                startInviteSubscription(userId)
                applySyncEvent(SyncFocusStateMachine.Event.MyInvite)
                // 真正发邀请
                val invite = inviteRepo.sendInvite(
                    toUserId = partner.id,
                    plannedDurationMs = plannedMs,
                    mode = mode,
                    goal = session.goal
                )
                currentInviteId = invite?.id
                // 60s 后未应答 → InviteTimeout
                viewModelScope.launch {
                    delay(60_000)
                    if (_state.value.syncState == SyncFocusStateMachine.State.INVITING) {
                        applySyncEvent(SyncFocusStateMachine.Event.InviteTimeout)
                        currentInviteId?.let { inviteRepo.cancel(it) }
                    }
                }
            }
        }
    }

    /** 订阅 sync_invites 推送，主要监听自己发出的邀请被 ACCEPTED/DECLINED */
    private fun startInviteSubscription(myUserId: String) {
        inviteJob?.cancel()
        inviteJob = viewModelScope.launch {
            realtime.events.collect { ev ->
                if (!ev.topic.startsWith(com.studybuddy.v2.data.pb.PbConfig.SYNC_INVITES)) return@collect
                val record = ev.record
                val id = record["id"]?.jsonPrimitive?.contentOrNull
                val fromId = record["fromUserId"]?.jsonPrimitive?.contentOrNull
                val status = record["status"]?.jsonPrimitive?.contentOrNull
                if (id == null || status == null) return@collect
                if (id != currentInviteId) return@collect
                if (fromId != myUserId) return@collect
                when (status) {
                    "ACCEPTED" -> applySyncEvent(SyncFocusStateMachine.Event.PartnerAccepted)
                    "DECLINED" -> applySyncEvent(SyncFocusStateMachine.Event.PartnerDeclined)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SYNC 状态机驱动
    // ─────────────────────────────────────────────────────────────────────

    private fun applySyncEvent(event: SyncFocusStateMachine.Event) {
        val before = _state.value.syncState
        val after = SyncFocusStateMachine.reduce(before, event)
        if (before == after) return
        _state.update { it.copy(syncState = after) }
        // 副作用按状态切换执行
        when (after) {
            SyncFocusStateMachine.State.CONFIRMING -> {
                viewModelScope.launch {
                    delay(3_000)
                    applySyncEvent(SyncFocusStateMachine.Event.ConfirmTick)
                }
            }
            SyncFocusStateMachine.State.COUNTDOWN -> startCountdown321()
            SyncFocusStateMachine.State.ABORTED -> {
                viewModelScope.launch {
                    abort()
                }
            }
            else -> {}
        }
    }

    private fun startCountdown321() {
        viewModelScope.launch {
            for (n in 3 downTo 1) {
                _state.update { it.copy(countdown321 = n) }
                delay(1_000)
            }
            _state.update { it.copy(countdown321 = 0) }
            applySyncEvent(SyncFocusStateMachine.Event.CountdownEnd)
        }
    }

    /** 订阅 PbRealtime 中 partner 的 status 记录，派生 [PartnerPaneState]。 */
    private fun startPartnerSubscription(partnerId: String) {
        partnerJob?.cancel()
        partnerJob = viewModelScope.launch {
            // 拉一次初始值
            val initial = statusRepo.getPartnerStatus(partnerId)
            initial?.let { applyPartnerStatus(it) }
            // 然后订阅 SSE 推送
            realtime.events.collect { ev ->
                if (ev.topic != com.studybuddy.v2.data.pb.PbConfig.STATUS) return@collect
                val record = ev.record
                val pid = record["userId"]?.jsonPrimitive?.contentOrNull
                if (pid != partnerId) return@collect
                try {
                    val status = Json { ignoreUnknownKeys = true }
                        .decodeFromJsonElement(RealtimeStatus.serializer(), record)
                    applyPartnerStatus(status)
                } catch (_: Exception) { /* schema 漂移就忽略 */ }
            }
        }
    }

    private fun applyPartnerStatus(status: RealtimeStatus) {
        val now = System.currentTimeMillis()
        val ageMs = (now - status.lastHeartbeat).coerceAtLeast(0)
        val newPartner = _state.value.partner.copy(
            online = status.online,
            focusStatus = status.focusStatus,
            elapsedSec = status.currentFocusSeconds,
            isInLibrary = status.isInLibrary,
            libraryName = status.libraryName,
            lastSeenAgoMs = ageMs
        )
        _state.update {
            it.copy(
                partner = newPartner,
                partnerElapsedSec = status.currentFocusSeconds,
                partnerFocusStatus = status.focusStatus,
                partnerInLibrary = status.isInLibrary
            )
        }
        // 把 partner 状态变化映射为状态机事件
        val event = when {
            !newPartner.isPresent && _state.value.syncState in listOf(
                SyncFocusStateMachine.State.CONFIRMING,
                SyncFocusStateMachine.State.COUNTDOWN,
                SyncFocusStateMachine.State.ACTIVE,
                SyncFocusStateMachine.State.PAUSED
            ) -> SyncFocusStateMachine.Event.PartnerOffline
            status.focusStatus == "PAUSED" && _state.value.syncState == SyncFocusStateMachine.State.ACTIVE
                -> SyncFocusStateMachine.Event.PartnerPaused
            status.focusStatus == "ACTIVE" && _state.value.syncState == SyncFocusStateMachine.State.PAUSED
                -> SyncFocusStateMachine.Event.PartnerResumed
            else -> null
        }
        event?.let { applySyncEvent(it) }
    }

    /** 暂停 —— 仅暂停，不弹 sheet（sheet 由 UI 层管显隐）。 */
    fun pause() {
        val s = _state.value
        val session = s.session ?: return
        if (s.status != FocusStatus.ACTIVE) return
        viewModelScope.launch {
            tickJob?.cancel()
            heartbeatJob?.cancel()
            val updated = sessionRepo.pauseSession(session)
            _state.update { it.copy(session = updated, status = FocusStatus.PAUSED) }
            statusRepo.setFocusStatus("PAUSED", s.elapsedSec, session.id)
        }
    }

    fun resume() {
        val s = _state.value
        val session = s.session ?: return
        if (s.status != FocusStatus.PAUSED) return
        viewModelScope.launch {
            val updated = sessionRepo.resumeSession(session)
            _state.update { it.copy(session = updated, status = FocusStatus.ACTIVE) }
            startTicking()
            startHeartbeat()
        }
    }

    /** 完成并保存（写入今日时长 + 给宠物加心情）。 */
    fun complete() {
        val s = _state.value
        val session = s.session ?: return
        if (s.status == FocusStatus.COMPLETED) return
        viewModelScope.launch {
            tickJob?.cancel()
            heartbeatJob?.cancel()
            val updated = sessionRepo.completeSession(session)
            _state.update { it.copy(session = updated, status = FocusStatus.COMPLETED, celebrating = true) }
            statusRepo.setFocusStatus("IDLE", 0, null)
            val rel = userRepo.getRelationship()
            if (rel != null) {
                val actualMin = ((updated.actualDurationMs ?: 0L) / 60_000L).toInt()
                if (actualMin > 0) {
                    val boosted = (actualMin * _state.value.multiplier).toInt().coerceAtLeast(actualMin)
                    petRepo.rewardForFocusCompletion(rel.id, boosted)
                }
            }
            delay(1500)
            _events.tryEmit(FocusEvent.Dismiss)
        }
    }

    /** 放弃（不计入今日，不奖励宠物）。 */
    fun abort() {
        val session = _state.value.session ?: return
        viewModelScope.launch {
            tickJob?.cancel()
            heartbeatJob?.cancel()
            sessionRepo.abortSession(session)
            statusRepo.setFocusStatus("IDLE", 0, null)
            _events.tryEmit(FocusEvent.Dismiss)
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                val sess = _state.value.session ?: break
                val now = System.currentTimeMillis()
                val elapsed = ((now - sess.startedAt - sess.totalPausedMs) / 1000).coerceAtLeast(0)
                _state.update { it.copy(elapsedSec = elapsed) }
                // 倒计时模式才会自动完成；正计时由用户主动结束
                if (_state.value.mode == "COUNTDOWN" && elapsed >= sess.plannedDurationMs / 1000) {
                    complete()
                    break
                }
                delay(500)
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (isActive) {
                val s = _state.value
                if (s.status != FocusStatus.ACTIVE) break
                statusRepo.setFocusStatus("ACTIVE", s.elapsedSec, s.session?.id)
                delay(5000)
            }
        }
    }

    override fun onCleared() {
        tickJob?.cancel()
        heartbeatJob?.cancel()
        partnerJob?.cancel()
        inviteJob?.cancel()
        super.onCleared()
    }
}

