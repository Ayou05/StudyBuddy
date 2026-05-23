package com.studybuddy.v2.ui.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.model.Pet
import com.studybuddy.v2.data.model.PetBreed
import com.studybuddy.v2.data.repo.PetRepo
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

data class CodexUiState(
    val loading: Boolean = true,
    val pet: Pet? = null,
    val unlockedSaddleCat: Boolean = false,
    val pendingBreed: PetBreed? = null   // 用户点了某个品种但还没确认
)

@HiltViewModel
class CodexViewModel @Inject constructor(
    private val userRepo: UserRepo,
    private val petRepo: PetRepo,
    private val prefs: PreferencesStore
) : ViewModel() {

    private val _state = MutableStateFlow(CodexUiState())
    val state: StateFlow<CodexUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val unlocked = prefs.unlockedSaddleCat.first()
            val rel = userRepo.getRelationship() ?: run {
                _state.update { it.copy(loading = false, unlockedSaddleCat = unlocked) }
                return@launch
            }
            val pet = petRepo.getOrFetchByPair(rel.id)
            _state.update { it.copy(loading = false, pet = pet, unlockedSaddleCat = unlocked) }
        }
    }

    fun pickBreed(breed: PetBreed) {
        _state.update { it.copy(pendingBreed = breed) }
    }

    fun cancelPick() {
        _state.update { it.copy(pendingBreed = null) }
    }

    fun confirmPick(onDone: () -> Unit) {
        val pet = _state.value.pet ?: return
        val target = _state.value.pendingBreed ?: return
        if (pet.breed == target.name) {
            _state.update { it.copy(pendingBreed = null) }
            onDone()
            return
        }
        viewModelScope.launch {
            val updated = petRepo.setBreed(pet, target)
            _state.update { it.copy(pet = updated, pendingBreed = null) }
            onDone()
        }
    }
}
