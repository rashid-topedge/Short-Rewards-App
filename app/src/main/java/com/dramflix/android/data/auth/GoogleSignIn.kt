package com.dramflix.android.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.SecureRandom
import android.util.Base64

/** Result of asking the user to pick a Google account. */
sealed interface GoogleIdTokenResult {
    data class Success(val idToken: String, val email: String?) : GoogleIdTokenResult

    /** The user closed the sheet. Not an error – show nothing. */
    data object Cancelled : GoogleIdTokenResult

    data class Failure(val message: String) : GoogleIdTokenResult
}

/**
 * "Sign in with Google" through Credential Manager.
 *
 * Uses the Firebase project's **Web** OAuth client ID as `serverClientId`
 * (Firebase Auth verifies the token against it). The google-services plugin
 * generates it from google-services.json as `R.string.default_web_client_id`.
 * Each signing key's SHA-1 must be added to the Firebase Android app, or
 * Google returns "developer console is not set up correctly".
 */
object GoogleSignIn {

    /** `default_web_client_id`, looked up by name so a json without it doesn't break the build. */
    private fun webClientId(context: Context): String? {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id == 0) null else context.getString(id).takeIf { it.isNotBlank() }
    }

    fun isConfigured(context: Context): Boolean = webClientId(context) != null

    /**
     * Shows the Google account picker and returns an ID token.
     * [context] must be (or wrap) an Activity.
     */
    suspend fun requestIdToken(context: Context): GoogleIdTokenResult {
        val webClientId = webClientId(context)
            ?: return GoogleIdTokenResult.Failure(
                "Google sign-in isn't set up yet. Enable Google in Firebase Authentication, " +
                    "then download google-services.json again.",
            )
        val activity = context.findActivity()
            ?: return GoogleIdTokenResult.Failure("Google sign-in needs an open screen. Please try again.")

        val option = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(newNonce())
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val response = CredentialManager.create(activity).getCredential(activity, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleIdTokenResult.Success(idToken = google.idToken, email = google.id)
            } else {
                GoogleIdTokenResult.Failure("Unexpected sign-in response. Please try again.")
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleIdTokenResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleIdTokenResult.Failure("No Google account found on this phone. Add one in Settings → Accounts.")
        } catch (e: GetCredentialException) {
            GoogleIdTokenResult.Failure(e.errorMessage?.toString() ?: "Google sign-in failed. Please try again.")
        } catch (_: GoogleIdTokenParsingException) {
            GoogleIdTokenResult.Failure("Google sign-in failed. Please try again.")
        }
    }

    private fun newNonce(): String {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
