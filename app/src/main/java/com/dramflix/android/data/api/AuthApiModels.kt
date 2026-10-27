package com.dramflix.android.data.api

import kotlinx.serialization.Serializable

/** `POST /api/user/refresh` and `POST /api/user/logout` body. */
@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
)

/**
 * `POST /api/user/firebase-session` body.
 *
 * The server must verify [firebaseIdToken] with the Firebase Admin SDK and
 * check that its `uid` equals [userId]; the id alone proves nothing.
 */
@Serializable
data class FirebaseSessionRequest(
    val firebaseIdToken: String,
    /** The Firebase UID (also inside the verified token). */
    val userId: String,
    /** Pre-auth account id from an older app version, attached to this UID once. */
    val legacyUserId: String? = null,
    val clientDate: String? = null,
)

/** `POST /api/user/login-google` and `POST /api/user/link-google` body. */
@Serializable
data class GoogleIdTokenRequest(
    val idToken: String,
)

/**
 * Token pair. Returned on its own by `/refresh`, and merged into the profile
 * object by `/sync` (with `requestToken`) and `/login-google`.
 * `*ExpiresIn` values are in seconds.
 */
@Serializable
data class TokenPair(
    val accessToken: String,
    val accessTokenExpiresIn: Long = 30 * 60,
    val refreshToken: String,
    val refreshTokenExpiresIn: Long = 180L * 24 * 60 * 60,
)

/** Extra fields on `/login-google` (and `/link-google`) responses besides the profile. */
@Serializable
data class GoogleAuthFlags(
    /** login-google: a brand-new account was created for this Google user. */
    val created: Boolean = false,
    /** link-google: the account is now linked. */
    val linked: Boolean = false,
)

/** Result of a call that starts a session: the profile plus its tokens. */
data class SessionResponse(
    val profile: com.dramflix.android.data.model.UserProfile,
    val tokens: TokenPair,
    val created: Boolean = false,
)

/**
 * Supplies bearer tokens to [DramaApiImpl]. Implemented by
 * [com.dramflix.android.data.auth.AuthManager].
 */
interface AccessTokenProvider {
    /** A usable access token (refreshed first if it's about to expire), or null if there is no session. */
    suspend fun currentAccessToken(): String?

    /**
     * Called when the server rejected [rejectedToken]. Refreshes (once per
     * device, even with many concurrent callers) and returns the new token,
     * or null if the session can't be recovered.
     */
    suspend fun refreshAfterRejection(rejectedToken: String?): String?
}
