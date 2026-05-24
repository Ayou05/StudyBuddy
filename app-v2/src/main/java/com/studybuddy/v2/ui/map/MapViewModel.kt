package com.studybuddy.v2.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.model.PairLandmark
import com.studybuddy.v2.data.model.RealtimeStatus
import com.studybuddy.v2.data.model.UserProfile
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbRealtime
import com.studybuddy.v2.data.repo.LocationRepo
import com.studybuddy.v2.data.repo.PairLandmarkRepo
import com.studybuddy.v2.data.repo.StatusRepo
import com.studybuddy.v2.data.repo.UserRepo
import com.studybuddy.v2.util.PoseCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

data class MapUiState(
    val loading: Boolean = true,
    val partner: UserProfile? = null,
    val partnerLat: Double? = null,
    val partnerLng: Double? = null,
    val partnerFresh: Boolean = false,  // 位置是否在 30 分钟内
    val myLat: Double? = null,
    val myLng: Double? = null,
    val distanceM: Double? = null,
    val landmarks: List<PairLandmark> = emptyList()
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val userRepo: UserRepo,
    private val statusRepo: StatusRepo,
    private val locationRepo: LocationRepo,
    private val landmarkRepo: PairLandmarkRepo,
    private val realtime: PbRealtime
) : ViewModel() {

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    init {
        refresh()
        startPartnerWatcher()
        startPeriodicLocation()
    }

    fun refresh() {
        viewModelScope.launch {
            val partner = userRepo.getPartner()
            _state.update { it.copy(partner = partner) }
            val rel = userRepo.getRelationship()
            if (rel != null) {
                runCatching { landmarkRepo.list() }.getOrDefault(emptyList()).let { list ->
                    _state.update { it.copy(landmarks = list) }
                }
            }
            if (partner != null) {
                val s = statusRepo.getPartnerStatus(partner.id)
                applyPartner(s)
            }
            _state.update { it.copy(loading = false) }
        }
    }

    /** 主动报一次自己的位置，并算距离 */
    fun tickMyLocation() {
        viewModelScope.launch {
            val ll = locationRepo.currentOnce() ?: return@launch
            statusRepo.setCoarseLocation(ll.lat, ll.lng)
            _state.update { it.copy(myLat = ll.lat, myLng = ll.lng) }
            recomputeDistance()
        }
    }

    private fun startPeriodicLocation() {
        viewModelScope.launch {
            while (true) {
                tickMyLocation()
                delay(5 * 60 * 1000L)  // 每 5 分钟报一次
            }
        }
    }

    private fun startPartnerWatcher() {
        viewModelScope.launch {
            val partner = userRepo.getPartner() ?: return@launch
            realtime.events.collect { ev ->
                if (ev.topic != PbConfig.STATUS) return@collect
                val pid = ev.record["userId"]?.jsonPrimitive?.contentOrNull
                if (pid != partner.id) return@collect
                runCatching {
                    Json { ignoreUnknownKeys = true }
                        .decodeFromJsonElement(RealtimeStatus.serializer(), ev.record)
                }.getOrNull()?.let { applyPartner(it) }
            }
        }
    }

    private fun applyPartner(status: RealtimeStatus?) {
        val lat = status?.coarseLat
        val lng = status?.coarseLng
        val fresh = (System.currentTimeMillis() - (status?.lastLocAt ?: 0)) < 30 * 60 * 1000L
        _state.update { it.copy(partnerLat = lat, partnerLng = lng, partnerFresh = fresh) }
        recomputeDistance()
    }

    private fun recomputeDistance() {
        val s = _state.value
        if (s.myLat == null || s.myLng == null || s.partnerLat == null || s.partnerLng == null) {
            _state.update { it.copy(distanceM = null) }
            return
        }
        val d = PoseCalculator.distanceMeters(s.myLat, s.myLng, s.partnerLat, s.partnerLng)
        _state.update { it.copy(distanceM = d) }
    }

    fun createLandmark(name: String, lat: Double, lng: Double) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val created = landmarkRepo.create(name = name.trim().take(40), lat = lat, lng = lng)
            if (created != null) {
                _state.update { it.copy(landmarks = it.landmarks + created) }
            }
        }
    }
}
