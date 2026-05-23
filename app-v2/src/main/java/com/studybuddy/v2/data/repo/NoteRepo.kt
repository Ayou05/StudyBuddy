package com.studybuddy.v2.data.repo

import com.studybuddy.v2.data.model.Note
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbException
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 便签仓库 —— 两人共建的"冰箱贴"，必须支持图片。
 *
 * P5 阶段不做 PB 文件上传（要 multipart 改 PbClient），先支持纯文字 + 外部 URL。
 * P6 加上 PB file 字段上传支持时改这里。
 */
@Singleton
class NoteRepo @Inject constructor(
    private val pb: PbClient,
    private val prefs: PreferencesStore,
    private val userRepo: UserRepo
) {
    /** 拉所有便签（按时间倒序） */
    suspend fun list(): List<Note> {
        val rel = userRepo.getRelationship() ?: return emptyList()
        return try {
            pb.listRecords<Note>(
                collection = PbConfig.NOTES,
                filter = "pairId='${rel.id}'",
                sort = "-createdAt",
                perPage = 200
            ).items
        } catch (_: PbException) { emptyList() }
    }

    /** 拉关联到某共享地标的便签（地标详情页用） */
    suspend fun listByLandmark(landmarkId: String): List<Note> {
        val rel = userRepo.getRelationship() ?: return emptyList()
        return try {
            pb.listRecords<Note>(
                collection = PbConfig.NOTES,
                filter = "pairId='${rel.id}' && landmarkId='$landmarkId'",
                sort = "-createdAt",
                perPage = 100
            ).items
        } catch (_: PbException) { emptyList() }
    }

    /** 拉关联到某次见面的便签（见面详情页用） */
    suspend fun listByMeeting(meetingId: String): List<Note> {
        val rel = userRepo.getRelationship() ?: return emptyList()
        return try {
            pb.listRecords<Note>(
                collection = PbConfig.NOTES,
                filter = "pairId='${rel.id}' && meetingId='$meetingId'",
                sort = "-createdAt",
                perPage = 100
            ).items
        } catch (_: PbException) { emptyList() }
    }

    suspend fun create(
        text: String,
        imageUrls: List<String> = emptyList(),
        landmarkId: String? = null,
        meetingId: String? = null,
        positionX: Float = 0.5f,
        positionY: Float = 0.5f
    ): Note? {
        val me = prefs.currentUserId.first() ?: run {
            android.util.Log.e("NoteRepo", "create failed: no currentUserId")
            return null
        }
        val rel = userRepo.getRelationship() ?: run {
            android.util.Log.e("NoteRepo", "create failed: no relationship")
            return null
        }
        val fields = mutableMapOf<String, Any?>(
            "pairId" to rel.id,
            "authorId" to me,
            "text" to text,
            "positionX" to positionX,
            "positionY" to positionY,
            "createdAt" to System.currentTimeMillis()
        )
        // landmarkId / meetingId 必须传字符串（空也行），避免 null
        fields["landmarkId"] = landmarkId ?: ""
        fields["meetingId"] = meetingId ?: ""
        return try {
            val result = pb.createRecord<Note>(PbConfig.NOTES, fields)
            android.util.Log.i("NoteRepo", "create success: id=${result.id}")
            result
        } catch (e: PbException) {
            android.util.Log.e("NoteRepo", "create failed: ${e.message}", e)
            null
        } catch (e: Exception) {
            android.util.Log.e("NoteRepo", "create failed (other): ${e.message}", e)
            null
        }
    }

    /**
     * 创建带图片的便签（multipart 上传）。
     * @param images list of (filename, bytes)，最多 4 张，每张 < 5MB
     */
    suspend fun createWithImages(
        text: String,
        images: List<Pair<String, ByteArray>>,
        landmarkId: String? = null,
        meetingId: String? = null,
        positionX: Float = 0.5f,
        positionY: Float = 0.5f
    ): Note? {
        val me = prefs.currentUserId.first() ?: run {
            android.util.Log.e("NoteRepo", "createWithImages failed: no currentUserId")
            return null
        }
        val rel = userRepo.getRelationship() ?: run {
            android.util.Log.e("NoteRepo", "createWithImages failed: no relationship")
            return null
        }
        val fields = mutableMapOf<String, Any?>(
            "pairId" to rel.id,
            "authorId" to me,
            "text" to text,
            "positionX" to positionX,
            "positionY" to positionY,
            "createdAt" to System.currentTimeMillis(),
            "landmarkId" to (landmarkId ?: ""),
            "meetingId" to (meetingId ?: "")
        )
        return try {
            val result = pb.createRecordWithFiles<Note>(
                collection = PbConfig.NOTES,
                fields = fields,
                files = mapOf("imageUrls" to images)
            )
            android.util.Log.i("NoteRepo", "createWithImages success: id=${result.id}, images=${images.size}")
            result
        } catch (e: PbException) {
            android.util.Log.e("NoteRepo", "createWithImages failed: ${e.message}", e)
            null
        } catch (e: Exception) {
            android.util.Log.e("NoteRepo", "createWithImages failed (other): ${e.message}", e)
            null
        }
    }

    /** 构造图片完整 URL。PB file URL 格式：BASE_URL + api/files/{collectionId}/{recordId}/{filename} */
    fun fileUrl(note: Note, filename: String): String {
        // collectionName 直接用 PbConfig.NOTES
        return "${PbConfig.BASE_URL}api/files/${PbConfig.NOTES}/${note.id}/$filename"
    }

    suspend fun updatePosition(noteId: String, x: Float, y: Float) {
        try {
            pb.updateRecord<Note>(
                collection = PbConfig.NOTES,
                id = noteId,
                fields = mapOf("positionX" to x, "positionY" to y)
            )
        } catch (_: Exception) {}
    }

    suspend fun delete(noteId: String): Boolean = try {
        pb.deleteRecord(PbConfig.NOTES, noteId); true
    } catch (_: PbException) { false }
}
