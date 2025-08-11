package com.example.campfire.core.data.auth

import android.content.SharedPreferences
import androidx.core.content.edit
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


private const val KEY_AUTH_TOKENS = "encrypted_auth_tokens"
private const val AUTH_TOKEN_ENCRYPTION_ALIAS = "auth_token_alias"

data class StoredEncryptedTokens(val ciphertext: String, val iv: String)

@Singleton
class AuthTokenStorage @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val encryptionManager: IEncryptionManager,
    private val gson: Gson,
    @Named("ApplicationScope") private val applicationScope: CoroutineScope
) : IAuthTokenManager {
    
    // In-memory cache, potentially reactive
    private val _cachedTokens = MutableStateFlow<AuthTokens?>(null)
    
    init {
        // Load initial tokens into cache when AuthTokenStorage is created
        applicationScope.launch {
            _cachedTokens.value = loadTokensFromPersistence()
        }
    }
    
    private suspend fun loadTokensFromPersistence(): AuthTokens? {
        return withContext(Dispatchers.IO) {
            try {
                val storedJson =
                    sharedPreferences.getString(KEY_AUTH_TOKENS, null) ?: return@withContext null
                val storable = gson.fromJson(storedJson, StoredEncryptedTokens::class.java)
                
                val ciphertext =
                    android.util.Base64.decode(storable.ciphertext, android.util.Base64.DEFAULT)
                val iv = android.util.Base64.decode(storable.iv, android.util.Base64.DEFAULT)
                
                val decryptedJson = encryptionManager.decrypt(
                    EncryptedData(ciphertext, iv),
                    AUTH_TOKEN_ENCRYPTION_ALIAS
                )
                
                decryptedJson?.let {
                    gson.fromJson(it, AuthTokens::class.java)
                }
            } catch (e: Exception) {
                // JD TODO: replace this with proper logging (maybe Timber?)
                e.printStackTrace()
                clearTokens()
                null
            }
        }
    }
    
    override suspend fun saveTokens(tokens: AuthTokens) {
        withContext(Dispatchers.IO) {
            val tokensJson = gson.toJson(tokens)
            val encryptedData = encryptionManager.encrypt(tokensJson, AUTH_TOKEN_ENCRYPTION_ALIAS)
            
            encryptedData?.let {
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
                sharedPreferences.edit { putString(KEY_AUTH_TOKENS, storedJson) }
            }
        }
        
        _cachedTokens.value = tokens
    }
    
    override suspend fun getTokens(): AuthTokens? {
        val current = _cachedTokens.value
        if (current == null) {
            val loaded = loadTokensFromPersistence()
            _cachedTokens.value = loaded
            return loaded
        }
        
        return current
    }
    
    override suspend fun clearTokens() {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit { remove(KEY_AUTH_TOKENS) }
        }
        
        _cachedTokens.value = null
    }
    
    override fun getCurrentTokens(): AuthTokens? {
        return _cachedTokens.value
    }
}