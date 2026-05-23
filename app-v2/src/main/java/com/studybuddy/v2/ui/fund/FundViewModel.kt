package com.studybuddy.v2.ui.fund

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.model.SharedFund
import com.studybuddy.v2.data.repo.FundRepo
import com.studybuddy.v2.data.repo.UserRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FundUiState(
    val loading: Boolean = true,
    val hasPartner: Boolean = false,
    val fund: SharedFund? = null
)

@HiltViewModel
class FundViewModel @Inject constructor(
    private val userRepo: UserRepo,
    private val fundRepo: FundRepo
) : ViewModel() {

    private val _state = MutableStateFlow(FundUiState())
    val state: StateFlow<FundUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val rel = userRepo.getRelationship()
            if (rel == null) {
                _state.update { it.copy(loading = false, hasPartner = false, fund = null) }
                return@launch
            }
            val fund = fundRepo.getByPair(rel.id)
            _state.update { it.copy(loading = false, hasPartner = true, fund = fund) }
        }
    }
}
