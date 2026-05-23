package com.studybuddy.v2.ui.focus

/**
 * 对端面板派生状态 —— 由 PbRealtime 的 partner status 流推导。
 *
 * 不持有 ViewModel / Coroutine，只是数据快照。FocusViewModel 会订阅 PbRealtime events，
 * 过滤出 partner 的 status 记录，构造 PartnerPaneState 推送到 UI。
 *
 * # 关键判定
 * - **offline = true** 的判定：lastHeartbeat 距今 > 30s（心跳间隔是 5s + 抖动）
 * - **isInLibrary**：直接读 status.isInLibrary（B 系列围栏完成后才会真有值）
 * - **focusStatus** 用字符串而非 enum，因为 PB 推过来的就是字符串
 */
data class PartnerPaneState(
    val nickname: String = "",
    val online: Boolean = false,
    /** "IDLE" / "ACTIVE" / "PAUSED" */
    val focusStatus: String = "IDLE",
    val elapsedSec: Long = 0,
    val isInLibrary: Boolean = false,
    val libraryName: String? = null,
    /** 上次心跳到现在的毫秒数；UI 可用来显示"5s 前"之类副标 */
    val lastSeenAgoMs: Long = 0
) {
    val isPresent: Boolean get() = online && lastSeenAgoMs < 30_000L
    val isFocusing: Boolean get() = isPresent && (focusStatus == "ACTIVE" || focusStatus == "PAUSED")

    companion object {
        val Empty = PartnerPaneState()
    }
}
