package com.studybuddy.v2.data.repo

import com.studybuddy.v2.data.model.FundTransaction
import com.studybuddy.v2.data.model.SharedFund
import com.studybuddy.v2.data.model.WishItem
import com.studybuddy.v2.data.model.statusEnum
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.pb.PbConfig
import com.studybuddy.v2.data.pb.PbException
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 共同金库 —— 账本概念合并。
 *
 * 设计哲学：双方共有的钱包，记录三类流水：
 *   - DEPOSIT 主动充值
 *   - PENALTY 工作日断连自动入金
 *   - WITHDRAWAL 用于实现愿望
 *
 * 余额 = sum(DEPOSIT + PENALTY) - sum(WITHDRAWAL)，剔除 voided=true 的记录。
 */
@Singleton
class FundRepo @Inject constructor(
    private val pb: PbClient,
    private val prefs: PreferencesStore,
    private val userRepo: UserRepo,
    private val momentBus: com.studybuddy.v2.data.moment.MomentBus
) {
    suspend fun getByPair(pairId: String): SharedFund? {
        return try {
            val list = pb.listRecords<SharedFund>(
                collection = PbConfig.FUNDS,
                filter = "pairId='$pairId'",
                perPage = 1
            )
            list.items.firstOrNull()
        } catch (_: PbException) { null }
    }

    suspend fun listTransactions(): List<FundTransaction> {
        val rel = userRepo.getRelationship() ?: return emptyList()
        return try {
            pb.listRecords<FundTransaction>(
                collection = PbConfig.FUND_TRANSACTIONS,
                filter = "pairId='${rel.id}'",
                sort = "-at",
                perPage = 200
            ).items
        } catch (_: PbException) { emptyList() }
    }

    suspend fun penalize(amountCents: Long, note: String): FundTransaction? {
        val me = prefs.currentUserId.first() ?: return null
        val rel = userRepo.getRelationship() ?: return null
        val record = createTransaction(rel.id, me, "PENALTY", amountCents, note)
        if (record != null) bumpBalance(rel.id, amountCents)
        return record
    }

    suspend fun deposit(amountCents: Long, note: String): FundTransaction? {
        val me = prefs.currentUserId.first() ?: return null
        val rel = userRepo.getRelationship() ?: return null
        val record = createTransaction(rel.id, me, "DEPOSIT", amountCents, note)
        if (record != null) bumpBalance(rel.id, amountCents)
        return record
    }

    suspend fun withdraw(amountCents: Long, note: String): FundTransaction? {
        val me = prefs.currentUserId.first() ?: return null
        val rel = userRepo.getRelationship() ?: return null
        val record = createTransaction(rel.id, me, "WITHDRAWAL", amountCents, note)
        if (record != null) bumpBalance(rel.id, -amountCents)
        return record
    }

    suspend fun voidTransaction(txId: String): Boolean {
        val me = prefs.currentUserId.first() ?: return false
        return try {
            val tx = pb.getRecord<FundTransaction>(PbConfig.FUND_TRANSACTIONS, txId)
            if (tx.voided) return false
            pb.updateRecord<FundTransaction>(
                collection = PbConfig.FUND_TRANSACTIONS,
                id = txId,
                fields = mapOf(
                    "voided" to true,
                    "voidedAt" to System.currentTimeMillis(),
                    "voidedBy" to me
                )
            )
            // 反向冲减余额
            val signedDelta = when (tx.type) {
                "WITHDRAWAL" -> tx.amountCents
                else -> -tx.amountCents
            }
            bumpBalance(tx.pairId, signedDelta)
            true
        } catch (_: PbException) { false }
    }

    suspend fun deleteTransaction(tx: FundTransaction): Boolean {
        return try {
            pb.deleteRecord(PbConfig.FUND_TRANSACTIONS, tx.id)
            if (!tx.voided) {
                val signedDelta = when (tx.type) {
                    "WITHDRAWAL" -> tx.amountCents
                    else -> -tx.amountCents
                }
                bumpBalance(tx.pairId, signedDelta)
            }
            true
        } catch (_: PbException) { false }
    }

    /**
     * 工作日断连判定 —— Worker 调用。未达标 → PENALTY 入金。
     * 防重写：调用方维护 lastWeekdayCheckedYmd。
     */
    suspend fun checkAndPenalizeWeekdayBreak(
        ymd: String,
        sessionRepo: SessionRepo
    ): Boolean {
        val me = prefs.currentUserId.first() ?: return false
        userRepo.getRelationship() ?: return false

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
        val note = "工作日断连 · $ymd · $actualMinutes/$threshold 分钟"
        val record = penalize(amountCents = unit.toLong(), note = note)
        // 触发情境事件：用户下次打开 Home 看到 banner
        if (record != null) {
            momentBus.emit(com.studybuddy.v2.data.moment.Moment.WeekdayBreakNoticed(
                id = "weekday_${ymd}_${System.currentTimeMillis()}",
                ymd = ymd,
                actualMin = actualMinutes,
                threshold = threshold,
                penalizedYuan = (unit / 100).coerceAtLeast(1)
            ))
        }
        return record != null
    }

    private suspend fun createTransaction(
        pairId: String,
        byUserId: String,
        type: String,
        amountCents: Long,
        note: String
    ): FundTransaction? {
        val now = System.currentTimeMillis()
        return try {
            pb.createRecord<FundTransaction>(PbConfig.FUND_TRANSACTIONS, mapOf(
                "pairId" to pairId,
                "byUserId" to byUserId,
                "type" to type,
                "amountCents" to amountCents,
                "note" to note,
                "at" to now,
                "voided" to false
            ))
        } catch (_: PbException) { null }
    }

    private suspend fun bumpBalance(pairId: String, deltaCents: Long) {
        try {
            val current = getByPair(pairId)
            if (current == null) {
                pb.createRecord<SharedFund>(PbConfig.FUNDS, mapOf(
                    "pairId" to pairId,
                    "balanceCents" to deltaCents.coerceAtLeast(0),
                    "totalInCents" to deltaCents.coerceAtLeast(0),
                    "totalOutCents" to 0L
                ))
            } else {
                val newBalance = (current.balanceCents + deltaCents).coerceAtLeast(0)
                val totalIn = if (deltaCents > 0) current.totalInCents + deltaCents else current.totalInCents
                val totalOut = if (deltaCents < 0) current.totalOutCents - deltaCents else current.totalOutCents
                pb.updateRecord<SharedFund>(PbConfig.FUNDS, current.id, mapOf(
                    "balanceCents" to newBalance,
                    "totalInCents" to totalIn,
                    "totalOutCents" to totalOut
                ))
            }
        } catch (_: Exception) {}
    }
}
