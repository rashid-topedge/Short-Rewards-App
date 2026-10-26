package com.dramflix.android.data.repo

import android.app.Application
import android.content.Context
import android.provider.Settings
import com.dramflix.android.data.api.DramaApi
import com.dramflix.android.data.api.RewardAdRequest
import com.dramflix.android.data.api.RewardEpisodeRequest
import com.dramflix.android.data.api.RewardReelRequest
import com.dramflix.android.data.api.UserIdRequest
import com.dramflix.android.data.local.AppDatabase
import com.dramflix.android.data.local.dao.UserDao
import com.dramflix.android.data.local.toDomain
import com.dramflix.android.data.local.toEntity
import com.dramflix.android.data.model.CheckInResult
import com.dramflix.android.data.model.CreateRedemptionRequest
import com.dramflix.android.data.model.RedemptionCreateResponse
import com.dramflix.android.data.model.RedemptionRequest
import com.dramflix.android.data.model.RewardResult
import com.dramflix.android.data.model.SystemSettingsDto
import com.dramflix.android.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.util.Calendar

/**
 * User repository – now **offline-first**.
 *
 * The user profile is persisted to Room so it survives process death and
 * is available instantly on cold start. The in-memory [StateFlow] is kept
 * in sync with Room for backward-compatibility with existing ViewModels.
 */
class UserRepository private constructor(
    private val dramaApi: DramaApi,
    private val context: Context,
    private val userDao: UserDao,
) {
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    // In-memory StateFlow kept for backward-compatibility.
    // Initialised from Room cache in init{}.
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    /** Observe profile directly from Room (for new consumers). */
    val userProfileFlow: Flow<UserProfile?> = userDao.observe().map { it?.toDomain() }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _settings = MutableStateFlow<SystemSettingsDto?>(null)
    val settings: StateFlow<SystemSettingsDto?> = _settings.asStateFlow()

    /**
     * Restore the last cached profile from Room so the StateFlow isn't null
     * on cold start. Called once from [DramaApp.onCreate].
     */
    suspend fun restoreCachedProfile() {
        val cached = userDao.get()?.toDomain()
        if (cached != null) {
            _userProfile.value = cached
        }
    }

    fun getUserId(): String {
        val persisted = prefs.getString(KEY_USER_ID, null)?.trim()
        if (!persisted.isNullOrBlank()) return persisted
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        ) ?: "android-${System.currentTimeMillis()}"
        prefs.edit().putString(KEY_USER_ID, androidId).apply()
        return androidId
    }

    fun persistUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    suspend fun sync() {
        if (_isSyncing.value) return
        _isSyncing.update { true }
        try {
            val userId = getUserId()
            val updated = dramaApi.syncUser(UserIdRequest(userId, todayIso()))
            applyProfile(updated)
        } catch (_: Exception) {
            // On failure, ensure we at least have the cached profile loaded.
            if (_userProfile.value == null) {
                val cached = userDao.get()?.toDomain()
                _userProfile.value = cached ?: placeholderProfile()
            }
        } finally {
            _isSyncing.update { false }
        }
    }

    suspend fun refreshSettings() {
        try {
            _settings.value = dramaApi.getRedemptionSettings()
        } catch (_: Exception) {
            // Keep previous settings if already cached.
        }
    }

    suspend fun checkIn(): CheckInResult {
        val result = dramaApi.checkIn(UserIdRequest(getUserId(), todayIso()))
        applyProfile(result.user)
        return result
    }

    suspend fun rewardAd(adType: String): RewardResult {
        val result = dramaApi.rewardAd(
            RewardAdRequest(userId = getUserId(), adType = adType, clientDate = todayIso()),
        )
        applyProfile(result.user)
        return result
    }

    suspend fun rewardEpisode(dramaId: Int, ep: Int): RewardResult {
        val result = dramaApi.rewardEpisode(
            RewardEpisodeRequest(
                userId = getUserId(),
                dramaId = dramaId,
                ep = ep,
                clientDate = todayIso(),
            ),
        )
        applyProfile(result.user)
        return result
    }

    suspend fun rewardReel(reelId: Int): RewardResult {
        val result = dramaApi.rewardReel(
            RewardReelRequest(
                userId = getUserId(),
                reelId = reelId,
                clientDate = todayIso(),
            ),
        )
        applyProfile(result.user)
        return result
    }

    suspend fun listRedemptions(): List<RedemptionRequest> {
        return dramaApi.listRedemptions(getUserId()).items
    }

    suspend fun createRedemption(request: CreateRedemptionRequest): RedemptionCreateResponse {
        val result = dramaApi.createRedemption(request)
        result.user?.let { applyProfile(it) }
        return result
    }

    suspend fun getSettings(): SystemSettingsDto {
        _settings.value?.let { return it }
        val loaded = dramaApi.getRedemptionSettings()
        _settings.value = loaded
        return loaded
    }

    /**
     * Write profile to both in-memory StateFlow **and** Room.
     */
    private suspend fun applyProfile(updated: UserProfile) {
        _userProfile.value = updated
        userDao.upsert(updated.toEntity())
        if (updated.userId.isNotBlank() && updated.userId != getUserId()) {
            persistUserId(updated.userId)
        }
    }

    private fun placeholderProfile(): UserProfile {
        return UserProfile(
            userId = getUserId(),
            coins = 0,
            streakDay = 0,
            lastCheckInDate = null,
            canCheckInToday = true,
            nextCheckInReward = 10,
            streakSchedule = emptyList(),
            transactions = emptyList(),
        )
    }

    private fun todayIso(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    companion object {
        private const val KEY_USER_ID = "user_id"

        @Volatile
        private var instance: UserRepository? = null

        fun getInstance(application: Application): UserRepository {
            return instance ?: synchronized(this) {
                instance ?: UserRepository(
                    DramaApi.getInstance(),
                    application.applicationContext,
                    AppDatabase.getInstance(application).userDao(),
                ).also { instance = it }
            }
        }

        fun getInstance(dramaApi: DramaApi, application: Application): UserRepository {
            return instance ?: synchronized(this) {
                instance ?: UserRepository(
                    dramaApi,
                    application.applicationContext,
                    AppDatabase.getInstance(application).userDao(),
                ).also { instance = it }
            }
        }
    }
}
