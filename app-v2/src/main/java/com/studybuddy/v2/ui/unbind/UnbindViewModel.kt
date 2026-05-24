package com.studybuddy.v2.ui.unbind

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.repo.UserRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UnbindUiState(
    val loading: Boolean = true,
    val hasPartner: Boolean = false,
    val partnerNickname: String = "",
    val showConfirmDialog: Boolean = false,
    val operating: Boolean = false,
    val done: Boolean = false
)

@HiltViewModel
class UnbindViewModel @Inject constructor(
    private val userRepo: UserRepo
) : ViewModel() {

    private val _state = MutableStateFlow(UnbindUiState())
    val state: StateFlow<UnbindUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val partner = userRepo.getPartner()
            _state.update {
                it.copy(
                    loading = false,
                    hasPartner = partner != null,
                    partnerNickname = partner?.nickname ?: ""
                )
            }
        }
    }

    fun showConfirm() { _state.update { it.copy(showConfirmDialog = true) } }
    fun dismissConfirm() { _state.update { it.copy(showConfirmDialog = false) } }

    fun confirmUnbind() {
        if (_state.value.operating) return
        viewModelScope.launch {
            _state.update { it.copy(operating = true, showConfirmDialog = false) }
            val ok = userRepo.unbindRelationship()
            _state.update { it.copy(operating = false, done = ok, hasPartner = !ok) }
        }
    }
}
