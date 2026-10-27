package com.dramflix.android.data.api

import android.content.Context
import android.util.Log
import com.dramflix.android.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.ConnectionPool
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Builds the one shared [HttpClient] for the whole app.
 *
 * - **OkHttp engine**: HTTP/2 multiplexing, connection pooling and an on-disk
 *   HTTP cache (conditional GETs / 304s against Vercel's ETags).
 * - **expectSuccess**: non-2xx responses throw instead of being decoded into the
 *   success model (previously a 400 `{ "error": … }` crashed deserialisation and
 *   the user saw a cryptic "Field 'coinsEarned' is required" message).
 * - **Retries only for GETs** with exponential backoff + jitter. POSTs (rewards,
 *   check-in, redemption) are never retried automatically so coins can't be
 *   double-credited.
 * - **Logging only in debug builds.**
 */
object NetworkModule {

    private const val TAG = "DramaApi"
    private const val HTTP_CACHE_BYTES = 20L * 1024 * 1024

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        // Don't send `"bankName": null` etc.; treat missing nullable fields as null.
        explicitNulls = false
    }

    @Volatile
    private var appContext: Context? = null

    /** Called once from [com.dramflix.android.DramaApp.onCreate] so the HTTP cache can live in cacheDir. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val baseUrl: String = BuildConfig.API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" }

    val httpClient: HttpClient by lazy { createClient() }

    /**
     * Supplies the bearer token for authenticated calls. Set by
     * [com.dramflix.android.data.AppContainer] to the app's AuthManager.
     */
    @Volatile
    var accessTokenProvider: AccessTokenProvider? = null

    val dramaApi: DramaApi by lazy { DramaApiImpl(httpClient) { accessTokenProvider } }

    private fun createClient(): HttpClient = HttpClient(OkHttp) {
        expectSuccess = true

        engine {
            config {
                retryOnConnectionFailure(true)
                connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
                appContext?.let { ctx ->
                    cache(Cache(File(ctx.cacheDir, "http_cache"), HTTP_CACHE_BYTES))
                }
            }
        }

        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 20_000
            requestTimeoutMillis = 25_000
        }

        install(HttpRequestRetry) {
            maxRetries = 2
            retryIf { request, response ->
                request.method == HttpMethod.Get &&
                    (response.status.value >= 500 || response.status.value == 429)
            }
            retryOnExceptionIf { request, cause ->
                request.method == HttpMethod.Get && isRetryableFailure(cause)
            }
            exponentialDelay(base = 2.0, maxDelayMs = 8_000)
        }

        HttpResponseValidator {
            handleResponseExceptionWithRequest { cause, _ ->
                val responseException = cause as? ResponseException
                    ?: return@handleResponseExceptionWithRequest
                val response = responseException.response
                val body = runCatching { response.bodyAsText() }.getOrNull()
                throw ApiException.fromResponse(
                    statusCode = response.status.value,
                    body = body,
                    cause = responseException,
                    retryAfterHeader = response.headers[HttpHeaders.RetryAfter],
                )
            }
        }

        if (BuildConfig.DEBUG) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d(TAG, message)
                    }
                }
                level = LogLevel.INFO
            }
        }

        defaultRequest {
            url(baseUrl)
            header(HttpHeaders.Accept, "application/json")
            header("X-Client-Platform", "android")
            header("X-App-Version", BuildConfig.VERSION_NAME)
        }
    }

    private fun isRetryableFailure(cause: Throwable): Boolean = when (cause) {
        is CancellationException -> false
        is ApiException -> cause.isRetryable
        is ResponseException -> cause.response.status.value >= 500 ||
            cause.response.status.value == 429
        is IOException -> true
        else -> false
    }
}
