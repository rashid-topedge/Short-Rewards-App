package com.dramflix.android.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dramflix.android.data.AppContainer
import com.dramflix.android.data.auth.AuthState
import com.dramflix.android.ui.auth.AuthLoadingScreen
import com.dramflix.android.ui.auth.AuthStartupErrorScreen
import com.dramflix.android.ui.auth.SignInScreen
import kotlinx.coroutines.launch

/**
 * Top of the UI tree: shows the sign-in flow until a session exists, then the
 * app. If the session is lost later (refresh token rejected, sign-out), this
 * switches back to the sign-in screen automatically.
 */
@Composable
fun AppRoot() {
    val auth = AppContainer.auth
    val state by auth.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (state == AuthState.Initializing) auth.start()
    }

    AnimatedContent(
        targetState = state,
        contentKey = { it::class },
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
        label = "auth",
    ) { current ->
        when (current) {
            AuthState.Initializing -> AuthLoadingScreen()
            is AuthState.StartupFailed -> AuthStartupErrorScreen(
                message = current.message,
                onRetry = { scope.launch { auth.start() } },
            )
            is AuthState.SignInRequired -> SignInScreen(reason = current.reason)
            AuthState.SignedIn -> DramaNavGraph()
        }
    }
}
