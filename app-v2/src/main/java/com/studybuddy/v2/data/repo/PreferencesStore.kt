package com.studybuddy.v2.data.repo

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "v2_prefs")

/**
 * 全局偏好存储。Token / 用户 ID / 主题 / 专注偏好等。
 */
@Singleton
class PreferencesStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val ds = ctx.dataStore

    val pbToken: Flow<String?> = ds.data.map { it[KEY_PB_TOKEN] }
    val currentUserId: Flow<String?> = ds.data.map { it[KEY_USER_ID] }
    val userEmail: Flow<String?> = ds.data.map { it[KEY_USER_EMAIL] }
    val userNickname: Flow<String?> = ds.data.map { it[KEY_USER_NICKNAME] }
    val darkTheme: Flow<Boolean> = ds.data.map { it[KEY_DARK_THEME] ?: false }
    val focusDurationMin: Flow<Int> = ds.data.map { it[KEY_FOCUS_DURATION] ?: 25 }
    /** "COUNTDOWN" / "STOPWATCH"；默认 COUNTDOWN。 */
    val focusMode: Flow<String> = ds.data.map { it[KEY_FOCUS_MODE] ?: "COUNTDOWN" }
    /** "WAVE"（双色正弦波，默认） / "TYPOGRAPHY"（字号即数据） */
    val partnerWidgetStyle: Flow<String> = ds.data.map { it[KEY_PARTNER_WIDGET] ?: "WAVE" }
    /** 鞍部猫彩蛋是否解锁。Settings 长按 About 5 次 + 输入 cc-pet-ahoy 触发。 */
    val unlockedSaddleCat: Flow<Boolean> = ds.data.map { it[KEY_UNLOCK_SADDLE] ?: false }
    /** MascotDock（右下角常驻吉祥物）总开关。默认 true，仅解锁后才有视觉。 */
    val mascotDockEnabled: Flow<Boolean> = ds.data.map { it[KEY_MASCOT_DOCK_ENABLED] ?: true }
    /** 长按折叠的恢复时间戳（System.currentTimeMillis）。0 表示未折叠。 */
    val mascotDockCollapsedUntil: Flow<Long> = ds.data.map { it[KEY_MASCOT_COLLAPSED_UNTIL] ?: 0L }
    /** 账本每份金额（分）。默认 ¥5 = 500 分。 */
    val ledgerUnitCents: Flow<Int> = ds.data.map { it[KEY_LEDGER_UNIT_CENTS] ?: 500 }
    /** 工作日每日最低专注分钟数，未达标算"断连"。默认 25 分钟。 */
    val weekdayMinFocusMin: Flow<Int> = ds.data.map { it[KEY_WEEKDAY_MIN_MIN] ?: 25 }
    /** 工作日断连开关。默认开启；未绑搭档时本来也写不进 Debt，所以可以开着。 */
    val weekdayBreakEnabled: Flow<Boolean> = ds.data.map { it[KEY_WEEKDAY_BREAK_ON] ?: true }
    /** 上次断连判定到的 ymd（"yyyy-MM-dd"），避免重复扣同一天。 */
    val lastWeekdayCheckedYmd: Flow<String?> = ds.data.map { it[KEY_LAST_WEEKDAY_YMD] }
    /** 上次选的专注主题 id（下次默认）；null 表示不绑主题。 */
    val lastSelectedTopicId: Flow<String?> = ds.data.map { it[KEY_LAST_TOPIC_ID] }
    /** Moment 抑制状态 JSON：`{type: lastShownEpochMs}`，用于 24h 内不重复同种 banner。 */
    val momentSuppressionJson: Flow<String> = ds.data.map { it[KEY_MOMENT_SUPPRESSION] ?: "{}" }
    /** 用户禁用的 Moment type（逗号分隔），例如 "meeting_started,weekday_break_noticed" */
    val momentDisabledTypes: Flow<String> = ds.data.map { it[KEY_MOMENT_DISABLED] ?: "" }
    /** App 模式 "FOCUS"（默认，专注向）/ "LEISURE"（娱乐向，宠物不衰减、专注 banner 静默） */
    val appMode: Flow<String> = ds.data.map { it[KEY_APP_MODE] ?: "FOCUS" }

    suspend fun setPbToken(token: String?) = ds.edit {
        if (token == null) it.remove(KEY_PB_TOKEN) else it[KEY_PB_TOKEN] = token
    }
    suspend fun setCurrentUserId(id: String?) = ds.edit {
        if (id == null) it.remove(KEY_USER_ID) else it[KEY_USER_ID] = id
    }
    suspend fun setUserEmail(email: String?) = ds.edit {
        if (email == null) it.remove(KEY_USER_EMAIL) else it[KEY_USER_EMAIL] = email
    }
    suspend fun setUserNickname(nickname: String?) = ds.edit {
        if (nickname == null) it.remove(KEY_USER_NICKNAME) else it[KEY_USER_NICKNAME] = nickname
    }
    suspend fun setDarkTheme(on: Boolean) = ds.edit { it[KEY_DARK_THEME] = on }
    suspend fun setFocusDurationMin(min: Int) = ds.edit { it[KEY_FOCUS_DURATION] = min }
    suspend fun setFocusMode(mode: String) = ds.edit { it[KEY_FOCUS_MODE] = mode }
    suspend fun setPartnerWidgetStyle(style: String) = ds.edit { it[KEY_PARTNER_WIDGET] = style }
    suspend fun setUnlockedSaddleCat(on: Boolean) = ds.edit { it[KEY_UNLOCK_SADDLE] = on }
    suspend fun setMascotDockEnabled(on: Boolean) = ds.edit { it[KEY_MASCOT_DOCK_ENABLED] = on }
    suspend fun setMascotDockCollapsedUntil(ts: Long) = ds.edit { it[KEY_MASCOT_COLLAPSED_UNTIL] = ts }
    suspend fun setLedgerUnitCents(cents: Int) = ds.edit { it[KEY_LEDGER_UNIT_CENTS] = cents }
    suspend fun setWeekdayMinFocusMin(min: Int) = ds.edit { it[KEY_WEEKDAY_MIN_MIN] = min }
    suspend fun setWeekdayBreakEnabled(on: Boolean) = ds.edit { it[KEY_WEEKDAY_BREAK_ON] = on }
    suspend fun setLastWeekdayCheckedYmd(ymd: String) = ds.edit { it[KEY_LAST_WEEKDAY_YMD] = ymd }
    suspend fun setLastSelectedTopicId(id: String?) = ds.edit {
        if (id == null) it.remove(KEY_LAST_TOPIC_ID) else it[KEY_LAST_TOPIC_ID] = id
    }
    suspend fun setMomentSuppressionJson(json: String) = ds.edit { it[KEY_MOMENT_SUPPRESSION] = json }
    suspend fun setMomentDisabledTypes(csv: String) = ds.edit { it[KEY_MOMENT_DISABLED] = csv }
    suspend fun setAppMode(mode: String) = ds.edit { it[KEY_APP_MODE] = mode }

    suspend fun clearAll() = ds.edit { it.clear() }

    private companion object {
        val KEY_PB_TOKEN = stringPreferencesKey("pb_token")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_USER_NICKNAME = stringPreferencesKey("user_nickname")
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEY_FOCUS_DURATION = intPreferencesKey("focus_duration")
        val KEY_FOCUS_MODE = stringPreferencesKey("focus_mode")
        val KEY_PARTNER_WIDGET = stringPreferencesKey("partner_widget_style")
        val KEY_UNLOCK_SADDLE = booleanPreferencesKey("unlock_saddle_cat")
        val KEY_MASCOT_DOCK_ENABLED = booleanPreferencesKey("mascot_dock_enabled")
        val KEY_MASCOT_COLLAPSED_UNTIL = longPreferencesKey("mascot_collapsed_until")
        val KEY_LEDGER_UNIT_CENTS = intPreferencesKey("ledger_unit_cents")
        val KEY_WEEKDAY_MIN_MIN = intPreferencesKey("weekday_min_min")
        val KEY_WEEKDAY_BREAK_ON = booleanPreferencesKey("weekday_break_on")
        val KEY_LAST_WEEKDAY_YMD = stringPreferencesKey("last_weekday_ymd")
        val KEY_LAST_TOPIC_ID = stringPreferencesKey("last_topic_id")
        val KEY_MOMENT_SUPPRESSION = stringPreferencesKey("moment_suppression")
        val KEY_MOMENT_DISABLED = stringPreferencesKey("moment_disabled_types")
        val KEY_APP_MODE = stringPreferencesKey("app_mode")
    }

    // Required because Hilt likes to see Preferences typealias resolvable
    @Suppress("unused")
    private val _t: Preferences? = null
}
