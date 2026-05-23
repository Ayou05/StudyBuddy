package com.studybuddy.v2.data.pb

object PbConfig {
    const val BASE_URL = "https://catclaw.cloud/"

    // PocketBase collections（沿用 :app 现有 schema）
    const val USERS = "users"
    const val RELATIONSHIPS = "relationships"
    const val STATUS = "status"
    const val SESSIONS = "sessions"
    const val MESSAGES = "messages"
    const val PETS = "pets"
    const val FUNDS = "funds"
    const val CHECKINS = "checkins"
    const val LANDMARKS = "landmarks"

    // P4 新增 collections（手动在 PocketBase Console 创建 schema 后启用）
    const val SYNC_INVITES = "sync_invites"
    const val FUND_TRANSACTIONS = "fund_transactions"
    const val UNBIND_REQUESTS = "unbind_requests"
    const val QUOTES = "quotes"
    const val DEBTS = "debts"
    const val LETTERS = "letters"   // P5 信件 + 飞机双载体
    const val NOTES = "notes"       // P5 便签墙（图 + 文）
    const val MEETINGS = "meetings" // P5 见面记录（异地党核心）
    const val PAIR_LANDMARKS = "pair_landmarks"  // P5 共享地标（你们一起去过的地方）
    const val FOCUS_TOPICS = "focus_topics"      // P5 多主题专注（鞍部风铭牌）
}
