package com.studybuddy.v2.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.model.FundTransaction
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

data class VaultUiState(
    val fund: SharedFund? = null,
    val transactions: List<FundTransaction> = emptyList(),
    val partnerName: String = "TA",
    val hasPartner: Boolean = false,
    val loading: Boolean = true,
    val showAddDialog: Boolean = false
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val fundRepo: FundRepo,
    private val userRepo: UserRepo
) : ViewModel() {

    private val _state = MutableStateFlow(VaultUiState())
    val state: StateFlow<VaultUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val partner = userRepo.getPartner()
            val rel = userRepo.getRelationship()
            if (rel == null) {
                _state.update {
                    it.copy(hasPartner = false, partnerName = "TA", loading = false)
                }
                return@launch
            }
            val fund = fundRepo.getByPair(rel.id)
            val txs = fundRepo.listTransactions()
            _state.update {
                it.copy(
                    fund = fund,
                    transactions = txs,
                    hasPartner = true,
                    partnerName = partner?.nickname?.takeIf { n -> n.isNotBlank() } ?: "TA",
                    loading = false
                )
            }
        }
    }

    fun openAdd() = _state.update { it.copy(showAddDialog = true) }
    fun closeAdd() = _state.update { it.copy(showAddDialog = false) }

    fun deposit(amountCents: Long, note: String) {
        viewModelScope.launch {
            fundRepo.deposit(amountCents, note)
            _state.update { it.copy(showAddDialog = false) }
            refresh()
        }
    }

    fun voidTransaction(tx: FundTransaction) {
        viewModelScope.launch {
            fundRepo.voidTransaction(tx.id)
            refresh()
        }
    }

    fun deleteTransaction(tx: FundTransaction) {
        viewModelScope.launch {
            fundRepo.deleteTransaction(tx)
            refresh()
        }
    }
}
