package com.dramflix.android

import android.app.Application
import com.dramflix.android.data.AppContainer
import com.dramflix.android.data.api.NetworkModule
import com.dramflix.android.data.auth.AuthState
import com.dramflix.android.data.repo.DramaRepository
import com.dramflix.android.data.repo.ReelsRepository
import com.dramflix.android.data.repo.UserRepository
import com.dramflix.android.data.util.NetworkMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Cold-start budget: `onCreate` only wires up lazy singletons. Everything
 * else runs on background threads, and anything not needed for the first
 * screen waits until after it has been drawn.
 *
 * Also disabled in the manifest: Firebase's auto-initialising ContentProvider
 * (Firebase starts lazily, only when sign-in needs it) and WorkManager's
 * auto-initialiser (in case a library ever adds it).
 */
class DramaApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Wires up lazy singletons only; no I/O, no network client, no database.
        AppContainer.init(this)

        // Build the HTTP client (OkHttp, Ktor, JSON) on a background thread while
        // the first frame is being drawn, so the first API call doesn't pay for it
        // on the main thread.
        AppContainer.appScope.launch { NetworkModule.dramaApi }

        // Housekeeping that isn't needed for the first screen.
        AppContainer.appScope.launch {
            delay(DEFERRED_WORK_DELAY_MS)

            DramaRepository.getInstance().pruneCache()
            ReelsRepository.getInstance().pruneCache()

            // Whenever we're signed in and online, re-send rewards whose result was
            // unknown (timeout / offline / app killed) using their original claim IDs.
            val users = UserRepository.getInstance(this@DramaApp)
            combine(
                AppContainer.auth.state,
                NetworkMonitor.getInstance(this@DramaApp).isOnline,
            ) { auth, online -> auth == AuthState.SignedIn && online }
                .distinctUntilChanged()
                .collectLatest { ready -> if (ready) users.retryPendingClaims() }
        }
    }

    private companion object {
        /** Long enough for the first screen to render and load its data. */
        const val DEFERRED_WORK_DELAY_MS = 5_000L
    }
}
