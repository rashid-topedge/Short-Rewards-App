package com.dramflix.android

import android.app.Application
import com.dramflix.android.data.config.ServerConfigRepository
import com.dramflix.android.data.repo.UserRepository

class DramaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServerConfigRepository.init(this)
        UserRepository.getInstance(this)
    }
}
