package com.studybuddy.v2.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.model.FocusSession
import com.studybuddy.v2.data.repo.SessionRepo
import com.studybuddy.v2.data.repo.UserRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsDayUiState(
    val ymd: String = "",
    val sessions: List<FocusSession> = emptyList(),
    val loading: Boolean = true
)

@HiltViewModel
class StatsDayDetailViewModel @Inject constructor(
    private val sessionRepo: SessionRepo,
    private val userRepo: UserRepo
) : ViewModel() {

    private val _state = MutableStateFlow(StatsDayUiState())
    val state: StateFlow<StatsDayUiState> = _state.asStateFlow()

    fun load(ymd: String) {
        viewModelScope.launch {
            val uid = userRepo.currentUserId.first() ?: run {
                _state.update { it.copy(ymd = ymd, sessions = emptyList(), loading = false) }
                return@launch
            }
            val sessions = sessionRepo.sessionsForDay(uid, ymd)
            _state.update { StatsDayUiState(ymd = ymd, sessions = sessions, loading = false) }
        }
    }
}
