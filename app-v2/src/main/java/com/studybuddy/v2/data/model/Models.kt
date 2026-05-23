package com.studybuddy.v2.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// ═════════════════════════════════════════════════════════════════════════════
// 业务实体 —— 与 PocketBase 9 collections schema 字段名 1:1 对齐
// （pb_schema.json 已确认）
// 同时部分实体（FocusSession / Pet）兼任 Room @Entity，本地缓存用同一份结构
// ═════════════════════════════════════════════════════════════════════════════

@Serializable
enum class FocusType { SINGLE, SYNC }

@Serializable
enum class FocusStatus { ACTIVE, PAUSED, COMPLETED, ABORTED }

@Serializable
enum class PetBreed { ORANGE_CAT, SIAMESE_CAT, SADDLE_CAT, FOX, RABBIT, COW, SHIBA, CAPYBARA, HEDGEHOG }

@Serializable
enum class GrowthStage { EGG, BABY, YOUNG, ADULT, ULTIMATE }

// ─── PB users collection ─────────────────────────────────────────────────────
@Serializable
data class UserProfile(
    val id: String,
    val email: String = "",
    val nickname: String = "",
    val avatarUrl: String? = null,
    val partnerId: String? = null,
    val partnerSince: Long? = null
)

// ─── PB relationships collection ─────────────────────────────────────────────
@Serializable
data class Relationship(
    val id: String,
    val userAId: String,
    val userBId: String,
    val status: String = "active",
    val boundAt: Long = 0,
    val streakDays: Int = 0,
    val totalFocusSessions: Int = 0,
    val intimacyScore: Float = 0f,
    val petId: String? = null,
    val activeBreed: String = "ORANGE_CAT"
)

// ─── PB status collection ────────────────────────────────────────────────────
@Serializable
data class RealtimeStatus(
    val id: String = "",
    val userId: String,
    val online: Boolean = false,
    val focusStatus: String = "IDLE",   // IDLE / ACTIVE / PAUSED
    val currentSessionId: String? = null,
    val currentFocusSeconds: Long = 0,
    val todayFocusSeconds: Long = 0,
    val lastHeartbeat: Long = 0,
    val isInLibrary: Boolean = false,
    val libraryName: String? = null,
    val coarseLat: Double? = null,    // 500m 精度位置（双人姿态判定用）
    val coarseLng: Double? = null,
    val lastLocAt: Long = 0           // 上次定位写入时刻；30 分钟内有效
)

// ─── PB sessions collection + Room entity ───────────────────────────────────
@Entity(tableName = "sessions")
@Serializable
data class FocusSession(
    @PrimaryKey val id: String,
    val userId: String,
    val partnerId: String? = null,
    val type: String = "SINGLE",            // FocusType.name
    val status: String = "ACTIVE",          // FocusStatus.name
    val goal: String = "",
    val startedAt: Long = 0,
    val pausedAt: Long? = null,
    val totalPausedMs: Long = 0,
    val endedAt: Long? = null,
    val plannedDurationMs: Long = 25 * 60 * 1000,
    val actualDurationMs: Long? = null,
    val pointsEarned: Int = 0,
    val isInLibrary: Boolean = false,
    val breakCount: Int = 0,
    val tag: String = "",
    val topicId: String? = null     // P5 多主题专注：归属哪个 FocusTopic
)

// ─── PB pets collection + Room entity ───────────────────────────────────────
@Entity(tableName = "pets")
@Serializable
data class Pet(
    @PrimaryKey val id: String,
    val pairId: String,
    val name: String = "小猫",
    val type: String = "CAT",
    val breed: String = "ORANGE_CAT",       // PetBreed.name
    val growthStage: String = "BABY",        // GrowthStage.name
    val ageDays: Int = 1,
    val level: Int = 1,
    val exp: Int = 0,
    val hunger: Float = 80f,
    val mood: Float = 80f,
    val cleanliness: Float = 80f,
    val intimacy: Float = 50f,
    val pixelSpriteId: String = "cat_default",
    val createdAt: Long = 0,
    val lastFedAt: Long? = null,
    val lastInteractionAt: Long? = null,
    val lastDecayAt: Long = 0   // 上次衰减计算时间戳，0 = 未初始化（按 createdAt 兜底）
)

// Helper extension
fun Pet.growthStageEnum(): GrowthStage =
    runCatching { GrowthStage.valueOf(growthStage) }.getOrDefault(GrowthStage.BABY)

fun Pet.breedEnum(): PetBreed =
    runCatching { PetBreed.valueOf(breed) }.getOrDefault(PetBreed.ORANGE_CAT)

fun FocusSession.typeEnum(): FocusType =
    runCatching { FocusType.valueOf(type) }.getOrDefault(FocusType.SINGLE)

fun FocusSession.statusEnum(): FocusStatus =
    runCatching { FocusStatus.valueOf(status) }.getOrDefault(FocusStatus.ACTIVE)

// ─── PB funds collection ─────────────────────────────────────────────────────
@kotlinx.serialization.Serializable
data class WishItem(
    val id: String = "",
    val name: String = "",
    val targetCents: Long = 0,
    val savedCents: Long = 0,
    val createdBy: String = ""
)

@kotlinx.serialization.Serializable
data class FundTransaction(
    val id: String = "",
    val pairId: String = "",
    val type: String = "DEPOSIT",     // DEPOSIT / WITHDRAWAL / PENALTY / VOIDED
    val amountCents: Long = 0,
    val note: String = "",
    val byUserId: String = "",
    val at: Long = 0,
    val voided: Boolean = false,         // 核销状态（保留记录但不计入余额）
    val voidedAt: Long? = null,
    val voidedBy: String? = null
)

@kotlinx.serialization.Serializable
data class SharedFund(
    val id: String = "",
    val pairId: String = "",
    val balanceCents: Long = 0,
    val totalInCents: Long = 0,
    val totalOutCents: Long = 0,
    val wishlist: List<WishItem> = emptyList(),
    val transactions: List<FundTransaction> = emptyList()
)

// ─── PB landmarks collection ─────────────────────────────────────────────────
// 自建地标。在地标范围内专注会被"加持" —— 不做 ×2 RPG 风，只是一句温柔的陪伴
@kotlinx.serialization.Serializable
data class Landmark(
    val id: String = "",
    val userId: String = "",
    val name: String = "",                  // 用户给的名字，如"图书馆三楼"
    val type: String = "LIBRARY",           // LIBRARY / CAFE / CLASSROOM / OTHER
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val radiusM: Int = 100,
    val multiplier: Float = 1.5f,           // 1.5 是默认轻"加持"，不是 RPG 翻倍
    val createdAt: Long = 0
)

// ─── PB sync_invites collection ──────────────────────────────────────────────
@kotlinx.serialization.Serializable
data class SyncInvite(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val plannedDurationMs: Long = 25 * 60_000L,
    val mode: String = "COUNTDOWN",         // COUNTDOWN / STOPWATCH
    val goal: String = "",
    val status: String = "PENDING",         // PENDING / ACCEPTED / DECLINED / EXPIRED / CANCELLED
    val createdAt: Long = 0,
    val expiresAt: Long = 0,
    val sessionId: String? = null
)

// ─── PB debts collection ─────────────────────────────────────────────────────
// 承诺账本：双人见证的"欠条"，App 不沾资金，仅记录与提醒，结算线下进行。
@kotlinx.serialization.Serializable
data class Debt(
    val id: String = "",
    val pairId: String = "",
    val fromUserId: String = "",     // 欠方（断连那个人）
    val toUserId: String = "",       // 被欠方
    val unitCents: Int = 500,        // 每份 ¥X，默认 ¥5
    val count: Int = 1,              // 几份
    val reason: String = "",         // 例如 "工作日断连 周一 10/25 分钟"
    val createdAt: Long = 0,
    val settled: Boolean = false,
    val settledAt: Long? = null,
    val settledBy: String? = null    // 仅被欠方可点；存 userId 留痕
)

// ─── PB quotes collection ───────────────────────────────────────────────────
// 话廊：用户记录灵光一闪的句子。默认 PRIVATE 仅自己可见，PARTNER 可见时对端能看到
@Serializable
enum class QuoteVisibility { PRIVATE, PARTNER }

@Serializable
data class Quote(
    val id: String = "",
    val authorId: String,
    val pairId: String? = null,         // 仅 PARTNER 可见时填
    val text: String,
    val source: String = "",            // 来源（书名 / 谁说的 / 留空 = 自创）
    val visibility: String = "PRIVATE", // QuoteVisibility.name
    val createdAt: Long = 0
)

// ─── PB letters collection ──────────────────────────────────────────────────
// 信件 + 纸飞机双载体。同表两种 kind：
//   LETTER —— 不限字数，慢、写情感、推送静默
//   PLANE  —— 30 字限制，快、传信号、推送锁屏 + 振动
@Serializable
enum class LetterKind { LETTER, PLANE }

@Serializable
data class Letter(
    val id: String = "",
    val pairId: String,
    val authorId: String,
    val kind: String = "LETTER",        // LetterKind.name
    val text: String,
    val createdAt: Long = 0,
    val readAt: Long? = null            // 对端打开过的时间，用于 "TA 读过了" 副标
)

/** 判断信件是否已读：readAt 为 null / 0 / 1 都视为未读（1 是 PB required 字段的 placeholder） */
fun Letter.isRead(): Boolean = (readAt ?: 0L) > 1L

fun Letter.kindEnum(): LetterKind =
    runCatching { LetterKind.valueOf(kind) }.getOrDefault(LetterKind.LETTER)

// ─── PB notes collection ────────────────────────────────────────────────────
// 便签墙：两人共建的"冰箱贴"。语义=感性 / 共有 / 必须能贴图。
// landmarkId / meetingId 都可空 —— 不属于具体地点 / 见面的随手贴是 NULL/NULL
@Serializable
data class Note(
    val id: String = "",
    val pairId: String,
    val authorId: String,
    val text: String = "",
    val imageUrls: List<String> = emptyList(),  // PB file 字段；上传后填回 URL
    val landmarkId: String? = null,    // 共享地标 id
    val meetingId: String? = null,     // 见面记录 id
    val positionX: Float = 0.5f,       // 便签墙拖动位置（0-1 比例）
    val positionY: Float = 0.5f,
    val createdAt: Long = 0
)

// ─── PB meetings collection ─────────────────────────────────────────────────
// 见面记录：异地党核心情感容器。app 自动判定双方距离 < 500m + 持续 > 60min = 一次见面
@Serializable
data class Meeting(
    val id: String = "",
    val pairId: String,
    val startedAt: Long,
    val endedAt: Long? = null,         // null = 进行中
    val centerLat: Double = 0.0,       // 500m 精度网格中心
    val centerLng: Double = 0.0,
    val durationMs: Long = 0,
    val locationName: String? = null   // 反向地理编码后填，可空
)

// ─── PB pair_landmarks collection ───────────────────────────────────────────
// 共享地标：你们一起去过的咖啡馆 / 电影院 / 旅游地。和私有 [Landmark] 区分：
// - 私有 = user 维度，用于专注加成判定
// - 共享 = pair 维度，用于"探索 tab 共在态展示" + "便签的容器"
@Serializable
data class PairLandmark(
    val id: String = "",
    val pairId: String,
    val name: String = "",          // 反向地理编码 / 用户起的名字
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val firstVisitedAt: Long = 0,
    val visitCount: Int = 1,
    val lastVisitedAt: Long = 0
)

// ─── PB unbind_requests collection ──────────────────────────────────────────
// 解绑请求：7 天冷静期，期间宠物冬眠
@Serializable
data class UnbindRequest(
    val id: String = "",
    val pairId: String,
    val byUserId: String,
    val createdAt: Long = 0,
    val cooldownEndsAt: Long = 0,    // createdAt + 7 天
    val cancelled: Boolean = false,
    val cancelledAt: Long? = null,
    val cancelledBy: String? = null
)

// ─── PB focus_topics collection ─────────────────────────────────────────────
// 多主题专注（鞍部风铭牌）。每个用户可以建多个主题（"GRE 单词" / "毕设" / "论文阅读"），
// FocusSession 记录 topicId，Stats 按主题切片。
//
// 不带衰减、不"养成"——它就是事项的标签。chip 颜色用户选，但鞍部风偏低饱和。
@Serializable
data class FocusTopic(
    val id: String = "",
    val userId: String,
    val name: String = "",          // 衬线大字主名："GRE 单词"
    val colorHex: String = "#CC785C",  // 8×8 像素方块色（默认 coral）
    val totalFocusMs: Long = 0,    // 累计专注毫秒
    val sessionCount: Int = 0,
    val createdAt: Long = 0,
    val archivedAt: Long? = null   // 归档（不删，保留累计）
)
