package com.studybuddy.v2.ui.focus

/**
 * 同步专注状态机 —— 纯函数 reducer，无副作用。
 *
 * 7 态有限状态机：
 *
 *   IDLE         （初始）
 *     ↓ Invite
 *   INVITING     （我发出邀请，等对方应答）
 *     ↓ Accepted ↘ Declined/Timeout → IDLE
 *   CONFIRMING   （双方都已就绪，准备进入 3-2-1）
 *     ↓ Tick(3s)
 *   COUNTDOWN    （3-2-1 全屏倒数）
 *     ↓ CountdownEnd
 *   ACTIVE       （正在专注；双方 startedAt = invitedAcceptedAt + 3000）
 *     ↓ MyPause/PartnerPause
 *   PAUSED       （任一方暂停 → 双方都暂停；提示对方）
 *     ↓ MyResume/PartnerResume
 *   ACTIVE
 *     ↓ MyComplete/PartnerComplete/CountdownReached
 *   COMPLETED    （结算 + 庆祝）
 *     ↓ MyAbort/PartnerAbort/PartnerOffline
 *   ABORTED      （任一方放弃 → 双方都终止；流水标记原因）
 *
 * # 设计哲学
 * - 状态机里**不持有 Coroutine、不发网络请求**。只接收 Event 输出 State。
 * - 副作用（发邀请、写 PB、启动计时）都在 FocusViewModel 中根据 State 变化触发
 * - PARTNER_* 事件由 PbRealtime 的 partner status 流派生（见 [PartnerPaneState]）
 */
object SyncFocusStateMachine {

    enum class State { IDLE, INVITING, CONFIRMING, COUNTDOWN, ACTIVE, PAUSED, COMPLETED, ABORTED }

    sealed class Event {
        // 我方动作
        object MyInvite : Event()
        object MyCancelInvite : Event()
        object MyPause : Event()
        object MyResume : Event()
        object MyComplete : Event()
        object MyAbort : Event()

        // 对端事件（由 PbRealtime partner status 流派生）
        object PartnerAccepted : Event()
        object PartnerDeclined : Event()
        object PartnerPaused : Event()
        object PartnerResumed : Event()
        object PartnerCompleted : Event()
        object PartnerAborted : Event()
        object PartnerOffline : Event()  // partner 心跳超时 → 视为 abort

        // 定时事件
        object InviteTimeout : Event()    // 60s 无响应
        object ConfirmTick : Event()      // CONFIRMING → COUNTDOWN（3s 准备结束）
        object CountdownEnd : Event()     // 3-2-1 结束 → ACTIVE
        object CountdownReached : Event() // 计划时长达到 → COMPLETED（自动）
    }

    fun reduce(state: State, event: Event): State = when (state) {
        State.IDLE -> when (event) {
            Event.MyInvite -> State.INVITING
            else -> state
        }
        State.INVITING -> when (event) {
            Event.MyCancelInvite -> State.IDLE
            Event.PartnerAccepted -> State.CONFIRMING
            Event.PartnerDeclined -> State.IDLE
            Event.InviteTimeout -> State.IDLE
            else -> state
        }
        State.CONFIRMING -> when (event) {
            Event.ConfirmTick -> State.COUNTDOWN
            Event.PartnerAborted, Event.PartnerOffline, Event.MyAbort -> State.ABORTED
            else -> state
        }
        State.COUNTDOWN -> when (event) {
            Event.CountdownEnd -> State.ACTIVE
            Event.MyAbort, Event.PartnerAborted, Event.PartnerOffline -> State.ABORTED
            else -> state
        }
        State.ACTIVE -> when (event) {
            Event.MyPause, Event.PartnerPaused -> State.PAUSED
            Event.MyComplete, Event.PartnerCompleted, Event.CountdownReached -> State.COMPLETED
            Event.MyAbort, Event.PartnerAborted, Event.PartnerOffline -> State.ABORTED
            else -> state
        }
        State.PAUSED -> when (event) {
            Event.MyResume, Event.PartnerResumed -> State.ACTIVE
            Event.MyAbort, Event.PartnerAborted, Event.PartnerOffline -> State.ABORTED
            Event.MyComplete, Event.PartnerCompleted -> State.COMPLETED
            else -> state
        }
        State.COMPLETED, State.ABORTED -> state  // 终态
    }

    fun isTerminal(state: State): Boolean = state == State.COMPLETED || state == State.ABORTED

    fun isRunning(state: State): Boolean = state in setOf(
        State.CONFIRMING, State.COUNTDOWN, State.ACTIVE, State.PAUSED
    )
}
