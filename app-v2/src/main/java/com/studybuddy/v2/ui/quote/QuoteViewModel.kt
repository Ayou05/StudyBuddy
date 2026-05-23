package com.studybuddy.v2.ui.quote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.model.Quote
import com.studybuddy.v2.data.model.QuoteVisibility
import com.studybuddy.v2.data.repo.QuoteRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuoteUiState(
    val quotes: List<Quote> = emptyList(),
    val loading: Boolean = false,
    val composing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class QuoteViewModel @Inject constructor(
    private val repo: QuoteRepo
) : ViewModel() {
    private val _state = MutableStateFlow(QuoteUiState())
    val state: StateFlow<QuoteUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val list = repo.list()
            _state.update { it.copy(quotes = list, loading = false) }
        }
    }

    fun openCompose() = _state.update { it.copy(composing = true) }
    fun closeCompose() = _state.update { it.copy(composing = false) }

    fun submit(text: String, source: String, visibility: QuoteVisibility) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val saved = repo.add(text.trim(), source.trim(), visibility)
            if (saved == null) {
                _state.update { it.copy(errorMessage = "保存失败，请检查网络或稍后再试") }
            } else {
                _state.update { it.copy(composing = false, errorMessage = null) }
            }
            load()
        }
    }

    fun dismissError() = _state.update { it.copy(errorMessage = null) }

    fun delete(id: String) {
        viewModelScope.launch {
            val ok = repo.delete(id)
            if (!ok) {
                _state.update { it.copy(errorMessage = "删除失败，可能没权限或网络问题") }
            }
            load()
        }
    }
}
