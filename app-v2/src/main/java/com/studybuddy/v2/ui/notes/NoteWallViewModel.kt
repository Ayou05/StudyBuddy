package com.studybuddy.v2.ui.notes

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.model.Note
import com.studybuddy.v2.data.repo.NoteRepo
import com.studybuddy.v2.data.repo.PreferencesStore
import com.studybuddy.v2.data.repo.UserRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteWallUiState(
    val notes: List<Note> = emptyList(),
    val showEditDialog: Boolean = false,
    val draft: String = "",
    val pickedImages: List<Uri> = emptyList(),  // 用户选了但还没上传的图片
    val myUserId: String = "",
    val myNickname: String = "",
    val partnerNickname: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false
)

@HiltViewModel
class NoteWallViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val noteRepo: NoteRepo,
    private val prefs: PreferencesStore,
    private val userRepo: UserRepo
) : ViewModel() {

    private val _state = MutableStateFlow(NoteWallUiState())
    val state: StateFlow<NoteWallUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val list = noteRepo.list()
            val myId = prefs.currentUserId.first().orEmpty()
            val myNick = prefs.userNickname.first().orEmpty()
            val partnerNick = userRepo.getPartner()?.nickname.orEmpty()
            _state.update {
                it.copy(
                    notes = list,
                    loading = false,
                    myUserId = myId,
                    myNickname = myNick,
                    partnerNickname = partnerNick
                )
            }
        }
    }

    fun openEdit() {
        _state.update { it.copy(showEditDialog = true, draft = "", pickedImages = emptyList()) }
    }

    fun closeEdit() {
        _state.update { it.copy(showEditDialog = false, draft = "", pickedImages = emptyList()) }
    }

    fun updateDraft(text: String) {
        _state.update { it.copy(draft = text) }
    }

    fun addImages(uris: List<Uri>) {
        val combined = (_state.value.pickedImages + uris).take(4)  // 最多 4 张
        _state.update { it.copy(pickedImages = combined) }
    }

    fun removeImage(uri: Uri) {
        _state.update { it.copy(pickedImages = it.pickedImages - uri) }
    }

    fun saveNote() {
        val draft = _state.value.draft.trim()
        val pics = _state.value.pickedImages
        if (draft.isEmpty() && pics.isEmpty()) return
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val saved = if (pics.isEmpty()) {
                noteRepo.create(text = draft)
            } else {
                // 读取图片字节
                val imageData = pics.mapIndexedNotNull { idx, uri ->
                    try {
                        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: return@mapIndexedNotNull null
                        val ext = guessExt(ctx, uri)
                        "note_${System.currentTimeMillis()}_$idx.$ext" to bytes
                    } catch (e: Exception) {
                        android.util.Log.e("NoteWallVM", "read image failed: $uri", e)
                        null
                    }
                }
                if (imageData.isEmpty()) {
                    noteRepo.create(text = draft)
                } else {
                    noteRepo.createWithImages(text = draft, images = imageData)
                }
            }
            if (saved != null) {
                val newList = listOf(saved) + _state.value.notes
                _state.update {
                    it.copy(
                        notes = newList,
                        showEditDialog = false,
                        draft = "",
                        pickedImages = emptyList(),
                        saving = false
                    )
                }
            } else {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    private fun guessExt(ctx: Context, uri: Uri): String {
        val mime = ctx.contentResolver.getType(uri) ?: ""
        return when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            else -> "jpg"
        }
    }

    fun moveNote(noteId: String, x: Float, y: Float) {
        // 优化：先本地更新，再异步写 PB
        val updated = _state.value.notes.map {
            if (it.id == noteId) it.copy(positionX = x, positionY = y) else it
        }
        _state.update { it.copy(notes = updated) }
        viewModelScope.launch { noteRepo.updatePosition(noteId, x, y) }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            val ok = noteRepo.delete(noteId)
            if (ok) {
                _state.update { it.copy(notes = it.notes.filter { n -> n.id != noteId }) }
            }
        }
    }
}
