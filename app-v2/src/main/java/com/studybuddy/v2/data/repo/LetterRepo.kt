package com.studybuddy.v2.data.repo

import com.studybuddy.v2.data.model.Letter
import com.studybuddy.v2.data.model.LetterKind
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbException
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 信件 + 飞机仓库 —— 替代传统 IM。
 *
 * 设计立场：
 * - 同张表 [PbConfig.LETTERS]，kind=LETTER 不限字 / kind=PLANE 30 字
 * - 30 字限制写在 Repo 层强制，UI 也校验，但即便绕过也不会越界
 * - 所有 letters 都关联 pairId，没绑搭档不能写
 */
@Singleton
class LetterRepo @Inject constructor(
    private val pb: PbClient,
    private val prefs: PreferencesStore,
    private val userRepo: UserRepo
) {
    companion object {
        const val PLANE_MAX_CHARS = 30
    }

    /** 拉这对 pair 的所有 letters（按时间倒序）。 */
    suspend fun list(): List<Letter> {
        val rel = userRepo.getRelationship() ?: return emptyList()
        return try {
            pb.listRecords<Letter>(
                collection = PbConfig.LETTERS,
                filter = "pairId='${rel.id}'",
                sort = "-createdAt",
                perPage = 100
            ).items
        } catch (_: PbException) { emptyList() }
    }

    /** 寄一封信 */
    suspend fun sendLetter(text: String): Letter? = send(text, LetterKind.LETTER)

    /** 寄一架飞机（30 字限制） */
    suspend fun sendPlane(text: String): Letter? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.length > PLANE_MAX_CHARS) return null
        return send(trimmed, LetterKind.PLANE)
    }

    private suspend fun send(text: String, kind: LetterKind): Letter? {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return null
        val me = prefs.currentUserId.first() ?: run {
            android.util.Log.e("LetterRepo", "send failed: no currentUserId")
            return null
        }
        val rel = userRepo.getRelationship() ?: run {
            android.util.Log.e("LetterRepo", "send failed: no relationship")
            return null
        }
        // 注意：PB 后台无法设置 default，必填 number 字段传 0 会被认为空，必须传非 0
        // readAt 用 1 表示"未读"（用户读后会更新为真实 timestamp）
        val fields = mapOf(
            "pairId" to rel.id,
            "authorId" to me,
            "kind" to kind.name,
            "text" to cleaned,
            "createdAt" to System.currentTimeMillis(),
            "readAt" to 1L  // 1 = 未读 placeholder（PB number required 字段不能为 0）
        )
        return try {
            val result = pb.createRecord<Letter>(PbConfig.LETTERS, fields)
            android.util.Log.i("LetterRepo", "send success: id=${result.id}, kind=${kind.name}")
            result
        } catch (e: PbException) {
            android.util.Log.e("LetterRepo", "send failed: ${e.message}", e)
            null
        } catch (e: Exception) {
            android.util.Log.e("LetterRepo", "send failed (other): ${e.message}", e)
            null
        }
    }

    /** 标记一封信已读（对端打开时调用）。 */
    suspend fun markRead(letterId: String) {
        try {
            pb.updateRecord<Letter>(
                collection = PbConfig.LETTERS,
                id = letterId,
                fields = mapOf("readAt" to System.currentTimeMillis())
            )
        } catch (_: Exception) {}
    }
}
