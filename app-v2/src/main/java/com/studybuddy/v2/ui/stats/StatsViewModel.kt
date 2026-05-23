package com.studybuddy.v2.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.model.FocusTopic
import com.studybuddy.v2.data.repo.FocusTopicRepo
import com.studybuddy.v2.data.repo.UserRepo
import com.studybuddy.v2.data.room.SessionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class StatsRange(val days: Int, val label: String) {
    WEEK(7, "周"), MONTH(30, "月"), YEAR(365, "年")
}

/** 主题分布的一片：name 用于显示，colorHex 用于色块，minutes 是该范围内累计分钟 */
data class TopicSlice(
    val topicId: String,    // 空字符串 = 未分类
    val name: String,
    val colorHex: String,
    val minutes: Int
)

data class StatsUiState(
    val loading: Boolean = true,
    val range: StatsRange = StatsRange.WEEK,
    /** 按天划分的分钟数（最旧 → 最新），长度 = range.days */
    val dailyMinutes: List<Int> = emptyList(),
    val totalMinutes: Int = 0,
    val streakDays: Int = 0,
    /** 按主题切片（已按 minutes 倒序）。空 list = 没有任何带主题的 session */
    val topicSlices: List<TopicSlice> = emptyList()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val userRepo: UserRepo,
    private val sessionDao: SessionDao,
    private val topicRepo: FocusTopicRepo
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init { load(StatsRange.WEEK) }

    fun setRange(range: StatsRange) {
        _state.update { it.copy(range = range) }
        load(range)
    }

    private fun load(range: StatsRange) {
        viewModelScope.launch {
            val userId = userRepo.currentUserId.first() ?: return@launch
            _state.update { it.copy(loading = true) }
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val startDate = today.minusDays((range.days - 1).toLong())
            val sinceMs = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val untilMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

            val rows = sessionDao.getCompletedSessionsBetween(userId, sinceMs, untilMs)

            // 按"天 index"汇总
            val perDay = LongArray(range.days)
            rows.forEach { r ->
                val dayIdx = ((r.startedAt - sinceMs) / (24 * 3600 * 1000L)).toInt()
                if (dayIdx in 0 until range.days) {
                    perDay[dayIdx] = perDay[dayIdx] + (r.actualDurationMs ?: 0L)
                }
            }
            val daily = perDay.map { (it / 60_000L).toInt() }
            val total = daily.sum()
            var streak = 0
            for (i in daily.indices.reversed()) {
                if (daily[i] > 0) streak++ else break
            }

            // 主题分布
            val topicRows = sessionDao.topicDistribution(userId, sinceMs, untilMs)
            val topics: List<FocusTopic> = runCatching { topicRepo.list(includeArchived = true) }.getOrDefault(emptyList())
            val topicMap = topics.associateBy { it.id }
            val slices = topicRows
                .filter { it.totalMs > 0 }
                .map { r ->
                    val tid = r.topicId.orEmpty()
                    val topic = topicMap[tid]
                    TopicSlice(
                        topicId = tid,
                        name = topic?.name?.takeIf { it.isNotBlank() } ?: if (tid.isBlank()) "未分类" else "已删除",
                        colorHex = topic?.colorHex ?: "#8B8580",
                        minutes = (r.totalMs / 60_000L).toInt()
                    )
                }
                .sortedByDescending { it.minutes }

            _state.update {
                it.copy(
                    loading = false,
                    dailyMinutes = daily,
                    totalMinutes = total,
                    streakDays = streak,
                    topicSlices = slices
                )
            }
        }
    }
}
