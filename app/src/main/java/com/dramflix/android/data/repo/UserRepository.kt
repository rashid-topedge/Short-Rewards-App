package com.dramflix.android.data.repo

import android.app.Application
import android.content.Context
import androidx.room.withTransaction
import com.dramflix.android.data.AppContainer
import com.dramflix.android.data.api.ApiException
import com.dramflix.android.data.api.DramaApi
import com.dramflix.android.data.api.NetworkModule
import com.dramflix.android.data.api.RewardAdRequest
import com.dramflix.android.data.api.RewardEpisodeRequest
import com.dramflix.android.data.api.RewardReelRequest
import com.dramflix.android.data.api.UserIdRequest
import com.dramflix.android.data.local.AppDatabase
import com.dramflix.android.data.local.entity.PendingClaimEntity
import com.dramflix.android.data.local.toDomain
import com.dramflix.android.data.local.toEntity
import com.dramflix.android.data.model.CheckInResult
import com.dramflix.android.data.model.CreateRedemptionRequest
import com.dramflix.android.data.model.RedemptionCreateResponse
import com.dramflix.android.data.model.RedemptionRequest
import com.dramflix.android.data.model.RewardResult
import com.dramflix.android.data.model.SystemSettingsDto
import com.dramflix.android.data.model.TransactionsPage
import com.dramflix.android.data.model.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * User repository – profile, rewards, redemptions. **Offline-first**: the
 * profile is cached in Room and mirrored in [userProfile].
 *
 * Identity comes from the bearer token (see AuthManager); this class never
 * invents a user id any more.
 *
 * **Claims are duplicate-safe** (check-in, ad, episode, reel):
 * - every claim gets a random claim ID, sent as `Idempotency-Key` + `claimId`;
 * - the claim is saved in `pending_claims` *before* the request, and retried
 *   later with the **same** ID if the outcome is unknown (timeout, offline,
 *   5xx, app killed) – the server then replays the original answer;
 * - `409 ALREADY_CLAIMED` is a success (already paid, maybe on another device);
 * - responses use `?lite=1`, so they don't carry transactions; the full list
 *   comes with the next [sync] (e.g. when the rewards dialog opens).
 */
class UserRepository private constructor(
    private val dramaApi: DramaApi,
    context: Context,
    private val db: AppDatabase,
) {
    private val userDao = db.userDao()
    private val pendingDao = db.pendingClaimDao()
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    /** Observe profile directly from Room. */
    val userProfileFlow: Flow<UserProfile?> = userDao.observe().map { it?.toDomain() }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _settings = MutableStateFlow<SystemSettingsDto?>(null)
    val settings: StateFlow<SystemSettingsDto?> = _settings.asStateFlow()

    private val syncMutex = Mutex()
    private val mutationMutex = Mutex()
    private val settingsMutex = Mutex()

    @Volatile
    private var lastSyncAt = 0L

    @Volatile
    private var cachedUserId: String? = null

    // ─── Identity & first-launch bookkeeping ────────────────────

    /** Populate [userProfile] from Room on cold start (never overwrites a fresher profile). */
    suspend fun restoreCachedProfile() {
        val cached = userDao.get()?.toDomain() ?: return
        _userProfile.compareAndSet(null, cached)
    }

    /** The server-assigned user id of the current session, or "" before the first session. */
    fun getUserId(): String {
        cachedUserId?.let { return it }
        val persisted = prefs.getString(KEY_USER_ID, null)?.trim().orEmpty()
        if (persisted.isNotEmpty()) cachedUserId = persisted
        return persisted
    }

    fun persistUserId(userId: String) {
        cachedUserId = userId
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    /**
     * The user id from a pre-auth version of the app, to be sent once with the
     * first token request so the existing account (and its coins) carries over.
     */
    fun legacyUserIdForMigration(): String? {
        if (prefs.getBoolean(KEY_LEGACY_MIGRATED, false)) return null
        return prefs.getString(KEY_USER_ID, null)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun markLegacyMigrated() {
        prefs.edit().putBoolean(KEY_LEGACY_MIGRATED, true).apply()
    }

    /** True once the user has chosen guest or Google on the welcome screen (or migrated). */
    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()
        }

    /** Called by AuthManager when a session starts: adopt the server's profile and id. */
    suspend fun onSessionStarted(profile: UserProfile, isDifferentAccount: Boolean) {
        if (isDifferentAccount) clearAccountData()
        markLegacyMigrated()
        hasCompletedOnboarding = true
        applyProfile(profile)
    }

    /** Called by AuthManager after link-google: same account, now backed up. */
    suspend fun onProfileUpdated(profile: UserProfile) = applyProfile(profile)

    /**
     * Forget everything that belongs to the current account: cached profile,
     * unsent claims (they would otherwise be paid to the next account) and likes.
     */
    suspend fun clearAccountData() {
        db.withTransaction {
            userDao.clear()
            pendingDao.clear()
            db.reelDao().clearLikes()
        }
        _userProfile.value = null
        lastSyncAt = 0L
    }

    // ─── Profile ────────────────────────────────────────────────

    /**
     * Pull the latest profile (including recent transactions).
     * Throttled to once per [MIN_SYNC_INTERVAL_MS] unless [force]; single-flight.
     * Never throws; on failure the cached profile stays in place.
     */
    suspend fun sync(force: Boolean = false) {
        if (!force && System.currentTimeMillis() - lastSyncAt < MIN_SYNC_INTERVAL_MS) return
        if (!syncMutex.tryLock()) return
        _isSyncing.value = true
        try {
            val updated = dramaApi.syncUser(UserIdRequest(getUserId().ifBlank { null }, todayIso()))
            applyProfile(updated)
            lastSyncAt = System.currentTimeMillis()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            if (_userProfile.value == null) {
                _userProfile.value = userDao.get()?.toDomain()
            }
        } finally {
            _isSyncing.value = false
            syncMutex.unlock()
        }
    }

    /** One page of coin history for a wallet screen (`nextCursor` is null on the last page). */
    suspend fun loadTransactions(cursor: String? = null, limit: Int = 30): TransactionsPage =
        dramaApi.getTransactions(limit, cursor)

    suspend fun refreshSettings() {
        try {
            _settings.value = dramaApi.getRedemptionSettings()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Keep previous settings if already cached.
        }
    }

    /** Cached redemption settings; fetched once per process (single-flight). */
    suspend fun getSettings(): SystemSettingsDto {
        _settings.value?.let { return it }
        return settingsMutex.withLock {
            _settings.value ?: dramaApi.getRedemptionSettings().also { _settings.value = it }
        }
    }

    // ─── Claims ─────────────────────────────────────────────────

    suspend fun checkIn(): CheckInResult = mutationMutex.withLock {
        val today = todayIso()
        val claim = pendingDao.find(TYPE_CHECKIN, today)
            ?: newClaim(TYPE_CHECKIN, key = today, clientDate = today)
        sendCheckIn(claim)
    }

    suspend fun rewardAd(adType: String): RewardResult = mutationMutex.withLock {
        // One claim ID per ad view.
        val claimId = UUID.randomUUID().toString()
        val claim = newClaim(TYPE_AD, key = claimId, claimId = claimId, adType = adType)
        sendReward(claim)
    }

    suspend fun rewardEpisode(dramaId: Int, ep: Int): RewardResult = mutationMutex.withLock {
        val key = "$dramaId:$ep"
        val claim = pendingDao.find(TYPE_EPISODE, key)
            ?: newClaim(TYPE_EPISODE, key = key, dramaId = dramaId, ep = ep)
        sendReward(claim)
    }

    suspend fun rewardReel(reelId: Int): RewardResult = mutationMutex.withLock {
        val key = reelId.toString()
        val claim = pendingDao.find(TYPE_REEL, key)
            ?: newClaim(TYPE_REEL, key = key, reelId = reelId)
        sendReward(claim)
    }

    /**
     * Re-send claims whose outcome is unknown, with their original IDs.
     * Called on start-up and whenever connectivity comes back. Never throws.
     */
    suspend fun retryPendingClaims() {
        val pending = runCatching { pendingDao.getAll() }.getOrDefault(emptyList())
        if (pending.isEmpty()) return
        val today = todayIso()
        val cutoff = System.currentTimeMillis() - PENDING_TTL_MS
        for (claim in pending) {
            try {
                mutationMutex.withLock {
                    // Re-read: it may have been settled by a user tap meanwhile.
                    val current = pendingDao.find(claim.type, claim.claimKey) ?: return@withLock
                    when {
                        current.createdAt < cutoff -> pendingDao.delete(current.claimId)
                        // A check-in only counts for its own day.
                        current.type == TYPE_CHECKIN && current.claimKey != today ->
                            pendingDao.delete(current.claimId)
                        current.type == TYPE_CHECKIN -> sendCheckIn(current)
                        else -> sendReward(current)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.isOutcomeUnknown) return // still offline / server down: try again later
            } catch (_: Exception) {
                // Leave it for the next attempt.
            }
        }
    }

    private suspend fun newClaim(
        type: String,
        key: String,
        claimId: String = UUID.randomUUID().toString(),
        dramaId: Int? = null,
        ep: Int? = null,
        reelId: Int? = null,
        adType: String? = null,
        clientDate: String? = todayIso(),
    ) = PendingClaimEntity(
        claimId = claimId,
        type = type,
        claimKey = key,
        dramaId = dramaId,
        ep = ep,
        reelId = reelId,
        adType = adType,
        clientDate = clientDate,
    )

    private suspend fun sendCheckIn(claim: PendingClaimEntity): CheckInResult = settle(
        claim = claim,
        send = {
            dramaApi.checkIn(
                UserIdRequest(
                    userId = getUserId().ifBlank { null },
                    clientDate = claim.clientDate ?: todayIso(),
                    claimId = claim.claimId,
                ),
            )
        },
        profileOf = { it.user },
        balanceOf = { it.newCoinsBalance },
        alreadyClaimed = { e ->
            decode<CheckInResult>(e.body)?.copy(success = true, message = e.message.orEmpty(), alreadyClaimed = true)
                ?: CheckInResult(success = true, message = e.message.orEmpty(), alreadyClaimed = true)
        },
    )

    private suspend fun sendReward(claim: PendingClaimEntity): RewardResult = settle(
        claim = claim,
        send = {
            val userId = getUserId().ifBlank { null }
            when (claim.type) {
                TYPE_AD -> dramaApi.rewardAd(
                    RewardAdRequest(
                        userId = userId,
                        adType = claim.adType ?: "REWARDED_VIDEO",
                        clientDate = claim.clientDate,
                        claimId = claim.claimId,
                    ),
                )
                TYPE_EPISODE -> dramaApi.rewardEpisode(
                    RewardEpisodeRequest(
                        userId = userId,
                        dramaId = requireNotNull(claim.dramaId),
                        ep = requireNotNull(claim.ep),
                        clientDate = claim.clientDate,
                        claimId = claim.claimId,
                    ),
                )
                TYPE_REEL -> dramaApi.rewardReel(
                    RewardReelRequest(
                        userId = userId,
                        reelId = requireNotNull(claim.reelId),
                        clientDate = claim.clientDate,
                        claimId = claim.claimId,
                    ),
                )
                else -> error("Unknown claim type ${claim.type}")
            }
        },
        profileOf = { it.user },
        balanceOf = { it.newCoinsBalance },
        alreadyClaimed = { e ->
            decode<RewardResult>(e.body)?.copy(success = true, message = e.message.orEmpty(), alreadyClaimed = true)
                ?: RewardResult(success = true, message = e.message.orEmpty(), alreadyClaimed = true)
        },
    )

    /**
     * Sends one claim and settles it according to the server's answer
     * (see the table in the API doc). The pending row is written *before*
     * sending, so a crash or kill mid-request still gets retried.
     */
    private suspend fun <T> settle(
        claim: PendingClaimEntity,
        send: suspend () -> T,
        profileOf: (T) -> UserProfile?,
        balanceOf: (T) -> Int?,
        alreadyClaimed: (ApiException) -> T,
    ): T {
        pendingDao.upsert(claim.copy(attempts = claim.attempts + 1))
        try {
            val result = send()
            pendingDao.delete(claim.claimId)
            applyClaimOutcome(profileOf(result), balanceOf(result))
            return result
        } catch (e: CancellationException) {
            throw e // keep the pending row; it's retried later with the same ID
        } catch (e: ApiException) {
            when {
                e.code == ApiException.ALREADY_CLAIMED -> {
                    pendingDao.delete(claim.claimId)
                    val result = alreadyClaimed(e)
                    applyClaimOutcome(profileOf(result), balanceOf(result))
                    return result
                }
                e.isOutcomeUnknown -> {
                    // Keep the row: we'll retry with the same claim ID.
                    throw ApiException(
                        kind = e.kind,
                        message = "Couldn't reach the server. Your reward is saved and will be added automatically when you're back online.",
                        statusCode = e.statusCode,
                        cause = e,
                        code = e.code,
                    )
                }
                else -> {
                    // DAILY_LIMIT / COOLDOWN / CLAIM_ID_REUSED / validation: nothing was paid.
                    pendingDao.delete(claim.claimId)
                    if (e.body != null) applyClaimOutcome(decodeProfile(e.body), decodeBalance(e.body))
                    if (e.code == ApiException.CLAIM_ID_REUSED) {
                        throw ApiException(e.kind, "Something went wrong. Please try again.", e.statusCode, e, e.code)
                    }
                    throw e
                }
            }
        }
    }

    /** Update the balance from a claim response (lite profile or just `newCoinsBalance`). */
    private suspend fun applyClaimOutcome(profile: UserProfile?, newBalance: Int?) {
        when {
            profile != null -> applyProfile(profile, lite = true)
            newBalance != null -> _userProfile.value?.let { applyProfile(it.copy(coins = newBalance)) }
        }
    }

    // ─── Redemptions ────────────────────────────────────────────

    suspend fun listRedemptions(): List<RedemptionRequest> {
        return dramaApi.listRedemptions(getUserId()).items
            .sortedByDescending { it.createdAt }
    }

    /** Throws ApiException with code GOOGLE_LINK_REQUIRED when the server requires a backed-up account. */
    suspend fun createRedemption(request: CreateRedemptionRequest): RedemptionCreateResponse =
        mutationMutex.withLock {
            val result = dramaApi.createRedemption(request)
            result.user?.let { applyProfile(it) }
            result
        }

    // ─── Internals ──────────────────────────────────────────────

    /**
     * Write profile to both the in-memory StateFlow and Room.
     * With [lite] the response has no transactions, so the cached list is kept.
     */
    private suspend fun applyProfile(updated: UserProfile, lite: Boolean = false) {
        val previous = _userProfile.value
        val merged = if (lite && updated.transactions.isEmpty() && previous?.userId == updated.userId) {
            updated.copy(
                transactions = previous.transactions,
                streakSchedule = updated.streakSchedule.ifEmpty { previous.streakSchedule },
                googleEmail = updated.googleEmail ?: previous.googleEmail,
                googleLinked = updated.googleLinked || previous.googleLinked,
            )
        } else {
            updated
        }
        _userProfile.value = merged
        db.withTransaction {
            userDao.deleteOthers(merged.userId)
            userDao.upsert(merged.toEntity())
        }
        if (merged.userId.isNotBlank() && merged.userId != getUserId()) {
            persistUserId(merged.userId)
        }
    }

    private inline fun <reified T> decode(body: String?): T? =
        body?.let { runCatching { NetworkModule.json.decodeFromString<T>(it) }.getOrNull() }

    private fun decodeProfile(body: String?): UserProfile? = decode<RewardResult>(body)?.user

    private fun decodeBalance(body: String?): Int? = decode<RewardResult>(body)?.newCoinsBalance

    /**
     * Today's date as yyyy-MM-dd in Pakistan time (the server's day boundary
     * for check-ins). [Locale.US] keeps the digits ASCII.
     */
    private fun todayIso(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Karachi"))
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
        )
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_LEGACY_MIGRATED = "legacy_migrated"
        private const val KEY_ONBOARDED = "onboarded"
        private const val MIN_SYNC_INTERVAL_MS = 30_000L
        private val PENDING_TTL_MS = TimeUnit.DAYS.toMillis(7)

        private const val TYPE_CHECKIN = "CHECKIN"
        private const val TYPE_AD = "AD"
        private const val TYPE_EPISODE = "EPISODE"
        private const val TYPE_REEL = "REEL"

        @Volatile
        private var instance: UserRepository? = null

        fun getInstance(application: Application = AppContainer.app): UserRepository =
            getInstance(AppContainer.api, application)

        fun getInstance(dramaApi: DramaApi, application: Application): UserRepository {
            return instance ?: synchronized(this) {
                instance ?: UserRepository(
                    dramaApi,
                    application.applicationContext,
                    AppDatabase.getInstance(application),
                ).also { instance = it }
            }
        }
    }
}
