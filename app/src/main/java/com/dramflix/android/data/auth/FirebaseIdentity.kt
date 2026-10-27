package com.dramflix.android.data.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Firebase Authentication is the identity layer:
 *
 * - **Guest** = Firebase anonymous user. Its UID is the app account's id.
 * - **Back up with Google** = `linkWithCredential` on that same anonymous
 *   user, so the UID (and therefore the account and its coins) never changes.
 * - **Sign in with Google** on a new phone = `signInWithCredential`, which
 *   returns the UID that was linked earlier.
 *
 * The server never trusts a bare UID: every session call sends a Firebase
 * **ID token**, which the server verifies with the Firebase Admin SDK.
 *
 * Firebase is configured by the google-services Gradle plugin from
 * `app/google-services.json`.
 */
class FirebaseIdentity(private val context: Context) {

    sealed interface LinkOutcome {
        /** Google is now linked; UID unchanged. */
        data class Linked(val email: String?) : LinkOutcome

        /** That Google account already belongs to another Firebase user (another app account). */
        data object GoogleAccountInUse : LinkOutcome
    }

    class NotConfiguredException : IllegalStateException(
        "Firebase isn't set up in this build (app/google-services.json is missing).",
    )

    /**
     * The default FirebaseApp, created here on first use from the values the
     * google-services plugin generated. Firebase's auto-init ContentProvider is
     * removed in the manifest so launches with a saved session never pay for it.
     * Null if google-services.json is missing.
     */
    private val app: FirebaseApp? by lazy {
        FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context)
    }

    val isConfigured: Boolean
        get() = app != null

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance(app ?: throw NotConfiguredException())
    }

    val currentUser: FirebaseUser?
        get() = if (isConfigured) auth.currentUser else null

    val isAnonymous: Boolean
        get() = currentUser?.isAnonymous == true

    /** Reuse the signed-in Firebase user, or create an anonymous one. */
    suspend fun ensureSignedIn(): FirebaseUser = wrap {
        auth.currentUser ?: requireNotNull(auth.signInAnonymously().await().user) {
            "Anonymous sign-in returned no user"
        }
    }

    /** A fresh Firebase ID token for the server (forced refresh so claims like the Google link are current). */
    suspend fun idToken(forceRefresh: Boolean = true): String = wrap {
        val user = auth.currentUser ?: throw IllegalStateException("Not signed in to Firebase")
        requireNotNull(user.getIdToken(forceRefresh).await().token) { "Firebase returned no ID token" }
    }

    /**
     * Attach Google to the current (anonymous) user. Keeps the same UID, so
     * the server account and its coins stay exactly as they are.
     */
    suspend fun linkGoogle(googleIdToken: String): LinkOutcome = wrap {
        val user = auth.currentUser ?: ensureSignedIn()
        try {
            val result = user.linkWithCredential(GoogleAuthProvider.getCredential(googleIdToken, null)).await()
            LinkOutcome.Linked(result.user?.email ?: googleEmailOf(result.user))
        } catch (_: FirebaseAuthUserCollisionException) {
            LinkOutcome.GoogleAccountInUse
        }
    }

    /**
     * Sign in with Google, replacing the current Firebase user. Used on a new
     * phone, or to switch to a Google account that already has an app account.
     */
    suspend fun signInWithGoogle(googleIdToken: String): FirebaseUser = wrap {
        val result = auth.signInWithCredential(GoogleAuthProvider.getCredential(googleIdToken, null)).await()
        requireNotNull(result.user) { "Google sign-in returned no user" }
    }

    fun signOut() {
        if (isConfigured) auth.signOut()
    }

    private fun googleEmailOf(user: FirebaseUser?): String? =
        user?.providerData?.firstOrNull { it.providerId == GoogleAuthProvider.PROVIDER_ID }?.email

    /** Map Firebase network failures to the app's connectivity error so the UI shows a friendly message. */
    private inline fun <T> wrap(block: () -> T): T = try {
        block()
    } catch (e: FirebaseNetworkException) {
        throw com.dramflix.android.data.api.ApiException.network(e)
    }
}
