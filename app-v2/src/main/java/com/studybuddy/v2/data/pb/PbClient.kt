package com.studybuddy.v2.data.pb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * PocketBase REST 客户端。Token 由调用方在每次请求前通过 [setAuthToken] 注入。
 *
 * 不持有 Context，不绑定 Hilt，方便单元测试。Hilt 注入逻辑放在 DI 层。
 */
@Singleton
class PbClient @Inject constructor() {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        isLenient = true
        explicitNulls = false
    }

    @Volatile private var authToken: String? = null

    fun setAuthToken(token: String?) { authToken = token }
    fun currentToken(): String? = authToken

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // ─── Auth ────────────────────────────────────────────────────────────────

    /**
     * users/auth-with-password
     * 注意：PocketBase 0.22+ 走 `/api/collections/users/auth-with-password`
     */
    suspend fun authWithPassword(identity: String, password: String): PbAuthResponse {
        val body = buildJsonObject {
            put("identity", identity)
            put("password", password)
        }
        val resp = postRaw("api/collections/${PbConfig.USERS}/auth-with-password", body, requireAuth = false)
        return decodeOrThrow(resp, PbAuthResponse.serializer())
    }

    /**
     * 创建用户。PocketBase auth collection 要求字段：email/password/passwordConfirm。
     */
    suspend fun createUser(
        email: String,
        password: String,
        nickname: String
    ): PbUserRecord {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
            put("passwordConfirm", password)
            put("emailVisibility", true)
            put("name", nickname)
            put("nickname", nickname)
        }
        val resp = postRaw("api/collections/${PbConfig.USERS}/records", body, requireAuth = false)
        return decodeOrThrow(resp, PbUserRecord.serializer())
    }

    /**
     * 刷新当前 token（需要已有 token）。
     */
    suspend fun authRefresh(): PbAuthResponse {
        val resp = postRaw(
            "api/collections/${PbConfig.USERS}/auth-refresh",
            JsonObject(emptyMap()),
            requireAuth = true
        )
        return decodeOrThrow(resp, PbAuthResponse.serializer())
    }

    // ─── Generic CRUD ────────────────────────────────────────────────────────

    suspend inline fun <reified T> getRecord(collection: String, id: String): T {
        val resp = withContext(Dispatchers.IO) { execute(buildGet("api/collections/$collection/records/$id")) }
        return decodeOrThrow(resp, json.serializersModule.serializer())
    }

    suspend inline fun <reified T> listRecords(
        collection: String,
        filter: String? = null,
        sort: String? = null,
        page: Int = 1,
        perPage: Int = 50
    ): PbListResponse<T> {
        val urlBuilder = (PbConfig.BASE_URL + "api/collections/$collection/records").toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("perPage", perPage.toString())
        if (!filter.isNullOrBlank()) urlBuilder.addQueryParameter("filter", filter)
        if (!sort.isNullOrBlank()) urlBuilder.addQueryParameter("sort", sort)

        val req = Request.Builder()
            .url(urlBuilder.build())
            .header("Accept", "application/json")
            .applyAuth()
            .build()
        val resp = withContext(Dispatchers.IO) { execute(req) }
        return decodeOrThrow(resp, PbListResponse.serializer(json.serializersModule.serializer<T>()))
    }

    suspend inline fun <reified T> createRecord(collection: String, fields: Map<String, Any?>): T {
        val body = buildJsonObjectFromMap(fields)
        val resp = postJsonRaw("api/collections/$collection/records", body)
        return decodeOrThrow(resp, json.serializersModule.serializer())
    }

    /**
     * 创建带文件附件的记录（multipart/form-data）。
     *
     * @param fields 普通字段
     * @param files map<fieldName, list<Pair<filename, bytes>>>
     *              注意：PB file 字段如果是 multi，需要多个同名 part；single 则只放一个
     */
    suspend inline fun <reified T> createRecordWithFiles(
        collection: String,
        fields: Map<String, Any?>,
        files: Map<String, List<Pair<String, ByteArray>>>
    ): T {
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        // 普通字段
        for ((k, v) in fields) {
            if (v == null) continue
            builder.addFormDataPart(k, v.toString())
        }
        // 文件
        for ((field, fileList) in files) {
            for ((filename, bytes) in fileList) {
                val mime = when {
                    filename.endsWith(".png", ignoreCase = true) -> "image/png"
                    filename.endsWith(".webp", ignoreCase = true) -> "image/webp"
                    else -> "image/jpeg"
                }
                builder.addFormDataPart(
                    field, filename,
                    bytes.toRequestBody(mime.toMediaType())
                )
            }
        }
        val req = Request.Builder()
            .url(PbConfig.BASE_URL + "api/collections/$collection/records")
            .post(builder.build())
            .applyAuth()
            .build()
        val resp = withContext(Dispatchers.IO) { execute(req) }
        return decodeOrThrow(resp, json.serializersModule.serializer())
    }

    suspend inline fun <reified T> updateRecord(collection: String, id: String, fields: Map<String, Any?>): T {
        val body = buildJsonObjectFromMap(fields)
        val resp = patchJsonRaw("api/collections/$collection/records/$id", body)
        return decodeOrThrow(resp, json.serializersModule.serializer())
    }

    suspend fun deleteRecord(collection: String, id: String) {
        val req = Request.Builder()
            .url(PbConfig.BASE_URL + "api/collections/$collection/records/$id")
            .delete()
            .applyAuth()
            .build()
        val resp = withContext(Dispatchers.IO) { execute(req) }
        if (!resp.isSuccessful) throw parseError(resp)
        resp.close()
    }

    // ─── Internal helpers ────────────────────────────────────────────────────

    @PublishedApi
    internal fun buildGet(path: String): Request = Request.Builder()
        .url(PbConfig.BASE_URL + path)
        .get()
        .header("Accept", "application/json")
        .applyAuth()
        .build()

    /**
     * 内部 raw POST。inline 函数也能调（@PublishedApi）。
     */
    @PublishedApi
    internal suspend fun postJsonRaw(path: String, body: JsonObject): Response =
        postRaw(path, body, requireAuth = true)

    @PublishedApi
    internal suspend fun patchJsonRaw(path: String, body: JsonObject): Response {
        val req = Request.Builder()
            .url(PbConfig.BASE_URL + path)
            .patch(json.encodeToString(JsonObject.serializer(), body).toRequestBody(jsonMediaType))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .applyAuth()
            .build()
        return withContext(Dispatchers.IO) { execute(req) }
    }

    private suspend fun postRaw(path: String, body: JsonObject, requireAuth: Boolean): Response {
        val req = Request.Builder()
            .url(PbConfig.BASE_URL + path)
            .post(json.encodeToString(JsonObject.serializer(), body).toRequestBody(jsonMediaType))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .apply { if (requireAuth) applyAuth() else { /* still try if token exists */ applyAuth() } }
            .build()
        return withContext(Dispatchers.IO) { execute(req) }
    }

    @PublishedApi
    internal fun Request.Builder.applyAuth(): Request.Builder = apply {
        authToken?.let { header("Authorization", it) }
    }

    @PublishedApi
    internal suspend fun execute(req: Request): Response =
        suspendCancellableCoroutine { cont ->
            val call = client.newCall(req)
            cont.invokeOnCancellation { runCatching { call.cancel() } }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { cont.resumeWithException(e) }
                override fun onResponse(call: Call, response: Response) { cont.resume(response) }
            })
        }

    @PublishedApi
    internal fun <T> decodeOrThrow(resp: Response, deserializer: kotlinx.serialization.DeserializationStrategy<T>): T {
        val text = resp.body?.string().orEmpty()
        resp.close()
        if (!resp.isSuccessful) throw parseErrorBody(resp.code, text)
        return try {
            json.decodeFromString(deserializer, text)
        } catch (e: SerializationException) {
            throw PbException(resp.code, -1, "解析响应失败: ${e.message}", text)
        }
    }

    private fun parseError(resp: Response): PbException {
        val text = resp.body?.string().orEmpty()
        resp.close()
        return parseErrorBody(resp.code, text)
    }

    private fun parseErrorBody(status: Int, text: String): PbException {
        return try {
            val err = json.decodeFromString(PbErrorBody.serializer(), text)
            PbException(status, err.code, err.message.ifBlank { "请求失败 ($status)" }, text)
        } catch (_: Exception) {
            PbException(status, -1, "请求失败 ($status)", text)
        }
    }

    @PublishedApi
    internal fun buildJsonObjectFromMap(fields: Map<String, Any?>): JsonObject = buildJsonObject {
        fields.forEach { (k, v) ->
            when (v) {
                null -> put(k, kotlinx.serialization.json.JsonNull)
                is String -> put(k, v)
                is Number -> put(k, v)
                is Boolean -> put(k, v)
                is JsonObject -> put(k, v)
                else -> put(k, v.toString())
            }
        }
    }
}
