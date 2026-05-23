package com.studybuddy.v2.data.pb

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * PocketBase 鉴权响应。
 */
@Serializable
data class PbAuthResponse(
    val token: String,
    val record: PbUserRecord
)

@Serializable
data class PbUserRecord(
    val id: String,
    val email: String = "",
    val username: String = "",
    val name: String = "",
    // 自定义业务字段（schema 中可能存在或不存在）
    val nickname: String = "",
    val avatarUrl: String? = null,
    val partnerId: String? = null,
    val partnerSince: Long? = null
)

@Serializable
data class PbListResponse<T>(
    val page: Int = 1,
    val perPage: Int = 30,
    val totalItems: Int = 0,
    val totalPages: Int = 0,
    val items: List<T> = emptyList()
)

@Serializable
data class PbErrorBody(
    val code: Int = 0,
    val message: String = "",
    val data: JsonElement? = null
)

class PbException(
    val status: Int,
    val code: Int,
    val pbMessage: String,
    val raw: String? = null
) : RuntimeException("PB[$status/$code]: $pbMessage")
