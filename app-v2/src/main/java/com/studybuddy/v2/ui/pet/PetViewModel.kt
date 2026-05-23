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

data class PetUiState(
    val loading: Boolean = true,
    val hasPartner: Boolean = false,
    val pet: Pet? = null,
    val unlockedSaddleCat: Boolean = false,
    val currentEmote: String = "idle",
    val feedingTrigger: Long = 0L
)

@HiltViewModel
class PetViewModel @Inject constructor(
    private val userRepo: UserRepo,
    private val petRepo: PetRepo,
    private val prefs: PreferencesStore
) : ViewModel() {

    private val _state = MutableStateFlow(PetUiState())
    val state: StateFlow<PetUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val unlocked = prefs.unlockedSaddleCat.first()
            val rel = userRepo.getRelationship()
            if (rel == null) {
                _state.update { it.copy(loading = false, hasPartner = false, pet = null, unlockedSaddleCat = unlocked) }
                return@launch
            }
            val pet = petRepo.getOrFetchByPair(rel.id)
            _state.update { it.copy(loading = false, hasPartner = true, pet = pet, unlockedSaddleCat = unlocked) }
        }
    }

    fun feed() = applyInteract { petRepo.interactFeed(it) }.also {
        _state.update { it.copy(currentEmote = "happy", feedingTrigger = System.currentTimeMillis()) }
        emoteAutoReset()
    }
    fun play() = applyInteract { petRepo.interactPlay(it) }.also {
        _state.update { it.copy(currentEmote = "happy") }; emoteAutoReset()
    }
    fun clean() = applyInteract { petRepo.interactClean(it) }
    fun stroke() = applyInteract { petRepo.interactStroke(it) }.also {
        _state.update { it.copy(currentEmote = "happy") }; emoteAutoReset()
    }

    fun setBreed(target: PetBreed) {
        val pet = _state.value.pet ?: return
        if (pet.breed == target.name) return
        viewModelScope.launch {
            val updated = petRepo.setBreed(pet, target)
            _state.update { it.copy(pet = updated) }
        }
    }

    fun renamePet(newName: String) {
        val pet = _state.value.pet ?: return
        viewModelScope.launch {
            val updated = petRepo.renamePet(pet, newName)
            _state.update { it.copy(pet = updated) }
        }
    }

    private fun emoteAutoReset() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2_500)
            _state.update { it.copy(currentEmote = "idle") }
        }
    }

    fun cycleBreed() {
        val pet = _state.value.pet ?: return
        val current = runCatching { PetBreed.valueOf(pet.breed) }.getOrDefault(PetBreed.ORANGE_CAT)
        val unlocked = _state.value.unlockedSaddleCat
        val pool = if (unlocked) PetBreed.values().toList()
                   else listOf(PetBreed.ORANGE_CAT, PetBreed.SIAMESE_CAT)
        val next = pool[(pool.indexOf(current) + 1) % pool.size]
        viewModelScope.launch {
            val updated = petRepo.setBreed(pet, next)
            _state.update { it.copy(pet = updated) }
        }
    }

    private fun applyInteract(block: suspend (Pet) -> Pet) {
        viewModelScope.launch {
            val pet = _state.value.pet ?: return@launch
            val next = block(pet)
            _state.update { it.copy(pet = next) }
        }
    }
}
