package com.dramflix.android.data.api

import com.dramflix.android.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkModule {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
            .build()
    }

    @Volatile
    private var cachedBaseUrl: String? = null

    @Volatile
    private var cachedApi: DramaApi? = null

    private fun currentBaseUrl(): String = BuildConfig.API_BASE_URL

    val dramaApi: DramaApi
        get() {
            val baseUrl = currentBaseUrl()
            cachedApi?.let { api ->
                if (baseUrl == cachedBaseUrl) return api
            }
            return synchronized(this) {
                val url = currentBaseUrl()
                cachedApi?.takeIf { url == cachedBaseUrl } ?: Retrofit.Builder()
                    .baseUrl(url)
                    .client(okHttp)
                    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                    .build()
                    .create(DramaApi::class.java)
                    .also {
                        cachedBaseUrl = url
                        cachedApi = it
                    }
            }
        }
}
