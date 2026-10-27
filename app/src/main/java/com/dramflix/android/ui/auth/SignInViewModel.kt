package com.dramflix.android.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dramflix.android.data.AppContainer
import com.dramflix.android.data.auth.AuthManager
import com.dramflix.android.data.auth.GoogleIdTokenResult
import com.dramflix.android.data.auth.GoogleSignIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SignInAction { Google, Guest }

data class SignInUiState(
    /** Which button is working, if any. */
    val inProgress: SignInAction? = null,
    val error: String? = null,
)

class SignInViewModel(
    private val auth: AuthManager = AppContainer.auth,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    /**
     * Pick a Google account, then sign in with it. Signing in with an account
     * that has no app account yet creates one (`created = true`).
     * [activityContext] is only used for the duration of the call.
     */
    fun signInWithGoogle(activityContext: Context) {
        if (_state.value.inProgress != null) return
        viewModelScope.launch {
            _state.update { it.copy(inProgress = SignInAction.Google, error = null) }
            try {
                when (val token = GoogleSignIn.requestIdToken(activityContext)) {
                    GoogleIdTokenResult.Cancelled -> Unit
                    is GoogleIdTokenResult.Failure -> _state.update { it.copy(error = token.message) }
                    is GoogleIdTokenResult.Success -> auth.signInWithGoogle(token.idToken)
                    // On success AuthManager switches to SignedIn and this screen goes away.
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Sign-in failed. Please try again.") }
            } finally {
                _state.update { it.copy(inProgress = null) }
            }
        }
    }

    /** Create an anonymous account. It can be backed up with Google later. */
    fun continueAsGuest() {
        if (_state.value.inProgress != null) return
        viewModelScope.launch {
            _state.update { it.copy(inProgress = SignInAction.Guest, error = null) }
            try {
                auth.startGuestSession()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Couldn't create your account. Please try again.") }
            } finally {
                _state.update { it.copy(inProgress = null) }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}
