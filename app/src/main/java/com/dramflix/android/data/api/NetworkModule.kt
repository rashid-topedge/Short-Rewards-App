package com.dramflix.android.data.api

import com.dramflix.android.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object NetworkModule {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Volatile
    private var cachedBaseUrl: String? = null

    @Volatile
    private var cachedClient: HttpClient? = null

    @Volatile
    private var cachedApi: DramaApi? = null

    private fun currentBaseUrl(): String = BuildConfig.API_BASE_URL

    private fun getHttpClient(baseUrl: String): HttpClient {
        val url = baseUrl.removeSuffix("/")
        cachedClient?.let { client ->
            if (url == cachedBaseUrl) return client
        }

        return synchronized(this) {
            cachedClient?.takeIf { url == cachedBaseUrl } ?: HttpClient(Android) {
                install(ContentNegotiation) {
                    json(json)
                }

                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            println("Ktor: $message")
                        }
                    }
                    level = LogLevel.INFO
                }

                defaultRequest {
                    url(url)
                    contentType(ContentType.Application.Json)
                    header("Accept", "application/json")
                }

                engine {
                    connectTimeout = 30_000
                    socketTimeout = 30_000
                }
            }.also {
                cachedBaseUrl = url
                cachedClient = it
            }
        }
    }

    val dramaApi: DramaApi
        get() {
            val baseUrl = currentBaseUrl()
            cachedApi?.let { api ->
                if (baseUrl == cachedBaseUrl) return api
            }
            return synchronized(this) {
                cachedApi?.takeIf { baseUrl == cachedBaseUrl } ?: DramaApiImpl(
                    getHttpClient(baseUrl)
                ).also {
                    cachedApi = it
                }
            }
        }
}
