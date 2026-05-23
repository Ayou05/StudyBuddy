package com.studybuddy.v2.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.model.Landmark
import com.studybuddy.v2.data.model.UserProfile
import com.studybuddy.v2.data.repo.LandmarkRepo
import com.studybuddy.v2.data.repo.PoseRepo
import com.studybuddy.v2.data.repo.UserRepo
import com.studybuddy.v2.util.Mode
import com.studybuddy.v2.util.PoseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExploreUiState(
    val pose: PoseResult? = null,
    val partner: UserProfile? = null,
    val myLandmarks: List<Landmark> = emptyList(),
    val loading: Boolean = true
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val poseRepo: PoseRepo,
    private val userRepo: UserRepo,
    private val landmarkRepo: LandmarkRepo
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreUiState())
    val state: StateFlow<ExploreUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            poseRepo.refresh()
            val partner = userRepo.getPartner()
            val landmarks = landmarkRepo.myLandmarks()
            _state.update {
                it.copy(
                    pose = poseRepo.current(),
                    partner = partner,
                    myLandmarks = landmarks,
                    loading = false
                )
            }
        }
    }
}
