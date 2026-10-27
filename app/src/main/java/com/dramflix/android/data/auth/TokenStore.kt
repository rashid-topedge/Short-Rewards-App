package com.dramflix.android.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.dramflix.android.data.api.TokenPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Holds the access / refresh token pair.
 *
 * Tokens are encrypted with an AES-256-GCM key that lives in the Android
 * Keystore (never leaves secure hardware where available) and stored in a
 * private SharedPreferences file that is excluded from cloud backup. If the
 * key is gone (backup restored to a new phone, keystore reset) the blob can't
 * be decrypted and is treated as "no session" – the user then recovers the
 * account with Google sign-in.
 */
class TokenStore(context: Context) {

    @Serializable
    data class Tokens(
        val accessToken: String,
        val accessExpiresAt: Long,
        val refreshToken: String,
        val refreshExpiresAt: Long,
    )

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()

    @Volatile
    private var tokens: Tokens? = null

    @Volatile
    private var loaded = false

    private val _hasSession = MutableStateFlow(false)

    /** True while a non-expired refresh token is stored. */
    val hasSessionFlow: StateFlow<Boolean> = _hasSession.asStateFlow()

    /** Decrypts the stored tokens. Call once off the main thread before first use. */
    suspend fun load() = withContext(Dispatchers.IO) { ensureLoaded() }

    private fun ensureLoaded() {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            val blob = prefs.getString(KEY_BLOB, null)
            tokens = blob?.let { encoded ->
                runCatching {
                    json.decodeFromString(Tokens.serializer(), KeystoreCipher.decrypt(encoded))
                }.getOrNull()
            }
            if (blob != null && tokens == null) {
                // Undecryptable (key lost) – forget it.
                prefs.edit().remove(KEY_BLOB).commit()
            }
            loaded = true
            _hasSession.value = hasSession
        }
    }

    val accessToken: String?
        get() {
            ensureLoaded()
            return tokens?.accessToken
        }

    val refreshToken: String?
        get() {
            ensureLoaded()
            return tokens?.takeIf { it.refreshExpiresAt > System.currentTimeMillis() }?.refreshToken
        }

    val hasSession: Boolean
        get() = refreshToken != null

    /** True if the access token is missing or expires within [marginMs]. */
    fun accessExpiresWithin(marginMs: Long): Boolean {
        ensureLoaded()
        val t = tokens ?: return true
        return t.accessExpiresAt - System.currentTimeMillis() <= marginMs
    }

    /**
     * Saves a new pair. Uses a synchronous commit: a rotated refresh token
     * that's lost to a crash would force the user to sign in again.
     */
    fun save(pair: TokenPair) {
        val now = System.currentTimeMillis()
        val accessExpiresAt = jwtExpiryMillis(pair.accessToken)
            ?: (now + pair.accessTokenExpiresIn.coerceAtLeast(60) * 1000)
        val refreshExpiresAt = now + pair.refreshTokenExpiresIn.coerceAtLeast(60) * 1000
        val updated = Tokens(pair.accessToken, accessExpiresAt, pair.refreshToken, refreshExpiresAt)
        synchronized(lock) {
            val encrypted = KeystoreCipher.encrypt(json.encodeToString(Tokens.serializer(), updated))
            prefs.edit().putString(KEY_BLOB, encrypted).commit()
            tokens = updated
            loaded = true
            _hasSession.value = true
        }
    }

    fun clear() {
        synchronized(lock) {
            prefs.edit().remove(KEY_BLOB).commit()
            tokens = null
            loaded = true
            _hasSession.value = false
        }
    }

    /** Reads `exp` from a JWT payload, if it is one. */
    private fun jwtExpiryMillis(token: String): Long? = runCatching {
        val payload = token.split('.').getOrNull(1) ?: return null
        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val obj = json.parseToJsonElement(String(decoded, Charsets.UTF_8)) as? JsonObject
        (obj?.get("exp") as? JsonPrimitive)?.longOrNull?.times(1000)
    }.getOrNull()

    private companion object {
        const val PREFS_NAME = "auth_tokens"
        const val KEY_BLOB = "tokens_v1"
    }
}

/** AES-GCM with a non-exportable Android Keystore key. */
private object KeystoreCipher {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "short_rewards_auth_tokens"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_BITS = 128

    @Synchronized
    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val out = ByteArray(1 + iv.size + encrypted.size)
        out[0] = iv.size.toByte()
        System.arraycopy(iv, 0, out, 1, iv.size)
        System.arraycopy(encrypted, 0, out, 1 + iv.size, encrypted.size)
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String {
        val data = Base64.decode(encoded, Base64.NO_WRAP)
        val ivLength = data[0].toInt()
        val iv = data.copyOfRange(1, 1 + ivLength)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(data, 1 + ivLength, data.size - 1 - ivLength), Charsets.UTF_8)
    }
}
