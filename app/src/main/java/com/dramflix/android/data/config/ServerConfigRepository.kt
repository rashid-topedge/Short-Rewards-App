package com.dramflix.android.data.config

import android.content.Context
import android.content.SharedPreferences
import com.dramflix.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ServerConfigRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("server_config_prefs", Context.MODE_PRIVATE)

    fun getBaseUrl(): String {
        val saved = prefs.getString(KEY_BASE_URL, null)?.trim()
        val url = if (!saved.isNullOrBlank()) saved else BuildConfig.API_BASE_URL
        return normalizeUrl(url)
    }

    fun setBaseUrl(url: String) {
        val normalized = normalizeUrl(url.trim())
        prefs.edit().putString(KEY_BASE_URL, normalized).apply()
    }

    fun resetToDefault() {
        prefs.edit().remove(KEY_BASE_URL).apply()
    }

    suspend fun testConnection(url: String): Result<String> = withContext(Dispatchers.IO) {
        val normalized = normalizeUrl(url.trim())
        val testUrl = if (normalized.endsWith("/")) "${normalized}api/dramas?limit=1" else "$normalized/api/dramas?limit=1"

        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        try {
            val request = Request.Builder().url(testUrl).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Connected successfully (Status ${response.code})")
                } else {
                    Result.failure(Exception("Server returned status ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to connect to server"))
        }
    }

    private fun normalizeUrl(url: String): String {
        var u = url.trim()
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://$u"
        }
        if (!u.endsWith("/")) {
            u = "$u/"
        }
        return u
    }

    companion object {
        private const val KEY_BASE_URL = "custom_base_url"

        @Volatile
        private var instance: ServerConfigRepository? = null

        fun init(context: Context): ServerConfigRepository {
            return instance ?: synchronized(this) {
                instance ?: ServerConfigRepository(context.applicationContext).also { instance = it }
            }
        }

        fun getInstance(): ServerConfigRepository {
            return instance ?: throw IllegalStateException("ServerConfigRepository not initialized. Call init(context) first.")
        }
    }
}
