package com.studybuddy.v2.ui.letter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.model.Letter
import com.studybuddy.v2.data.repo.LetterRepo
import com.studybuddy.v2.data.repo.UserRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LetterTab { LETTER, PLANE }

data class LetterUiState(
    val tab: LetterTab = LetterTab.LETTER,    // 信件唯一载体（飞机已隐藏）
    val letters: List<Letter> = emptyList(),
    val draft: String = "",
    val sending: Boolean = false,
    val justSentAnimation: Boolean = false,  // 触发寄出动画
    val myUserId: String = "",
    val partnerName: String = "TA",
    val hasPartner: Boolean = false,
    val loading: Boolean = true
)

@HiltViewModel
class LetterViewModel @Inject constructor(
    private val letterRepo: LetterRepo,
    private val userRepo: UserRepo
) : ViewModel() {

    private val _state = MutableStateFlow(LetterUiState())
    val state: StateFlow<LetterUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val me = userRepo.currentUserId.first().orEmpty()
            val partner = userRepo.getPartner()
            val letters = if (partner != null) letterRepo.list() else emptyList()
            _state.update {
                it.copy(
                    myUserId = me,
                    partnerName = partner?.nickname?.takeIf { it.isNotBlank() } ?: "TA",
                    hasPartner = partner != null,
                    letters = letters,
                    loading = false
                )
            }
        }
    }

    fun setTab(tab: LetterTab) {
        _state.update { it.copy(tab = tab, draft = "") }
    }

    fun updateDraft(text: String) {
        val tab = _state.value.tab
        // PLANE 强制 30 字硬截断
        val clipped = if (tab == LetterTab.PLANE) text.take(LetterRepo.PLANE_MAX_CHARS) else text
        _state.update { it.copy(draft = clipped) }
    }

    /** 寄出。成功后清空 draft + 触发动画 + 刷新列表。 */
    fun send() {
        val draft = _state.value.draft.trim()
        if (draft.isEmpty() || _state.value.sending) return
        viewModelScope.launch {
            _state.update { it.copy(sending = true, justSentAnimation = true) }
            val saved = when (_state.value.tab) {
                LetterTab.LETTER -> letterRepo.sendLetter(draft)
                LetterTab.PLANE -> letterRepo.sendPlane(draft)
            }
            if (saved != null) {
                val newList = listOf(saved) + _state.value.letters
                _state.update { it.copy(letters = newList, draft = "", sending = false) }
            } else {
                _state.update { it.copy(sending = false, justSentAnimation = false) }
            }
        }
    }

    /** 寄出动画播完后清除标记 */
    fun clearAnimation() {
        _state.update { it.copy(justSentAnimation = false) }
    }
}
