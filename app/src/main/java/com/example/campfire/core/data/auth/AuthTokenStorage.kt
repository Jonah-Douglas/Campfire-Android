package com.example.campfire.core.data.auth

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.security.EncryptedData
import com.example.campfire.core.security.IEncryptionManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton


/**
 * Key used to store the encrypted [AuthTokens] in SharedPreferences.
 */
private const val KEY_AUTH_TOKENS = "encrypted_auth_tokens"

/**
 * Alias used for encryption and decryption operations for the auth tokens.
 * This alias typically refers to a key in the Android Keystore.
 */
private const val AUTH_TOKEN_ENCRYPTION_ALIAS = "auth_token_alias"

/**
 * Data class representing the structure of how encrypted tokens are stored as a JSON string.
 * This class holds the Base64 encoded ciphertext and initialization vector (IV).
 *
 * @property ciphertext The Base64 encoded encrypted token data.
 * @property iv The Base64 encoded initialization vector used for encryption.
 */
data class StoredEncryptedTokens(val ciphertext: String, val iv: String)


/**
 * Manages the secure storage and retrieval of [AuthTokens] using [SharedPreferences]
 * for persistence, with encryption handled by [IEncryptionManager].
 *
 * This class implements [IAuthTokenManager] and maintains an in-memory cache
 * ([_cachedTokens]) of the tokens for quick synchronous access and to serve as a
 * reactive source if needed. Tokens are loaded into the cache upon initialization
 * and kept synchronized with persistence operations.
 *
 * It is a [Singleton] to ensure a single source of truth for token management
 * throughout the application.
 *
 * @param sharedPreferences The [SharedPreferences] instance for storing encrypted tokens.
 * @param encryptionManager The [IEncryptionManager] instance for encrypting and decrypting token data.
 * @param gson The [Gson] instance for serializing and deserializing token objects.
 * @param applicationScope A [CoroutineScope] tied to the application's lifecycle,
 *                         used for launching background tasks like initial token loading.
 */
@Singleton
class AuthTokenStorage @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val encryptionManager: IEncryptionManager,
    private val gson: Gson,
    @Named("ApplicationScope") private val applicationScope: CoroutineScope
) : IAuthTokenManager {
    
    /**
     * In-memory cache for the [AuthTokens].
     * This allows for quick synchronous access via [getCurrentTokens] and can serve
     * as a reactive data source if exposed as a Flow.
     * It is initialized by attempting to load tokens from persistence.
     */
    private val _cachedTokens = MutableStateFlow<AuthTokens?>(null)
    
    init {
        Firelog.i("Initializing AuthTokenStorage. Attempting to load tokens from persistence...")
        
        // Load initial tokens into cache when AuthTokenStorage is created
        applicationScope.launch {
            _cachedTokens.value = loadTokensFromPersistence()
            Firelog.d("Initial token load complete. Cached tokens populated: ${_cachedTokens.value != null}")
        }
    }
    
    /**
     * Loads and decrypts [AuthTokens] from [SharedPreferences].
     * This operation is performed on the IO dispatcher.
     *
     * In case of any error during loading or decryption (e.g., data corruption,
     * decryption failure), it logs the error, clears any potentially corrupted
     * tokens from persistence, and returns null.
     *
     * @return The decrypted [AuthTokens] if successfully loaded, otherwise null.
     */
    private suspend fun loadTokensFromPersistence(): AuthTokens? {
        Firelog.d("loadTokensFromPersistence called.")
        return withContext(Dispatchers.IO) {
            try {
                val storedJson =
                    sharedPreferences.getString(KEY_AUTH_TOKENS, null) ?: run {
                        Firelog.d("No stored tokens found in SharedPreferences for key: $KEY_AUTH_TOKENS")
                        return@withContext null
                    }
                Firelog.v("Found stored encrypted tokens JSON: $storedJson")
                val storable = gson.fromJson(storedJson, StoredEncryptedTokens::class.java)
                
                val ciphertext =
                    android.util.Base64.decode(storable.ciphertext, android.util.Base64.DEFAULT)
                val iv = android.util.Base64.decode(storable.iv, android.util.Base64.DEFAULT)
                
                Firelog.d("Attempting to decrypt tokens using alias: $AUTH_TOKEN_ENCRYPTION_ALIAS")
                val decryptedJson = encryptionManager.decrypt(
                    EncryptedData(ciphertext, iv),
                    AUTH_TOKEN_ENCRYPTION_ALIAS
                )
                
                decryptedJson?.let {
                    Firelog.d("Tokens decrypted successfully.")
                    Firelog.v("Decrypted JSON: $it")
                    gson.fromJson(it, AuthTokens::class.java)
                } ?: run {
                    Firelog.w("Failed to decrypt tokens. Decrypted JSON was null.")
                    null
                }
            } catch (e: Exception) {
                Firelog.e(
                    "Error loading/decrypting tokens from persistence. Clearing potentially corrupt data.",
                    e
                )
                clearTokens()
                null
            }
        }
    }
    
    /**
     * Encrypts and saves the provided [AuthTokens] to [SharedPreferences]
     * and updates the in-memory cache.
     * This operation is performed on the IO dispatcher.
     *
     * If encryption fails, tokens are not saved to persistence, but the
     * cache might still be updated based on the input tokens if the logic allows.
     * (Current implementation: cache is updated regardless of persistence success)
     * It's recommended to log details about the success or failure of encryption and saving.
     * Raw token values should NOT be logged.
     *
     * @param tokens The [AuthTokens] to be saved.
     */
    override suspend fun saveTokens(tokens: AuthTokens) {
        Firelog.i("saveTokens called. HasAccessToken: ${tokens.accessToken != null}, HasRefreshToken: ${tokens.refreshToken != null}")
        withContext(Dispatchers.IO) {
            Firelog.d("Serializing tokens to JSON.")
            val tokensJson = gson.toJson(tokens)
            Firelog.v("Serialized tokens JSON: $tokensJson")
            
            Firelog.d("Encrypting tokens JSON using alias: $AUTH_TOKEN_ENCRYPTION_ALIAS")
            val encryptedData = encryptionManager.encrypt(tokensJson, AUTH_TOKEN_ENCRYPTION_ALIAS)
            
            encryptedData?.let {
                Firelog.d("Tokens encrypted successfully. Preparing to store.")
                val storable = StoredEncryptedTokens(
                    ciphertext = android.util.Base64.encodeToString(
                        it.ciphertext,
                        android.util.Base64.DEFAULT
                    ),
                    iv = android.util.Base64.encodeToString(
                        it.initializationVector,
                        android.util.Base64.DEFAULT
                    )
                )
                
                val storedJson = gson.toJson(storable)
                Firelog.v("Storing encrypted tokens JSON: $storedJson")
                sharedPreferences.edit { putString(KEY_AUTH_TOKENS, storedJson) }
                Firelog.i("Tokens saved to SharedPreferences successfully.")
            } ?: run {
                Firelog.e("Failed to encrypt tokens. Tokens will not be persisted.")
            }
        }
        
        Firelog.d("Updating in-memory cache with new tokens.")
        _cachedTokens.value = tokens
    }
    
    /**
     * Retrieves [AuthTokens]. It first checks the in-memory cache.
     * If the cache is empty, it attempts to load tokens from persistence.
     * The result is then stored in the cache for subsequent accesses.
     *
     * @return The [AuthTokens] if found in cache or successfully loaded from
     *         persistence, otherwise null.
     */
    override suspend fun getTokens(): AuthTokens? {
        Firelog.d("getTokens called.")
        val current = _cachedTokens.value
        if (current == null) {
            Firelog.d("Cache miss for getTokens. Loading from persistence.")
            val loaded = loadTokensFromPersistence()
            _cachedTokens.value = loaded
            Firelog.i("Tokens loaded from persistence for getTokens. Cached: ${loaded != null}")
            return loaded
        }
        
        Firelog.d("Cache hit for getTokens. Returning cached tokens.")
        return current
    }
    
    /**
     * Clears [AuthTokens] from both [SharedPreferences] and the in-memory cache.
     * This operation is performed on the IO dispatcher for persistence.
     * Typically used during logout.
     */
    override suspend fun clearTokens() {
        Firelog.i("clearTokens called.")
        withContext(Dispatchers.IO) {
            sharedPreferences.edit { remove(KEY_AUTH_TOKENS) }
            Firelog.d("Tokens cleared from SharedPreferences.")
        }
        
        _cachedTokens.value = null
        Firelog.d("In-memory token cache cleared.")
    }
    
    /**
     * Synchronously retrieves the currently cached [AuthTokens].
     * This method directly returns the value from the in-memory cache [_cachedTokens]
     * and does not perform any I/O operations, making it suitable for contexts
     * like OkHttp Interceptors.
     *
     * The cache is populated upon class initialization and updated by
     * [saveTokens], [getTokens] (on cache miss), and [clearTokens].
     *
     * @return The currently cached [AuthTokens], or null if no tokens are cached.
     */
    override fun getCurrentTokens(): AuthTokens? {
        Firelog.d("getCurrentTokens (synchronous cache access) called. Cached: ${_cachedTokens.value != null}")
        return _cachedTokens.value
    }
}