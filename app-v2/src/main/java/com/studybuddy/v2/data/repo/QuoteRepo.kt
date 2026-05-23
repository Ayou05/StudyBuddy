package com.studybuddy.v2.data.repo

import com.studybuddy.v2.data.model.Quote
import com.studybuddy.v2.data.model.QuoteVisibility
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbException
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 话廊 —— 灵光一闪的句子。
 *
 * 设计哲学："一个小角落，不值得开 app，但塞进首页第二屏，要用户自己发现。"
 *
 * 可见性：
 *   PRIVATE  —— 仅自己可见（默认）
 *   PARTNER  —— 双方可见，对端拉自己 pairId 下的所有 PARTNER 条
 */
@Singleton
class QuoteRepo @Inject constructor(
    private val pb: PbClient,
    private val prefs: PreferencesStore,
    private val userRepo: UserRepo
) {

    suspend fun list(): List<Quote> {
        val me = prefs.currentUserId.first() ?: return emptyList()
        val rel = userRepo.getRelationship()
        val pairId = rel?.id
        // 我自己写的所有 + 对端写给"我俩"的 PARTNER 条
        val mineFilter = "authorId='$me'"
        val partnerFilter = if (pairId != null) " || (visibility='PARTNER' && pairId='$pairId')" else ""
        return try {
            val list = pb.listRecords<Quote>(
                collection = PbConfig.QUOTES,
                filter = "$mineFilter$partnerFilter",
                sort = "-createdAt",
                perPage = 100
            )
            list.items
        } catch (_: PbException) { emptyList() }
    }

    suspend fun add(text: String, source: String, visibility: QuoteVisibility): Quote? {
        val me = prefs.currentUserId.first() ?: return null
        val pairId = if (visibility == QuoteVisibility.PARTNER) {
            userRepo.getRelationship()?.id
        } else null
        val fields = mutableMapOf<String, Any?>(
            "authorId" to me,
            "text" to text,
            "source" to source,
            "visibility" to visibility.name,
            "createdAt" to System.currentTimeMillis()
        )
        if (pairId != null) fields["pairId"] = pairId
        return try {
            pb.createRecord<Quote>(PbConfig.QUOTES, fields)
        } catch (e: PbException) {
            android.util.Log.e("QuoteRepo", "add failed", e)
            null
        } catch (e: Exception) {
            android.util.Log.e("QuoteRepo", "add failed (unexpected)", e)
            null
        }
    }

    suspend fun delete(id: String): Boolean {
        return try {
            pb.deleteRecord(PbConfig.QUOTES, id); true
        } catch (e: PbException) {
            android.util.Log.e("QuoteRepo", "delete failed id=$id", e)
            false
        } catch (e: Exception) {
            android.util.Log.e("QuoteRepo", "delete failed (unexpected) id=$id", e)
            false
        }
    }
}
