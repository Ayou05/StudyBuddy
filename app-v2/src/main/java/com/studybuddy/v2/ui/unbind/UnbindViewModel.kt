package com.studybuddy.v2.ui.unbind

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.model.UnbindRequest
import com.studybuddy.v2.data.repo.UnbindRepo
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
    val active: UnbindRequest? = null,
    val showConfirmDialog: Boolean = false,
    val operating: Boolean = false
)

@HiltViewModel
class UnbindViewModel @Inject constructor(
    private val unbindRepo: UnbindRepo,
    private val userRepo: UserRepo
) : ViewModel() {

    private val _state = MutableStateFlow(UnbindUiState())
    val state: StateFlow<UnbindUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val rel = userRepo.getRelationship()
            if (rel == null) {
                _state.update { it.copy(loading = false, hasPartner = false) }
                return@launch
            }
            val partner = userRepo.getPartner()
            val active = unbindRepo.activeFor(rel.id)
            _state.update {
                it.copy(
                    loading = false,
                    hasPartner = true,
                    partnerNickname = partner?.nickname.orEmpty(),
                    active = active
                )
            }
        }
    }

    fun showConfirm() { _state.update { it.copy(showConfirmDialog = true) } }
    fun dismissConfirm() { _state.update { it.copy(showConfirmDialog = false) } }

    fun submitRequest() {
        if (_state.value.operating) return
        viewModelScope.launch {
            _state.update { it.copy(operating = true, showConfirmDialog = false) }
            val req = unbindRepo.request()
            _state.update { it.copy(operating = false, active = req) }
        }
    }

    fun cancelRequest() {
        val req = _state.value.active ?: return
        if (_state.value.operating) return
        viewModelScope.launch {
            _state.update { it.copy(operating = true) }
            val ok = unbindRepo.cancel(req.id)
            if (ok) {
                _state.update { it.copy(operating = false, active = null) }
            } else {
                _state.update { it.copy(operating = false) }
            }
        }
    }
}
