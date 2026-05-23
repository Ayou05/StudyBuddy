package com.studybuddy.v2.data.repo

import com.studybuddy.v2.data.model.Debt
import com.studybuddy.v2.data.model.statusEnum
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbException
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 承诺账本 —— App 不沾资金，仅记账与见证；结算线下进行。
 *
 * 规则：
 * - 任何一方都可写"我欠对方 N 份"或"对方欠我 N 份"；写入时 fromUserId = 欠方
 * - **仅被欠方** 可点 settle；防止欠方自己赖账
 */
@Singleton
class DebtRepo @Inject constructor(
    private val pb: PbClient,
    private val prefs: PreferencesStore,
    private val userRepo: UserRepo
) {

    suspend fun list(): List<Debt> {
        val rel = userRepo.getRelationship() ?: return emptyList()
        return try {
            val list = pb.listRecords<Debt>(
                collection = PbConfig.DEBTS,
                filter = "pairId='${rel.id}'",
                sort = "-createdAt",
                perPage = 200
            )
            list.items
        } catch (_: PbException) { emptyList() }
    }

    /** 写一笔欠条（任何一方可写） */
    suspend fun record(
        fromUserId: String,
        toUserId: String,
        unitCents: Int,
        count: Int,
        reason: String
    ): Debt? {
        val rel = userRepo.getRelationship() ?: return null
        val fields = mapOf(
            "pairId" to rel.id,
            "fromUserId" to fromUserId,
            "toUserId" to toUserId,
            "unitCents" to unitCents,
            "count" to count,
            "reason" to reason,
            "createdAt" to System.currentTimeMillis(),
            "settled" to false
        )
        return try {
            pb.createRecord<Debt>(PbConfig.DEBTS, fields)
        } catch (_: PbException) { null }
    }

    /** 仅被欠方可结算。Repo 层做权限检查，UI 层不要绕过。 */
    suspend fun settle(debtId: String): Boolean {
        val me = prefs.currentUserId.first() ?: return false
        return try {
            val current = pb.getRecord<Debt>(PbConfig.DEBTS, debtId)
            if (current.toUserId != me) return false        // 防赖账
            if (current.settled) return false
            pb.updateRecord<Debt>(
                collection = PbConfig.DEBTS,
                id = debtId,
                fields = mapOf(
                    "settled" to true,
                    "settledAt" to System.currentTimeMillis(),
                    "settledBy" to me
                )
            )
            true
        } catch (_: PbException) { false }
    }

    /**
     * 删除欠条 —— 任一方都可删（用于改正错误记录或工作日断连判定误差）。
     * 与 [settle]（结算）不同：结算保留记录变成"已结算"留痕，删除是物理删掉。
     */
    suspend fun delete(debtId: String): Boolean {
        return try {
            pb.deleteRecord(PbConfig.DEBTS, debtId)
            true
        } catch (_: PbException) { false }
    }

    /**
     * 核销 —— 双方协商一致放弃这一笔欠条。
     * 与 [delete] 不同：核销保留记录但标 settled+reason 注明"已核销"，留下情感痕迹。
     * 任一方都可核销（信任双人关系不会赖账）。
     */
    suspend fun markVoid(debtId: String, voidReason: String = "已核销"): Boolean {
        val me = prefs.currentUserId.first() ?: return false
        return try {
            val current = pb.getRecord<Debt>(PbConfig.DEBTS, debtId)
            if (current.settled) return false
            pb.updateRecord<Debt>(
                collection = PbConfig.DEBTS,
                id = debtId,
                fields = mapOf(
                    "settled" to true,
                    "settledAt" to System.currentTimeMillis(),
                    "settledBy" to me,
                    "reason" to "${current.reason} · $voidReason"
                )
            )
            true
        } catch (_: PbException) { false }
    }

    /**
     * 工作日断连判定：检查指定日期 [ymd] 是否需要写自动欠条。
     *
     * 规则：
     * - 仅工作日（周一-周五）计入考核
     * - 当日 actualDurationMs 累计 < threshold 分钟 → 写一笔 Debt
     * - 节假日表 backlog（第一期硬编码周一-周五）
     *
     * 防重写：DebtRepo 不存"是否已写"，由调用方（Worker）通过 PreferencesStore.lastWeekdayCheckedYmd 兜底。
     *
     * @return true 表示写了一笔；false 表示不需要写（达标 / 周末 / 没绑搭档）
     */
    suspend fun checkAndWriteWeekdayBreak(ymd: String, sessionRepo: SessionRepo): Boolean {
        val me = prefs.currentUserId.first() ?: return false
        val rel = userRepo.getRelationship() ?: return false
        val partner = if (rel.userAId == me) rel.userBId else rel.userAId

        val date = java.time.LocalDate.parse(ymd)
        val dow = date.dayOfWeek
        if (dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY) return false

        val threshold = prefs.weekdayMinFocusMin.first()
        val actualMinutes = sessionRepo.sessionsForDay(me, ymd)
            .filter { it.statusEnum().name == "COMPLETED" }
            .sumOf { (it.actualDurationMs ?: 0L) / 60_000L }
            .toInt()
        if (actualMinutes >= threshold) return false

        val unit = prefs.ledgerUnitCents.first()
        val reason = "工作日断连 · $ymd · $actualMinutes/$threshold 分钟"
        return record(
            fromUserId = me,           // 我是欠方
            toUserId = partner,
            unitCents = unit,
            count = 1,
            reason = reason
        ) != null
    }
}
