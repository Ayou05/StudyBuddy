package com.studybuddy.v2.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.model.Relationship
import com.studybuddy.v2.data.model.UserProfile
import com.studybuddy.v2.data.repo.AuthRepo
import com.studybuddy.v2.data.repo.UserRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class MeUiState(
    val loading: Boolean = true,
    val me: UserProfile? = null,
    val partner: UserProfile? = null,
    val relationship: Relationship? = null,
    val inviteCode: String? = null
)

sealed class MeEvent {
    object LoggedOut : MeEvent()
}

@HiltViewModel
class MeViewModel @Inject constructor(
    private val userRepo: UserRepo,
    private val authRepo: AuthRepo
) : ViewModel() {

    private val _state = MutableStateFlow(MeUiState())
    val state: StateFlow<MeUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<MeEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<MeEvent> = _events.asSharedFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val me = userRepo.getMe()
            val partner = userRepo.getPartner()
            val rel = userRepo.getRelationship()
            _state.update { it.copy(loading = false, me = me, partner = partner, relationship = rel) }
        }
    }

    /** 生成 6 位邀请码（仅本地随机；后端配套接口在 P3 做）。 */
    fun regenerateInviteCode() {
        val chars = ('A'..'Z').filter { it !in "OIL" } + ('2'..'9').toList()
        val code = (1..6).map { chars.random(Random.Default) }.joinToString("")
        _state.update { it.copy(inviteCode = code) }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.logout()
            _events.tryEmit(MeEvent.LoggedOut)
        }
    }
}
