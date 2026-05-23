package com.studybuddy.v2.data.repo

import com.studybuddy.v2.data.model.Relationship
import com.studybuddy.v2.data.model.UserProfile
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepo @Inject constructor(
    private val pb: PbClient,
    private val prefs: PreferencesStore
) {
    val currentUserId: Flow<String?> = prefs.currentUserId

    suspend fun getMe(): UserProfile? {
        val id = prefs.currentUserId.first() ?: return null
        return try {
            pb.getRecord<UserProfile>(PbConfig.USERS, id)
        } catch (_: PbException) { null }
    }

    suspend fun getPartner(): UserProfile? {
        val me = getMe() ?: return null
        val pid = me.partnerId ?: return null
        return try {
            pb.getRecord<UserProfile>(PbConfig.USERS, pid)
        } catch (_: PbException) { null }
    }

    /** 当前关系（如果用户已绑定）。 */
    suspend fun getRelationship(): Relationship? {
        val id = prefs.currentUserId.first() ?: return null
        return try {
            val list = pb.listRecords<Relationship>(
                collection = PbConfig.RELATIONSHIPS,
                filter = "(userAId='$id' || userBId='$id')",
                perPage = 1
            )
            list.items.firstOrNull()
        } catch (_: PbException) { null }
    }
}
