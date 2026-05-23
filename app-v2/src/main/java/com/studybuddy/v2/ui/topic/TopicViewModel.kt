package com.studybuddy.v2.ui.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.model.FocusTopic
import com.studybuddy.v2.data.repo.FocusTopicRepo
import com.studybuddy.v2.data.repo.PreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopicPickerUiState(
    val topics: List<FocusTopic> = emptyList(),
    val currentTopicId: String? = null,
    val showAddDialog: Boolean = false,
    val newTopicName: String = "",
    val newTopicColor: String = "#CC785C",
    val editingTopic: FocusTopic? = null,    // 长按某主题进入编辑态
    val loading: Boolean = true
)

@HiltViewModel
class TopicViewModel @Inject constructor(
    private val topicRepo: FocusTopicRepo,
    private val prefs: PreferencesStore
) : ViewModel() {

    private val _state = MutableStateFlow(TopicPickerUiState())
    val state: StateFlow<TopicPickerUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val topics = topicRepo.list()
            val lastSelected = prefs.lastSelectedTopicId.first()
            _state.update {
                it.copy(
                    topics = topics,
                    currentTopicId = lastSelected,
                    loading = false
                )
            }
        }
    }

    fun selectTopic(topicId: String?) {
        viewModelScope.launch {
            prefs.setLastSelectedTopicId(topicId)
            _state.update { it.copy(currentTopicId = topicId) }
        }
    }

    fun openAddDialog() = _state.update { it.copy(showAddDialog = true, newTopicName = "", newTopicColor = "#CC785C") }
    fun closeAddDialog() = _state.update { it.copy(showAddDialog = false, newTopicName = "") }
    fun updateNewTopicName(name: String) = _state.update { it.copy(newTopicName = name.take(16)) }
    fun updateNewTopicColor(hex: String) = _state.update { it.copy(newTopicColor = hex) }

    fun saveNewTopic() {
        val name = _state.value.newTopicName.trim()
        val color = _state.value.newTopicColor
        if (name.isEmpty()) return
        viewModelScope.launch {
            val saved = topicRepo.create(name, color) ?: return@launch
            _state.update {
                it.copy(
                    topics = listOf(saved) + it.topics,
                    showAddDialog = false,
                    newTopicName = "",
                    currentTopicId = saved.id
                )
            }
            prefs.setLastSelectedTopicId(saved.id)
        }
    }

    fun openEdit(topic: FocusTopic) = _state.update { it.copy(editingTopic = topic) }
    fun closeEdit() = _state.update { it.copy(editingTopic = null) }

    fun rename(id: String, newName: String) {
        viewModelScope.launch {
            val ok = topicRepo.rename(id, newName)
            if (ok) refresh()
            _state.update { it.copy(editingTopic = null) }
        }
    }

    fun setColor(id: String, hex: String) {
        viewModelScope.launch {
            topicRepo.setColor(id, hex)
            refresh()
        }
    }

    fun archive(id: String) {
        viewModelScope.launch {
            topicRepo.archive(id)
            // 如果归档的是当前选中，清空
            if (_state.value.currentTopicId == id) {
                prefs.setLastSelectedTopicId(null)
            }
            refresh()
            _state.update { it.copy(editingTopic = null) }
        }
    }
}
