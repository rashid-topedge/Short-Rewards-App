package com.dramflix.android

import android.app.Application
import com.dramflix.android.data.local.AppDatabase
import com.dramflix.android.data.repo.DramaRepository
import com.dramflix.android.data.repo.ReelsRepository
import com.dramflix.android.data.repo.UserRepository
import com.dramflix.android.data.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DramaApp : Application() {

    /** Application-scoped coroutine scope for one-shot init work. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Eagerly initialise singletons so the first getInstance() call
        // from any ViewModel never throws for missing Context.
        AppDatabase.getInstance(this)
        NetworkMonitor.getInstance(this)
        DramaRepository.getInstance(context = this)
        ReelsRepository.getInstance(context = this)
        val userRepo = UserRepository.getInstance(this)

        // Restore the last cached profile from Room so the StateFlow
        // is populated before the first screen renders.
        appScope.launch {
            userRepo.restoreCachedProfile()
        }
    }
}
