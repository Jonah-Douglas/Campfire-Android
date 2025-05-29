package com.example.campfire.core.data.auth

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.campfire.core.security.EncryptedData
import com.example.campfire.core.security.IEncryptionManager
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton


private const val KEY_AUTH_TOKENS = "encrypted_auth_tokens"
private const val AUTH_TOKEN_ENCRYPTION_ALIAS = "auth_token_alias"

@Singleton
class AuthTokenStorage @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val encryptionManager: IEncryptionManager,
    private val gson: Gson
) : IAuthTokenManager {
    override fun saveTokens(tokens: AuthTokens) {
        val tokensJson = gson.toJson(tokens)
        val encryptedData = encryptionManager.encrypt(tokensJson, AUTH_TOKEN_ENCRYPTION_ALIAS)
        
        encryptedData?.let {
            // Store the Base64 encoded ciphertext and IV
            val storedValue = "${
                android.util.Base64.encodeToString(
                    it.ciphertext,
                    android.util.Base64.DEFAULT
                )
            }:${
                android.util.Base64.encodeToString(
                    it.initializationVector,
                    android.util.Base64.DEFAULT
                )
            }"
            
            sharedPreferences.edit { putString(KEY_AUTH_TOKENS, storedValue) }
        }
    }
    
    override fun getTokens(): AuthTokens? {
        val storedValue = sharedPreferences.getString(KEY_AUTH_TOKENS, null) ?: return null
        
        return try {
            val parts = storedValue.split(":")
            if (parts.size != 2) return null
            
            val ciphertext = android.util.Base64.decode(parts[0], android.util.Base64.DEFAULT)
            val iv = android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT)
            
            val decryptedJson = encryptionManager.decrypt(
                EncryptedData(ciphertext, iv),
                AUTH_TOKEN_ENCRYPTION_ALIAS
            )
            
            decryptedJson?.let {
                gson.fromJson(it, AuthTokens::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            clearTokens()
            null
        }
    }
    
    override fun clearTokens() {
        sharedPreferences.edit { remove(KEY_AUTH_TOKENS) }
    }
}