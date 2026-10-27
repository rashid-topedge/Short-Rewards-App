package com.dramflix.android.data

import android.app.Application
import com.dramflix.android.data.api.DramaApi
import com.dramflix.android.data.api.NetworkModule
import com.dramflix.android.data.auth.AuthManager
import com.dramflix.android.data.auth.FirebaseIdentity
import com.dramflix.android.data.auth.TokenStore
import com.dramflix.android.data.local.AppDatabase
import com.dramflix.android.data.repo.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Minimal manual dependency container. Initialised once in
 * [com.dramflix.android.DramaApp.onCreate]; every repository resolves its
 * dependencies from here, so no call site needs to pass a Context and the
 * order in which singletons are first touched no longer matters.
 */
object AppContainer {

    @Volatile
    private var application: Application? = null

    val app: Application
        get() = application
            ?: error("AppContainer.init() must be called from DramaApp.onCreate()")

    fun init(app: Application) {
        if (application != null) return
        synchronized(this) {
            if (application == null) {
                application = app
                NetworkModule.init(app)
                // Every API call asks the auth manager for its bearer token.
                NetworkModule.accessTokenProvider = auth
            }
        }
    }

    val database: AppDatabase by lazy { AppDatabase.getInstance(app) }

    val api: DramaApi get() = NetworkModule.dramaApi

    val tokenStore: TokenStore by lazy { TokenStore(app) }

    /** Firebase Auth: anonymous guests, Google linking (same UID), Google sign-in. */
    val firebase: FirebaseIdentity by lazy { FirebaseIdentity(app) }

    /** Session owner: sign-in state, token refresh, Google link/login, sign-out. */
    val auth: AuthManager by lazy {
        AuthManager(
            apiProvider = { api },
            tokenStore = tokenStore,
            firebase = firebase,
            userRepository = { UserRepository.getInstance(app) },
            backgroundScope = appScope,
        )
    }

    /** Long-lived scope for fire-and-forget work that must outlive a screen (view/like sync, cache pruning). */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
