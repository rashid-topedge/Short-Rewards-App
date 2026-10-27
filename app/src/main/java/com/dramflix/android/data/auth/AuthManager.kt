package com.dramflix.android.data.auth

import com.dramflix.android.data.api.AccessTokenProvider
import com.dramflix.android.data.api.ApiException
import com.dramflix.android.data.api.DramaApi
import com.dramflix.android.data.api.FirebaseSessionRequest
import com.dramflix.android.data.api.GoogleIdTokenRequest
import com.dramflix.android.data.api.RefreshTokenRequest
import com.dramflix.android.data.api.SessionResponse
import com.dramflix.android.data.repo.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Where the app is in the sign-in lifecycle. */
sealed interface AuthState {
    /** Reading tokens / talking to the server on start-up. */
    data object Initializing : AuthState

    /** Couldn't start (e.g. offline on first launch of an existing install). */
    data class StartupFailed(val message: String) : AuthState

    /** Show the sign-in screen. */
    data class SignInRequired(val reason: SignInReason) : AuthState

    /** A session exists; show the app. */
    data object SignedIn : AuthState
}

enum class SignInReason {
    /** Brand-new install: welcome screen with Google / guest. */
    FirstLaunch,

    /** Returning install without a session (reinstall with restored prefs, or after sign-out). */
    Returning,

    /** This device's old account is already protected; recover it with Google. */
    AccountClaimed,

    /** The refresh token was rejected (expired, revoked, reused). */
    SessionExpired,
}

/** Outcome of linking Google to the current (guest) account. */
sealed interface LinkResult {
    data class Linked(val email: String?) : LinkResult

    /** That Google account already has its own app account; offer to switch to it. */
    data object GoogleAccountInUse : LinkResult

    /** This account is already linked to a different Google account. */
    data class AlreadyLinkedToAnother(val message: String) : LinkResult
}

/**
 * Owns the session: token storage, first-launch account creation, token
 * refresh, Google sign-in / linking and sign-out. Also the
 * [AccessTokenProvider] used by every API call.
 *
 * Identity comes from Firebase ([FirebaseIdentity]): a guest is a Firebase
 * anonymous user, and "back up with Google" links Google to that same user,
 * so the UID – and the server account keyed by it – never changes. The app
 * trades a verified Firebase ID token for its own access/refresh tokens at
 * `POST /api/user/firebase-session`.
 *
 * Refresh is single-flight: concurrent 401s or near-expiry checks wait on one
 * [refreshMutex], and the rotated refresh token is saved before anyone else
 * can use the old one (the server rejects reuse with REFRESH_REUSED).
 */
class AuthManager(
    /** Resolved on first use, so creating the AuthManager at app start doesn't build the HTTP client. */
    apiProvider: () -> DramaApi,
    private val tokenStore: TokenStore,
    private val firebase: FirebaseIdentity,
    private val userRepository: () -> UserRepository,
    /** For start-up work that isn't needed to decide the first screen. */
    private val backgroundScope: CoroutineScope,
) : AccessTokenProvider {

    private val api: DramaApi by lazy(apiProvider)

    private val _state = MutableStateFlow<AuthState>(AuthState.Initializing)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val refreshMutex = Mutex()
    private val sessionMutex = Mutex()

    // ─── Start-up ───────────────────────────────────────────────

    /** Decide what to show on launch. Safe to call again (e.g. a Retry button). */
    suspend fun start() {
        _state.value = AuthState.Initializing
        tokenStore.load()
        val users = userRepository()
        // Opening the database isn't needed to pick the first screen, so it
        // happens in parallel (screens show their own loading state meanwhile).
        backgroundScope.launch { users.restoreCachedProfile() }

        if (tokenStore.hasSession) {
            _state.value = AuthState.SignedIn
            return
        }

        // Firebase still knows who this is (e.g. app tokens were lost): get new
        // app tokens silently instead of showing the sign-in screen.
        if (firebase.currentUser != null) {
            try {
                sessionMutex.withLock { exchangeFirebaseIdentity(legacyUserId = null) }
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.isConnectivity) {
                    _state.value = AuthState.StartupFailed(e.message ?: "Couldn't connect.")
                    return
                }
                // Server rejected it: fall through to the normal sign-in choices.
            } catch (_: Exception) {
                // Firebase error: fall through.
            }
        }

        // SharedPreferences reads hit disk on a cold start: keep them off the main thread.
        val (legacyId, onboarded) = withContext(Dispatchers.IO) {
            users.legacyUserIdForMigration() to users.hasCompletedOnboarding
        }
        when {
            // Updating from a pre-auth version: keep the existing account silently.
            legacyId != null -> try {
                startGuestSession(legacyUserId = legacyId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                _state.value = when {
                    e.code == ApiException.ACCOUNT_ALREADY_CLAIMED -> {
                        users.markLegacyMigrated()
                        // Drop the fresh anonymous user; recovery is via Google.
                        firebase.signOut()
                        AuthState.SignInRequired(SignInReason.AccountClaimed)
                    }
                    else -> AuthState.StartupFailed(e.message ?: "Couldn't connect.")
                }
            } catch (e: Exception) {
                _state.value = AuthState.StartupFailed(e.message ?: "Couldn't start. Please try again.")
            }

            onboarded -> _state.value = AuthState.SignInRequired(SignInReason.Returning)
            else -> _state.value = AuthState.SignInRequired(SignInReason.FirstLaunch)
        }
    }

    // ─── Starting a session ─────────────────────────────────────

    /**
     * "Continue as guest": sign in to Firebase anonymously and create the app
     * account for that UID. With [legacyUserId] (update from a pre-auth
     * version) the server attaches the UID to the existing account once, so
     * its coins carry over.
     */
    suspend fun startGuestSession(legacyUserId: String? = null) = sessionMutex.withLock {
        firebase.ensureSignedIn()
        exchangeFirebaseIdentity(legacyUserId)
    }

    /**
     * Sign in with Google (reinstall, new phone, or switching to the Google
     * account's existing app account). Firebase returns the UID that Google was
     * linked to before, so the same server account – with its coins – comes
     * back. Returns true if a brand-new account was created.
     */
    suspend fun signInWithGoogle(idToken: String): Boolean = sessionMutex.withLock {
        firebase.signInWithGoogle(idToken)
        exchangeFirebaseIdentity(legacyUserId = null).created
    }

    /** Must hold [sessionMutex]. Sends a fresh, verifiable Firebase ID token and adopts the session. */
    private suspend fun exchangeFirebaseIdentity(legacyUserId: String?): SessionResponse {
        val user = firebase.currentUser ?: firebase.ensureSignedIn()
        val response = api.firebaseSession(
            FirebaseSessionRequest(
                firebaseIdToken = firebase.idToken(forceRefresh = true),
                userId = user.uid,
                legacyUserId = legacyUserId,
                clientDate = todayIso(),
            ),
        )
        adoptSession(response)
        return response
    }

    private suspend fun adoptSession(response: SessionResponse) {
        val users = userRepository()
        val previousUserId = users.userProfile.value?.userId ?: users.getUserId()
        tokenStore.save(response.tokens)
        users.onSessionStarted(
            profile = response.profile,
            isDifferentAccount = previousUserId.isNotBlank() && previousUserId != response.profile.userId,
        )
        _state.value = AuthState.SignedIn
    }

    // ─── Linking ────────────────────────────────────────────────

    /**
     * Back up the current account with Google by linking it to the current
     * Firebase user. The UID stays the same, so nothing moves on the server:
     * we only re-send the (now Google-linked) ID token so the server records
     * googleLinked / googleEmail.
     */
    suspend fun linkGoogle(idToken: String): LinkResult {
        if (firebase.currentUser != null) {
            return when (val outcome = firebase.linkGoogle(idToken)) {
                FirebaseIdentity.LinkOutcome.GoogleAccountInUse -> LinkResult.GoogleAccountInUse
                is FirebaseIdentity.LinkOutcome.Linked -> {
                    val session = sessionMutex.withLock { exchangeFirebaseIdentity(legacyUserId = null) }
                    LinkResult.Linked(session.profile.googleEmail ?: outcome.email)
                }
            }
        }
        // Session created before Firebase was added: use the server's own link endpoint.
        return try {
            val profile = api.linkGoogle(GoogleIdTokenRequest(idToken))
            userRepository().onProfileUpdated(profile)
            LinkResult.Linked(profile.googleEmail)
        } catch (e: ApiException) {
            when (e.code) {
                ApiException.GOOGLE_ACCOUNT_IN_USE -> LinkResult.GoogleAccountInUse
                ApiException.ACCOUNT_ALREADY_LINKED -> LinkResult.AlreadyLinkedToAnother(
                    e.message ?: "This account is already linked to a different Google account.",
                )
                else -> throw e
            }
        }
    }

    // ─── Sign-out ───────────────────────────────────────────────

    /**
     * Revoke the refresh token on the server (best effort), then delete both
     * tokens and this account's local data.
     */
    suspend fun signOut() = withContext(NonCancellable) {
        sessionMutex.withLock {
            val refreshToken = tokenStore.refreshToken
            if (refreshToken != null) {
                withTimeoutOrNull(LOGOUT_TIMEOUT_MS) {
                    runCatching { api.logout(RefreshTokenRequest(refreshToken)) }
                }
            }
            tokenStore.clear()
            firebase.signOut()
            userRepository().clearAccountData()
            _state.value = AuthState.SignInRequired(SignInReason.Returning)
        }
    }

    // ─── AccessTokenProvider ────────────────────────────────────

    override suspend fun currentAccessToken(): String? {
        val token = tokenStore.accessToken ?: return null
        if (!tokenStore.accessExpiresWithin(EXPIRY_MARGIN_MS)) return token
        return try {
            refreshMutex.withLock {
                // Another caller may have refreshed while we waited.
                if (!tokenStore.accessExpiresWithin(EXPIRY_MARGIN_MS)) tokenStore.accessToken else refreshLocked()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            // Offline: send the old token; if it's rejected, the 401 path retries.
            if (e.isConnectivity) token else null
        }
    }

    override suspend fun refreshAfterRejection(rejectedToken: String?): String? {
        return refreshMutex.withLock {
            val current = tokenStore.accessToken
            if (current != null && current != rejectedToken) {
                current // someone else already refreshed
            } else {
                refreshLocked()
            }
        }
    }

    /** Must hold [refreshMutex]. Returns the new access token, or null if the session is gone. */
    private suspend fun refreshLocked(): String? {
        val refreshToken = tokenStore.refreshToken ?: run {
            if (_state.value == AuthState.SignedIn) onSessionLost()
            return null
        }
        return try {
            // NonCancellable: if the server rotates the token we must save the new one.
            val pair = withContext(NonCancellable) {
                api.refreshTokens(RefreshTokenRequest(refreshToken)).also { tokenStore.save(it) }
            }
            pair.accessToken
        } catch (e: ApiException) {
            if (e.isRefreshRejected) {
                recoverWithFirebase() ?: run {
                    onSessionLost()
                    null
                }
            } else {
                throw e
            }
        }
    }

    /**
     * The app refresh token was rejected, but Firebase may still have a valid
     * user: trade a fresh ID token for new app tokens without bothering the user.
     */
    private suspend fun recoverWithFirebase(): String? {
        if (firebase.currentUser == null) return null
        return try {
            withContext(NonCancellable) {
                val user = firebase.currentUser ?: return@withContext null
                val response = api.firebaseSession(
                    FirebaseSessionRequest(
                        firebaseIdToken = firebase.idToken(forceRefresh = true),
                        userId = user.uid,
                        clientDate = todayIso(),
                    ),
                )
                tokenStore.save(response.tokens)
                userRepository().onProfileUpdated(response.profile)
                response.tokens.accessToken
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun onSessionLost() {
        tokenStore.clear()
        firebase.signOut()
        userRepository().clearAccountData()
        _state.value = AuthState.SignInRequired(SignInReason.SessionExpired)
    }

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

    private companion object {
        /** Refresh this long before the access token expires. */
        const val EXPIRY_MARGIN_MS = 60_000L
        const val LOGOUT_TIMEOUT_MS = 5_000L
    }
}
