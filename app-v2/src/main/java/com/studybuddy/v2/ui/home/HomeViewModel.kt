package com.studybuddy.v2.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.model.FocusSession
import com.studybuddy.v2.data.model.Pet
import com.studybuddy.v2.data.model.RealtimeStatus
import com.studybuddy.v2.data.model.Relationship
import com.studybuddy.v2.data.model.UserProfile
import com.studybuddy.v2.data.model.SyncInvite
import com.studybuddy.v2.data.pb.PbRealtime
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.repo.PetRepo
import com.studybuddy.v2.data.repo.PreferencesStore
import com.studybuddy.v2.data.repo.SessionRepo
import com.studybuddy.v2.data.repo.StatusRepo
import com.studybuddy.v2.data.repo.SyncInviteRepo
import com.studybuddy.v2.data.repo.UserRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val nickname: String = "",
    val me: UserProfile? = null,
    val partner: UserProfile? = null,
    val partnerStatus: RealtimeStatus? = null,
    val relationship: Relationship? = null,
    val pet: Pet? = null,
    val activeSession: FocusSession? = null,
    val todayMinutes: Int = 0,
    val partnerTodayMinutes: Int = 0,
    val partnerWidgetStyle: String = "WAVE",   // "WAVE" / "TYPOGRAPHY"
    val focusMode: String = "COUNTDOWN",       // "COUNTDOWN" / "STOPWATCH"
    val focusDurationMin: Int = 25,
    val incomingInvite: SyncInvite? = null,
    val poseCaption: String = "",    // Home 顶部小灰字（"工作日午后 · 你们都在常驻地"）
    val currentMoment: com.studybuddy.v2.data.moment.Moment? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepo: UserRepo,
    private val sessionRepo: SessionRepo,
    private val statusRepo: StatusRepo,
    private val petRepo: PetRepo,
    private val prefs: PreferencesStore,
    private val realtime: PbRealtime,
    private val inviteRepo: SyncInviteRepo,
    private val poseRepo: com.studybuddy.v2.data.repo.PoseRepo,
    private val momentBus: com.studybuddy.v2.data.moment.MomentBus,
    private val momentHub: com.studybuddy.v2.data.moment.MomentTriggerHub
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
    private var inviteJob: Job? = null

    init {
        refresh()
        startInviteWatcher()
        startMomentWatcher()
    }

    private fun startMomentWatcher() {
        viewModelScope.launch {
            momentBus.events.collect { moment ->
                _state.update { it.copy(currentMoment = moment) }
            }
        }
    }

    fun dismissMoment() {
        val current = _state.value.currentMoment
        _state.update { it.copy(currentMoment = null) }
        if (current != null) {
            viewModelScope.launch { momentBus.markDismissed(current) }
        }
    }

    private fun startInviteWatcher() {
        inviteJob?.cancel()
        inviteJob = viewModelScope.launch {
            // 启动时拉一次未处理的（补漏）
            val me = userRepo.currentUserId.first()
            if (!me.isNullOrBlank()) {
                inviteRepo.fetchPendingForMe()?.let { invite ->
                    _state.update { it.copy(incomingInvite = invite) }
                }
                realtime.events.collect { ev ->
                    if (!ev.topic.startsWith(PbConfig.SYNC_INVITES)) return@collect
                    val record = ev.record
                    val toId = record["toUserId"]?.jsonPrimitive?.contentOrNull
                    val status = record["status"]?.jsonPrimitive?.contentOrNull
                    if (toId != me) return@collect
                    if (status != "PENDING") return@collect
                    try {
                        val invite = Json { ignoreUnknownKeys = true }
                            .decodeFromJsonElement(SyncInvite.serializer(), record)
                        _state.update { it.copy(incomingInvite = invite) }
                    } catch (_: Exception) { }
                }
            }
        }
    }

    fun acceptInvite() {
        val inv = _state.value.incomingInvite ?: return
        viewModelScope.launch {
            inviteRepo.accept(inv.id, sessionId = null)
            _state.update { it.copy(incomingInvite = null) }
        }
    }

    fun declineInvite() {
        val inv = _state.value.incomingInvite ?: return
        viewModelScope.launch {
            inviteRepo.decline(inv.id)
            _state.update { it.copy(incomingInvite = null) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val userId = userRepo.currentUserId.first() ?: return@launch
            val widgetStyle = prefs.partnerWidgetStyle.first()
            val focusMode = prefs.focusMode.first()
            val focusDurationMin = prefs.focusDurationMin.first()
            _state.update {
                it.copy(
                    partnerWidgetStyle = widgetStyle,
                    focusMode = focusMode,
                    focusDurationMin = focusDurationMin
                )
            }

            val me = userRepo.getMe()
            _state.update { it.copy(me = me, nickname = me?.nickname.orEmpty()) }

            val partner = userRepo.getPartner()
            val relationship = userRepo.getRelationship()
            val active = sessionRepo.getActiveSession(userId)
            val todayMin = sessionRepo.getTodayMinutes(userId)
            _state.update {
                it.copy(
                    partner = partner,
                    relationship = relationship,
                    activeSession = active,
                    todayMinutes = todayMin
                )
            }

            val partnerStatus = partner?.id?.let { statusRepo.getPartnerStatus(it) }
            _state.update {
                it.copy(
                    partnerStatus = partnerStatus,
                    partnerTodayMinutes = ((partnerStatus?.todayFocusSeconds ?: 0) / 60).toInt()
                )
            }

            // 姿态系统 —— 算一次给 Home 顶部视觉锚（耗时不阻塞主流，单次定位 + 缓存）
            launch {
                runCatching { poseRepo.refresh() }
                val pose = poseRepo.current()
                _state.update { it.copy(poseCaption = pose?.displayCaption().orEmpty()) }
            }

            // 情境层 tick —— 上报位置 + 跑见面/停留 detector
            launch {
                runCatching { momentHub.tick() }
            }

            val pet = relationship?.id?.let { petRepo.getOrFetchByPair(it) }
            _state.update { it.copy(pet = pet, loading = false) }
        }
    }

    /** 更新专注偏好（来自 FocusSetupSheet）。 */
    fun saveFocusPreference(mode: String, durationMin: Int) {
        viewModelScope.launch {
            prefs.setFocusMode(mode)
            prefs.setFocusDurationMin(durationMin)
            _state.update { it.copy(focusMode = mode, focusDurationMin = durationMin) }
        }
    }
}
