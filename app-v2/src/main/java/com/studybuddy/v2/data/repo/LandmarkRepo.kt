package com.studybuddy.v2.data.repo

import com.studybuddy.v2.data.model.Landmark
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbException
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LandmarkRepo @Inject constructor(
    private val pb: PbClient,
    private val prefs: PreferencesStore
) {
    suspend fun myLandmarks(): List<Landmark> {
        val me = prefs.currentUserId.first() ?: return emptyList()
        return try {
            pb.listRecords<Landmark>(
                collection = PbConfig.LANDMARKS,
                filter = "userId='$me'",
                perPage = 50
            ).items
        } catch (_: PbException) { emptyList() }
    }

    suspend fun add(name: String, type: String, lat: Double, lng: Double, radiusM: Int = 100): Landmark? {
        val me = prefs.currentUserId.first() ?: return null
        val fields = mapOf(
            "userId" to me,
            "name" to name,
            "type" to type,
            "lat" to lat,
            "lng" to lng,
            "radiusM" to radiusM,
            "multiplier" to 1.5f,
            "createdAt" to System.currentTimeMillis()
        )
        return try {
            pb.createRecord<Landmark>(PbConfig.LANDMARKS, fields)
        } catch (_: PbException) { null }
    }

    suspend fun delete(id: String): Boolean {
        return try { pb.deleteRecord(PbConfig.LANDMARKS, id); true } catch (_: PbException) { false }
    }
}
