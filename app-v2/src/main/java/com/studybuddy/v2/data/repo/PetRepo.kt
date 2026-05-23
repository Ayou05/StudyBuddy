package com.studybuddy.v2.data.repo

import com.studybuddy.v2.data.model.Pet
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbException
import com.studybuddy.v2.data.room.PetDao
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PetRepo @Inject constructor(
    private val pb: PbClient,
    private val dao: PetDao,
    private val unbindRepo: UnbindRepo,
    private val prefs: PreferencesStore
) {
    suspend fun getOrFetchByPair(pairId: String): Pet? {
        // 先看本地
        dao.getByPair(pairId)?.let { return applyDecay(migrateIfSaddleCat(it)) }
        // 拉 PB
        val remote = try {
            pb.listRecords<Pet>(
                collection = PbConfig.PETS,
                filter = "pairId='$pairId'",
                perPage = 1
            ).items.firstOrNull()
        } catch (_: PbException) { null }
        if (remote != null) {
            dao.upsert(remote)
            return applyDecay(migrateIfSaddleCat(remote))
        }
        // PB 也没有 → 给这对 pair 发一颗 EGG（让用户经历"获得 → 孵化"的过程，不是直接给娃）
        return createEggForPair(pairId)
    }

    /**
     * 自动衰减：每次读取宠物时按"距上次衰减时间"补扣四维。
     *
     * 衰减速率（白皮书核心粘性机制）：
     * - hunger：每 6h -10（一天 -40）
     * - mood：每 4h -5
     * - cleanliness：每 12h -8
     * - intimacy：每天 -2（缓慢但持续，不互动会冷淡）
     *
     * 蛋阶段（EGG）不衰减——它还没破壳，没需求。
     * lastDecayAt = 0 的老记录用 createdAt 兜底，避免一次性扣爆。
     */
    private suspend fun applyDecay(pet: Pet): Pet {
        // 蛋不衰减
        if (pet.growthStage == "EGG") return pet
        // 宠物冬眠：解绑冷静期内跳过衰减
        if (unbindRepo.hasActive(pet.pairId)) return pet
        // 娱乐模式：跳过衰减（用户不想被压力）
        if (prefs.appMode.first() == "LEISURE") return pet
        val now = System.currentTimeMillis()
        val anchor = if (pet.lastDecayAt > 0) pet.lastDecayAt
                     else if (pet.createdAt > 0) pet.createdAt
                     else now
        val elapsedMs = (now - anchor).coerceAtLeast(0)
        if (elapsedMs < 60_000L) return pet  // < 1 分钟不动，避免抖动

        val hours = elapsedMs / (60.0 * 60.0 * 1000.0)
        val days = hours / 24.0

        val newHunger = (pet.hunger - (hours / 6.0 * 10.0).toFloat()).coerceIn(0f, 100f)
        val newMood = (pet.mood - (hours / 4.0 * 5.0).toFloat()).coerceIn(0f, 100f)
        val newCleanliness = (pet.cleanliness - (hours / 12.0 * 8.0).toFloat()).coerceIn(0f, 100f)
        val newIntimacy = (pet.intimacy - (days * 2.0).toFloat()).coerceIn(0f, 100f)

        val decayed = pet.copy(
            hunger = newHunger,
            mood = newMood,
            cleanliness = newCleanliness,
            intimacy = newIntimacy,
            lastDecayAt = now
        )
        dao.upsert(decayed)
        try {
            pb.updateRecord<Pet>(PbConfig.PETS, decayed.id, mapOf(
                "hunger" to decayed.hunger,
                "mood" to decayed.mood,
                "cleanliness" to decayed.cleanliness,
                "intimacy" to decayed.intimacy,
                "lastDecayAt" to decayed.lastDecayAt
            ))
        } catch (_: Exception) {}
        return decayed
    }

    /**
     * 迁移：把残留的 pet.breed=SADDLE_CAT 自动改回 ORANGE_CAT。
     *
     * 鞍部猫不再作为 BreedSwitcher 选项（它是漫游吉祥物，跟养成宠物分属两个层）。
     * 早期解锁过鞍部猫并把养成宠物切到 SADDLE_CAT 的用户，一进 PetScreen 就会被悄悄迁回橘猫，
     * 所有四维 / 等级 / 名字保留，仅替换 breed 字段。
     */
    private suspend fun migrateIfSaddleCat(pet: Pet): Pet {
        if (pet.breed != "SADDLE_CAT") return pet
        val migrated = pet.copy(breed = "ORANGE_CAT")
        dao.upsert(migrated)
        try {
            pb.updateRecord<Pet>(PbConfig.PETS, migrated.id, mapOf("breed" to "ORANGE_CAT"))
        } catch (_: Exception) {}
        return migrated
    }

    /**
     * 给指定 pair 创建一颗蛋（EGG 阶段橘猫）。
     *
     * 调用时机：
     * - 用户首次绑定搭档后打开 Pet 页（getOrFetchByPair 没找到现有记录）
     * - dashboard / 调试入口手动重置宠物
     *
     * 不要直接给 BABY —— 蛋是养成体验的起点，"它是你领养的"这件事比"它已经存在"更黏。
     * 默认 name="未命名"，引导用户长按改名（PetScreen 改名入口）。
     */
    suspend fun createEggForPair(pairId: String): Pet? {
        val now = System.currentTimeMillis()
        val payload = mapOf(
            "pairId" to pairId,
            "name" to "未命名",
            "type" to "CAT",
            "breed" to "ORANGE_CAT",
            "growthStage" to "EGG",
            "ageDays" to 0,
            "level" to 1,
            "exp" to 0,
            "hunger" to 80f,
            "mood" to 80f,
            "cleanliness" to 80f,
            "intimacy" to 50f,
            "pixelSpriteId" to "cat_default",
            "createdAt" to now,
            "lastDecayAt" to now
        )
        return try {
            val created = pb.createRecord<Pet>(PbConfig.PETS, payload)
            dao.upsert(created)
            created
        } catch (_: Exception) {
            null
        }
    }

    /** 给宠物加一份心情/亲密度（专注完成后调用）。 */
    suspend fun rewardForFocusCompletion(pairId: String, focusMinutes: Int) {
        val pet = getOrFetchByPair(pairId) ?: return
        val moodBoost = (focusMinutes / 5).coerceAtMost(20)
        val intimacyBoost = (focusMinutes / 25 * 10).coerceAtMost(15).toFloat()
        val updated = pet.copy(
            mood = (pet.mood + moodBoost).coerceAtMost(100f),
            intimacy = (pet.intimacy + intimacyBoost).coerceAtMost(100f),
            lastInteractionAt = System.currentTimeMillis()
        )
        dao.upsert(updated)
        try {
            pb.updateRecord<Pet>(PbConfig.PETS, updated.id, mapOf(
                "mood" to updated.mood,
                "intimacy" to updated.intimacy,
                "lastInteractionAt" to updated.lastInteractionAt
            ))
        } catch (_: Exception) {}
    }

    suspend fun interactFeed(pet: Pet): Pet = applyDelta(pet, hunger = +20f, mood = +5f, intimacy = +2f) {
        copy(lastFedAt = System.currentTimeMillis())
    }
    suspend fun interactPlay(pet: Pet): Pet = applyDelta(pet, mood = +15f, intimacy = +5f)
    suspend fun interactClean(pet: Pet): Pet = applyDelta(pet, cleanliness = +25f, mood = +3f)
    suspend fun interactStroke(pet: Pet): Pet = applyDelta(pet, intimacy = +8f, mood = +5f)

    /** 切换宠物品种。本期单方面切（白皮书原本有投票机制，作为 Backlog）。 */
    suspend fun setBreed(pet: Pet, breed: com.studybuddy.v2.data.model.PetBreed): Pet {
        val next = pet.copy(breed = breed.name)
        dao.upsert(next)
        try {
            pb.updateRecord<Pet>(PbConfig.PETS, next.id, mapOf("breed" to next.breed))
        } catch (_: Exception) {}
        return next
    }

    /** 用户给宠物改名。trim 后空字符串保持 "未命名"，避免 UI 显示空白。 */
    suspend fun renamePet(pet: Pet, newName: String): Pet {
        val cleaned = newName.trim().take(12).ifBlank { "未命名" }
        if (cleaned == pet.name) return pet
        val next = pet.copy(name = cleaned)
        dao.upsert(next)
        try {
            pb.updateRecord<Pet>(PbConfig.PETS, next.id, mapOf("name" to next.name))
        } catch (_: Exception) {}
        return next
    }

    private suspend fun applyDelta(
        pet: Pet,
        hunger: Float = 0f,
        mood: Float = 0f,
        cleanliness: Float = 0f,
        intimacy: Float = 0f,
        moreUpdate: Pet.() -> Pet = { this }
    ): Pet {
        val now = System.currentTimeMillis()
        val nextPet = pet.copy(
            hunger = (pet.hunger + hunger).coerceIn(0f, 100f),
            mood = (pet.mood + mood).coerceIn(0f, 100f),
            cleanliness = (pet.cleanliness + cleanliness).coerceIn(0f, 100f),
            intimacy = (pet.intimacy + intimacy).coerceIn(0f, 100f),
            lastInteractionAt = now,
            lastDecayAt = now    // 互动同时刷新衰减锚点，避免下次读时双重扣
        ).moreUpdate()
        dao.upsert(nextPet)
        try {
            pb.updateRecord<Pet>(PbConfig.PETS, nextPet.id, mapOf(
                "hunger" to nextPet.hunger,
                "mood" to nextPet.mood,
                "cleanliness" to nextPet.cleanliness,
                "intimacy" to nextPet.intimacy,
                "lastInteractionAt" to nextPet.lastInteractionAt,
                "lastFedAt" to nextPet.lastFedAt,
                "lastDecayAt" to nextPet.lastDecayAt
            ))
        } catch (_: Exception) {}
        return nextPet
    }
}
