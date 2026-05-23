package com.studybuddy.v2.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.pb.ConnectionState
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbRealtime
import com.studybuddy.v2.data.repo.PreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkTheme: Boolean = false,
    val focusMode: String = "COUNTDOWN",
    val focusDurationMin: Int = 25,
    val partnerWidgetStyle: String = "WAVE",
    val unlockedSaddleCat: Boolean = false,
    val showEasterEggInput: Boolean = false,
    val easterEggError: String? = null,
    val realtimeState: ConnectionState = ConnectionState.IDLE,
    val realtimeEventCount: Int = 0,
    val ledgerUnitCents: Int = 500,
    val momentDisabled: Set<String> = emptySet(),
    val appMode: String = "FOCUS"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesStore,
    private val realtime: PbRealtime,
    private val pb: PbClient
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                SettingsUiState(
                    darkTheme = prefs.darkTheme.first(),
                    focusMode = prefs.focusMode.first(),
                    focusDurationMin = prefs.focusDurationMin.first(),
                    partnerWidgetStyle = prefs.partnerWidgetStyle.first(),
                    unlockedSaddleCat = prefs.unlockedSaddleCat.first(),
                    ledgerUnitCents = prefs.ledgerUnitCents.first(),
                    momentDisabled = parseMomentDisabled(prefs.momentDisabledTypes.first()),
                    appMode = prefs.appMode.first()
                )
            }
        }
        viewModelScope.launch {
            realtime.state.collect { s -> _state.update { it.copy(realtimeState = s) } }
        }
        viewModelScope.launch {
            realtime.events.collect { _state.update { it.copy(realtimeEventCount = it.realtimeEventCount + 1) } }
        }
    }

    fun setDark(v: Boolean) { _state.update { it.copy(darkTheme = v) }; viewModelScope.launch { prefs.setDarkTheme(v) } }
    fun setFocusMode(m: String) { _state.update { it.copy(focusMode = m) }; viewModelScope.launch { prefs.setFocusMode(m) } }
    fun setFocusDuration(d: Int) { _state.update { it.copy(focusDurationMin = d) }; viewModelScope.launch { prefs.setFocusDurationMin(d) } }
    fun setPartnerWidget(s: String) { _state.update { it.copy(partnerWidgetStyle = s) }; viewModelScope.launch { prefs.setPartnerWidgetStyle(s) } }
    fun setLedgerUnitCents(cents: Int) { _state.update { it.copy(ledgerUnitCents = cents) }; viewModelScope.launch { prefs.setLedgerUnitCents(cents) } }

    /** 切换某个 Moment type 的开关。enabled=true 表示用户允许收到这种 banner。 */
    fun toggleMomentType(type: String, enabled: Boolean) {
        val cur = _state.value.momentDisabled.toMutableSet()
        if (enabled) cur.remove(type) else cur.add(type)
        _state.update { it.copy(momentDisabled = cur) }
        viewModelScope.launch { prefs.setMomentDisabledTypes(cur.joinToString(",")) }
    }

    private fun parseMomentDisabled(csv: String): Set<String> =
        csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    fun setAppMode(mode: String) {
        _state.update { it.copy(appMode = mode) }
        viewModelScope.launch { prefs.setAppMode(mode) }
    }

    private var aboutTapCount = 0
    fun tapAbout() {
        aboutTapCount += 1
        if (aboutTapCount >= 5 && !_state.value.unlockedSaddleCat) {
            _state.update { it.copy(showEasterEggInput = true) }
        }
    }

    fun dismissEasterEggInput() {
        aboutTapCount = 0
        _state.update { it.copy(showEasterEggInput = false, easterEggError = null) }
    }

    fun submitEasterEggCode(code: String) {
        if (code.trim().equals("cc-pet-ahoy", ignoreCase = true)) {
            viewModelScope.launch { prefs.setUnlockedSaddleCat(true) }
            _state.update { it.copy(unlockedSaddleCat = true, showEasterEggInput = false, easterEggError = null) }
            aboutTapCount = 0
        } else {
            _state.update { it.copy(easterEggError = "口令不对，再想想？") }
        }
    }

    /**
     * 调试用：往自己的 users 记录写一笔 lastSeen，触发 PB 推 update 事件回来。
     * 如果 SSE 通了，realtimeEventCount 会 +1。
     */
    fun pingSelf() {
        viewModelScope.launch {
            val uid = prefs.currentUserId.first() ?: return@launch
            runCatching {
                pb.updateRecord<kotlinx.serialization.json.JsonObject>(
                    collection = PbConfig.USERS,
                    id = uid,
                    fields = mapOf("lastSeen" to System.currentTimeMillis())
                )
            }
        }
    }
}
