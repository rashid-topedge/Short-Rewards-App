package com.dramflix.android.data.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * The single error type every [DramaApi] call throws (besides cancellation).
 *
 * [message] is always safe to show to the user: it is either the server's own
 * `error` / `message` field, or a friendly sentence for network problems.
 * [code] is the server's machine-readable `code` (e.g. `TOKEN_EXPIRED`,
 * `ALREADY_CLAIMED`, `GOOGLE_LINK_REQUIRED`) when present.
 */
class ApiException(
    val kind: Kind,
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
    val code: String? = null,
    /** Raw error body, for responses that carry data (e.g. 409 ALREADY_CLAIMED includes the balance). */
    val body: String? = null,
    /** Seconds from a `Retry-After` header (429 COOLDOWN). */
    val retryAfterSeconds: Long? = null,
) : Exception(message, cause) {

    enum class Kind {
        /** No connectivity, DNS failure, connection reset. */
        Network,

        /** Connect / socket / request timeout. */
        Timeout,

        /** HTTP 4xx – the request was rejected (validation, not found, already claimed…). */
        Client,

        /** HTTP 5xx – the server failed. */
        Server,

        /** 2xx response whose body didn't match the expected model. */
        Parse,

        Unknown,
    }

    /** Worth retrying automatically (only ever done for idempotent GETs). */
    val isRetryable: Boolean
        get() = kind == Kind.Network || kind == Kind.Timeout || kind == Kind.Server ||
            statusCode == 429

    /** True when the failure is "you're offline / server unreachable", not a rejection. */
    val isConnectivity: Boolean
        get() = kind == Kind.Network || kind == Kind.Timeout

    /** We don't know whether the server applied the request (so a claim must be retried with the same ID). */
    val isOutcomeUnknown: Boolean
        get() = isConnectivity || kind == Kind.Server

    /** 401 caused by the access token (expired / invalid / missing), fixable by refreshing. */
    val isAccessTokenRejected: Boolean
        get() = statusCode == 401 && (code == null || code.startsWith("TOKEN"))

    /** 401 from the refresh endpoint: the session is gone and the user must sign in again. */
    val isRefreshRejected: Boolean
        get() = statusCode == 401 && code?.startsWith("REFRESH") == true

    companion object {
        // Error codes from the auth / rewards API.
        const val TOKEN_EXPIRED = "TOKEN_EXPIRED"
        const val ACCOUNT_ALREADY_CLAIMED = "ACCOUNT_ALREADY_CLAIMED"
        const val GOOGLE_ACCOUNT_IN_USE = "GOOGLE_ACCOUNT_IN_USE"
        const val ACCOUNT_ALREADY_LINKED = "ACCOUNT_ALREADY_LINKED"
        const val GOOGLE_TOKEN_INVALID = "GOOGLE_TOKEN_INVALID"
        const val GOOGLE_LINK_REQUIRED = "GOOGLE_LINK_REQUIRED"
        const val ALREADY_CLAIMED = "ALREADY_CLAIMED"
        const val DAILY_LIMIT = "DAILY_LIMIT"
        const val COOLDOWN = "COOLDOWN"
        const val CLAIM_ID_REUSED = "CLAIM_ID_REUSED"
        const val CLAIM_ID_REQUIRED = "CLAIM_ID_REQUIRED"

        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        fun fromResponse(
            statusCode: Int,
            body: String?,
            cause: Throwable? = null,
            retryAfterHeader: String? = null,
        ): ApiException {
            val obj = parseObject(body)
            val serverMessage = obj?.string("error", "message", "detail")
            val code = obj?.string("code")
            val retryAfter = retryAfterHeader?.trim()?.toLongOrNull()
            val kind = if (statusCode >= 500) Kind.Server else Kind.Client
            val message = serverMessage ?: when {
                statusCode == 429 -> "Too many requests. Please wait a moment and try again."
                statusCode >= 500 -> "Server error ($statusCode). Please try again shortly."
                else -> "Request failed ($statusCode)."
            }
            return ApiException(
                kind = kind,
                message = message,
                statusCode = statusCode,
                cause = cause,
                code = code,
                body = body,
                retryAfterSeconds = retryAfter,
            )
        }

        fun network(cause: Throwable) = ApiException(
            Kind.Network,
            "No internet connection. Check your network and try again.",
            cause = cause,
        )

        fun timeout(cause: Throwable) = ApiException(
            Kind.Timeout,
            "The server is taking too long to respond. Please try again.",
            cause = cause,
        )

        fun parse(cause: Throwable) = ApiException(
            Kind.Parse,
            "Unexpected response from the server.",
            cause = cause,
        )

        private fun parseObject(body: String?): JsonObject? {
            if (body.isNullOrBlank()) return null
            return runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull()
        }

        private fun JsonObject.string(vararg keys: String): String? {
            for (key in keys) {
                val value = (this[key] as? JsonPrimitive)?.contentOrNull?.trim()
                if (!value.isNullOrEmpty()) return value
            }
            return null
        }
    }
}
