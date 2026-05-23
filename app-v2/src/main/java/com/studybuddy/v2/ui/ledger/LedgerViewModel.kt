package com.studybuddy.v2.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.model.Debt
import com.studybuddy.v2.data.repo.DebtRepo
import com.studybuddy.v2.data.repo.PreferencesStore
import com.studybuddy.v2.data.repo.UserRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LedgerUiState(
    val debts: List<Debt> = emptyList(),
    val myUserId: String = "",
    val partnerNickname: String = "",
    val partnerUserId: String = "",
    val unitCents: Int = 500,
    val composing: Boolean = false,
    val loading: Boolean = false
) {
    /** 我欠对方的总份数 */
    val owedByMe: Int get() = debts.filter { !it.settled && it.fromUserId == myUserId }.sumOf { it.count }
    /** 对方欠我的总份数 */
    val owedByPartner: Int get() = debts.filter { !it.settled && it.toUserId == myUserId }.sumOf { it.count }
}

@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val repo: DebtRepo,
    private val userRepo: UserRepo,
    private val prefs: PreferencesStore
) : ViewModel() {
    private val _state = MutableStateFlow(LedgerUiState())
    val state: StateFlow<LedgerUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val me = prefs.currentUserId.first() ?: return@launch
            val partner = userRepo.getPartner()
            val list = repo.list()
            val unit = prefs.ledgerUnitCents.first()
            _state.update {
                it.copy(
                    debts = list,
                    myUserId = me,
                    partnerNickname = partner?.nickname ?: "TA",
                    partnerUserId = partner?.id ?: "",
                    unitCents = unit,
                    loading = false
                )
            }
        }
    }

    fun openCompose() = _state.update { it.copy(composing = true) }
    fun closeCompose() = _state.update { it.copy(composing = false) }

    /** 写一笔。direction = "I_OWE" 我欠 / "PARTNER_OWES" TA 欠 */
    fun submit(direction: String, count: Int, reason: String) {
        if (count <= 0) return
        val s = _state.value
        if (s.partnerUserId.isBlank()) return
        viewModelScope.launch {
            val (from, to) = if (direction == "I_OWE")
                s.myUserId to s.partnerUserId
            else
                s.partnerUserId to s.myUserId
            repo.record(from, to, s.unitCents, count, reason)
            _state.update { it.copy(composing = false) }
            load()
        }
    }

    fun settle(debt: Debt) {
        viewModelScope.launch {
            repo.settle(debt.id)
            load()
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            repo.delete(debt.id)
            load()
        }
    }

    fun voidDebt(debt: Debt) {
        viewModelScope.launch {
            repo.markVoid(debt.id)
            load()
        }
    }
}
