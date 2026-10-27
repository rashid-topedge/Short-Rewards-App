package com.dramflix.android.data.api

import com.dramflix.android.data.model.CheckInResult
import com.dramflix.android.data.model.CreateRedemptionRequest
import com.dramflix.android.data.model.DramaDetail
import com.dramflix.android.data.model.DramaListResponse
import com.dramflix.android.data.model.EpisodeListResponse
import com.dramflix.android.data.model.RedemptionCreateResponse
import com.dramflix.android.data.model.RedemptionListResponse
import com.dramflix.android.data.model.ReelItem
import com.dramflix.android.data.model.ReelListResponse
import com.dramflix.android.data.model.RewardResult
import com.dramflix.android.data.model.SystemSettingsDto
import com.dramflix.android.data.model.TransactionsPage
import com.dramflix.android.data.model.UserProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.IOException
import java.net.SocketTimeoutException

interface DramaApi {
    // ── Catalog (token sent when available, not required) ──
    suspend fun getDramas(limit: Int, cursor: String?, query: String?): DramaListResponse
    suspend fun getDrama(dramaId: Int): DramaDetail
    suspend fun getEpisodes(dramaId: Int): EpisodeListResponse
    suspend fun getReels(limit: Int, cursor: String?, dramaId: Int?, sort: String?): ReelListResponse
    suspend fun getReel(reelId: Int): ReelItem
    suspend fun reelAction(reelId: Int, body: ReelActionRequest): ApiActionResult

    // ── Session (no bearer token) ──

    /** First launch: `POST /api/user/sync { requestToken: true, userId? }`. */
    suspend fun startSession(body: UserIdRequest): SessionResponse
    suspend fun refreshTokens(body: RefreshTokenRequest): TokenPair

    /**
     * Exchange a verified Firebase identity for app tokens. Creates the account
     * on first use (id = Firebase UID) and updates googleLinked / googleEmail
     * from the token, so call it again after linking Google.
     */
    suspend fun firebaseSession(body: FirebaseSessionRequest): SessionResponse
    suspend fun loginGoogle(body: GoogleIdTokenRequest): SessionResponse
    suspend fun logout(body: RefreshTokenRequest)

    // ── Account (bearer token) ──
    suspend fun syncUser(body: UserIdRequest): UserProfile
    suspend fun linkGoogle(body: GoogleIdTokenRequest): UserProfile
    suspend fun getTransactions(limit: Int, cursor: String?): TransactionsPage

    /** Claims: `claimId` in the body is also sent as the `Idempotency-Key` header. `lite` skips transactions. */
    suspend fun checkIn(body: UserIdRequest, lite: Boolean = true): CheckInResult
    suspend fun rewardAd(body: RewardAdRequest, lite: Boolean = true): RewardResult
    suspend fun rewardEpisode(body: RewardEpisodeRequest, lite: Boolean = true): RewardResult
    suspend fun rewardReel(body: RewardReelRequest, lite: Boolean = true): RewardResult

    suspend fun listRedemptions(userId: String): RedemptionListResponse
    suspend fun createRedemption(body: CreateRedemptionRequest): RedemptionCreateResponse
    suspend fun getRedemptionSettings(): SystemSettingsDto

    companion object {
        fun getInstance(): DramaApi = NetworkModule.dramaApi
    }
}

class DramaApiImpl(
    private val client: HttpClient,
    private val tokenProvider: () -> AccessTokenProvider?,
) : DramaApi {

    private val json = NetworkModule.json

    // ─── Catalog ────────────────────────────────────────────────

    override suspend fun getDramas(limit: Int, cursor: String?, query: String?): DramaListResponse =
        authed(optional = true) { token ->
            client.get("api/dramas") {
                bearer(token)
                parameter("limit", limit.coerceIn(1, MAX_PAGE_SIZE))
                parameter("cursor", cursor)
                parameter("q", query?.trim()?.takeIf { it.isNotEmpty() })
            }.body()
        }

    override suspend fun getDrama(dramaId: Int): DramaDetail = authed(optional = true) { token ->
        client.get("api/dramas/$dramaId") { bearer(token) }.body()
    }

    override suspend fun getEpisodes(dramaId: Int): EpisodeListResponse = authed(optional = true) { token ->
        client.get("api/dramas/$dramaId/episodes") { bearer(token) }.body()
    }

    override suspend fun getReels(limit: Int, cursor: String?, dramaId: Int?, sort: String?): ReelListResponse =
        authed(optional = true) { token ->
            client.get("api/reels") {
                bearer(token)
                parameter("limit", limit.coerceIn(1, MAX_PAGE_SIZE))
                parameter("cursor", cursor)
                parameter("dramaId", dramaId)
                parameter("sort", sort)
            }.body()
        }

    override suspend fun getReel(reelId: Int): ReelItem = authed(optional = true) { token ->
        client.get("api/reels/$reelId") { bearer(token) }.body()
    }

    override suspend fun reelAction(reelId: Int, body: ReelActionRequest): ApiActionResult = authed { token ->
        client.post("api/reels/$reelId") {
            bearer(token)
            jsonBody(body)
        }.body()
    }

    // ─── Session ────────────────────────────────────────────────

    override suspend fun startSession(body: UserIdRequest): SessionResponse = call {
        val obj: JsonObject = client.post("api/user/sync") {
            jsonBody(body.copy(requestToken = true))
        }.body()
        obj.toSession()
    }

    override suspend fun refreshTokens(body: RefreshTokenRequest): TokenPair = call {
        client.post("api/user/refresh") { jsonBody(body) }.body()
    }

    override suspend fun firebaseSession(body: FirebaseSessionRequest): SessionResponse = call {
        val obj: JsonObject = client.post("api/user/firebase-session") { jsonBody(body) }.body()
        obj.toSession()
    }

    override suspend fun loginGoogle(body: GoogleIdTokenRequest): SessionResponse = call {
        val obj: JsonObject = client.post("api/user/login-google") { jsonBody(body) }.body()
        obj.toSession()
    }

    override suspend fun logout(body: RefreshTokenRequest) {
        call { client.post("api/user/logout") { jsonBody(body) } }
    }

    // ─── Account ────────────────────────────────────────────────

    override suspend fun syncUser(body: UserIdRequest): UserProfile = authed { token ->
        client.post("api/user/sync") {
            bearer(token)
            jsonBody(body)
        }.body()
    }

    override suspend fun linkGoogle(body: GoogleIdTokenRequest): UserProfile = authed { token ->
        client.post("api/user/link-google") {
            bearer(token)
            jsonBody(body)
        }.body()
    }

    override suspend fun getTransactions(limit: Int, cursor: String?): TransactionsPage = authed { token ->
        client.get("api/user/transactions") {
            bearer(token)
            parameter("limit", limit.coerceIn(1, MAX_PAGE_SIZE))
            parameter("cursor", cursor)
        }.body()
    }

    override suspend fun checkIn(body: UserIdRequest, lite: Boolean): CheckInResult = authed { token ->
        client.post("api/user/checkin") {
            bearer(token)
            claim(body.claimId, lite)
            jsonBody(body)
        }.body()
    }

    override suspend fun rewardAd(body: RewardAdRequest, lite: Boolean): RewardResult = authed { token ->
        client.post("api/user/reward/ad") {
            bearer(token)
            claim(body.claimId, lite)
            jsonBody(body)
        }.body()
    }

    override suspend fun rewardEpisode(body: RewardEpisodeRequest, lite: Boolean): RewardResult = authed { token ->
        client.post("api/user/reward/episode") {
            bearer(token)
            claim(body.claimId, lite)
            jsonBody(body)
        }.body()
    }

    override suspend fun rewardReel(body: RewardReelRequest, lite: Boolean): RewardResult = authed { token ->
        client.post("api/user/reward/reel") {
            bearer(token)
            claim(body.claimId, lite)
            jsonBody(body)
        }.body()
    }

    override suspend fun listRedemptions(userId: String): RedemptionListResponse = authed { token ->
        client.get("api/user/redeem") {
            bearer(token)
            parameter("userId", userId.takeIf { it.isNotBlank() })
        }.body()
    }

    override suspend fun createRedemption(body: CreateRedemptionRequest): RedemptionCreateResponse = authed { token ->
        client.post("api/user/redeem") {
            bearer(token)
            jsonBody(body)
        }.body()
    }

    override suspend fun getRedemptionSettings(): SystemSettingsDto = authed(optional = true) { token ->
        client.get("api/user/redeem/settings") { bearer(token) }.body()
    }

    // ─── Plumbing ───────────────────────────────────────────────

    private fun HttpRequestBuilder.bearer(token: String?) {
        if (!token.isNullOrBlank()) header(HttpHeaders.Authorization, "Bearer $token")
    }

    private inline fun <reified T : Any> HttpRequestBuilder.jsonBody(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private fun HttpRequestBuilder.claim(claimId: String?, lite: Boolean) {
        if (!claimId.isNullOrBlank()) header("Idempotency-Key", claimId)
        if (lite) parameter("lite", 1)
    }

    private fun JsonObject.toSession(): SessionResponse = SessionResponse(
        profile = json.decodeFromJsonElement<UserProfile>(this),
        tokens = json.decodeFromJsonElement<TokenPair>(this),
        created = json.decodeFromJsonElement<GoogleAuthFlags>(this).created,
    )

    /**
     * Runs a request with the current access token. If the server rejects the
     * token (401 TOKEN_*), refreshes once and replays the same request – the
     * request builder is re-run, so a claim keeps its claim ID.
     *
     * @param optional the endpoint also works without a session (catalog).
     */
    private suspend fun <T> authed(optional: Boolean = false, block: suspend (token: String?) -> T): T {
        val provider = tokenProvider()
        val token = provider?.currentAccessToken()
        return try {
            call { block(token) }
        } catch (e: ApiException) {
            if (provider == null || !e.isAccessTokenRejected || (optional && token == null)) throw e
            val fresh = provider.refreshAfterRejection(token) ?: throw e
            call { block(fresh) }
        }
    }

    /**
     * Runs one request and normalises every failure into [ApiException], so
     * callers only need `catch (e: ApiException)` and can show `e.message`.
     * Coroutine cancellation is always rethrown untouched.
     */
    private suspend fun <T> call(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            throw e
        } catch (e: ResponseException) {
            val body = runCatching { e.response.bodyAsText() }.getOrNull()
            throw ApiException.fromResponse(
                e.response.status.value,
                body,
                e,
                e.response.headers[HttpHeaders.RetryAfter],
            )
        } catch (e: HttpRequestTimeoutException) {
            throw ApiException.timeout(e)
        } catch (e: SocketTimeoutException) {
            throw ApiException.timeout(e)
        } catch (e: IOException) {
            if (e.javaClass.simpleName.contains("Timeout", ignoreCase = true)) {
                throw ApiException.timeout(e)
            }
            throw ApiException.network(e)
        } catch (e: SerializationException) {
            throw ApiException.parse(e)
        } catch (e: ContentConvertException) {
            throw ApiException.parse(e)
        } catch (e: IllegalArgumentException) {
            // kotlinx.serialization reports some malformed payloads this way.
            throw ApiException.parse(e)
        }
    }

    private companion object {
        const val MAX_PAGE_SIZE = 50
    }
}

/**
 * Body for `/api/user/sync` and `/api/user/checkin`.
 * `userId` is optional now that the bearer token identifies the user; it's
 * still sent for servers running with `USER_AUTH_ALLOW_LEGACY=true`.
 */
@Serializable
data class UserIdRequest(
    val userId: String? = null,
    val clientDate: String? = null,
    /** Only for the first-launch sync: ask the server to issue tokens. */
    val requestToken: Boolean? = null,
    /** Check-in claim ID (also sent as Idempotency-Key). */
    val claimId: String? = null,
)

@Serializable
data class RewardAdRequest(
    val userId: String? = null,
    val adType: String = "REWARDED_VIDEO",
    val clientDate: String? = null,
    val claimId: String? = null,
)

@Serializable
data class RewardEpisodeRequest(
    val userId: String? = null,
    val dramaId: Int,
    val ep: Int,
    val clientDate: String? = null,
    val claimId: String? = null,
)

@Serializable
data class RewardReelRequest(
    val userId: String? = null,
    val reelId: Int,
    val clientDate: String? = null,
    val claimId: String? = null,
)

@Serializable
data class ReelActionRequest(
    val action: String,
)

@Serializable
data class ApiActionResult(
    val success: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)
