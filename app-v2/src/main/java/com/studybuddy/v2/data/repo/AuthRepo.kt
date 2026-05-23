package com.studybuddy.v2.data.repo

import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 登录/注册/登出 + 当前用户 ID 暴露。
 *
 * 启动时调用 [restoreFromCache] 把 DataStore 里的 token 注回 PbClient。
 */
@Singleton
class AuthRepo @Inject constructor(
    private val pb: PbClient,
    private val prefs: PreferencesStore
) {
    val currentUserId: Flow<String?> = prefs.currentUserId
    val currentNickname: Flow<String?> = prefs.userNickname

    /** App 启动时调用，让 PbClient 拿到上次的 token。 */
    suspend fun restoreFromCache() {
        val token = prefs.pbToken.first()
        pb.setAuthToken(token)
    }

    suspend fun isLoggedIn(): Boolean = prefs.pbToken.first() != null

    /**
     * 登录。失败抛 [PbException]（错误信息已中文化或带原始 message）。
     */
    suspend fun login(email: String, password: String) {
        val resp = pb.authWithPassword(email.trim(), password)
        pb.setAuthToken(resp.token)
        prefs.setPbToken(resp.token)
        prefs.setCurrentUserId(resp.record.id)
        prefs.setUserEmail(resp.record.email.ifBlank { email })
        prefs.setUserNickname(resp.record.nickname.ifBlank { resp.record.name.ifBlank { resp.record.email.substringBefore("@") } })
    }

    /**
     * 注册并自动登录。
     */
    suspend fun register(email: String, password: String, nickname: String) {
        pb.createUser(email.trim(), password, nickname.trim().ifBlank { email.substringBefore("@") })
        // 创建后自动登录拿 token
        login(email, password)
    }

    suspend fun logout() {
        pb.setAuthToken(null)
        prefs.setPbToken(null)
        prefs.setCurrentUserId(null)
        prefs.setUserEmail(null)
        prefs.setUserNickname(null)
    }
}
