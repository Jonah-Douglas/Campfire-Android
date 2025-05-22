package com.example.campfire.core.data

import android.content.Context
import android.content.SharedPreferences
import com.example.campfire.core.data.auth.AuthTokenStorage
import com.example.campfire.core.data.auth.AuthTokens
import com.example.campfire.core.data.utils.decryptData
import com.example.campfire.core.data.utils.encryptData
import com.example.campfire.core.data.utils.getOrCreateSecretKey
import javax.crypto.SecretKey


internal const val PREFS_NAME = "auth_token_prefs"
internal const val PREF_KEY_ACCESS_TOKEN = "auth_access_token"
internal const val PREF_KEY_REFRESH_TOKEN = "auth_refresh_token"

class EncryptedAuthTokenStorage(context: Context) : AuthTokenStorage {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val secretKey: SecretKey = getOrCreateSecretKey()
    
    override suspend fun saveTokens(tokens: AuthTokens?) {
        if (tokens == null) {
            clearTokens()
            return
        }
        
        val encryptedAccessToken = encryptData(tokens.accessToken, secretKey)
        val encryptedRefreshToken = encryptData(tokens.refreshToken, secretKey)
        
        sharedPreferences.edit().apply {
            putString(PREF_KEY_ACCESS_TOKEN, encryptedAccessToken)
            putString(PREF_KEY_REFRESH_TOKEN, encryptedRefreshToken)
            apply()
        }
    }
    
    override suspend fun getTokens(): AuthTokens? {
        val encryptedAccessToken = sharedPreferences.getString(PREF_KEY_ACCESS_TOKEN, null)
        val encryptedRefreshToken = sharedPreferences.getString(PREF_KEY_REFRESH_TOKEN, null)
        
        // If both encrypted tokens from prefs are null, no stored tokens.
        if (encryptedAccessToken == null && encryptedRefreshToken == null) {
            return null
        }
        
        val accessToken = encryptedAccessToken?.let { decryptData(it, secretKey) }
        val refreshToken = encryptedRefreshToken?.let { decryptData(it, secretKey) }
        
        if (accessToken == null && refreshToken == null) {
            return null
        }
        
        return AuthTokens(accessToken, refreshToken)
    }
    
    override suspend fun clearTokens() {
        sharedPreferences.edit().apply {
            remove(PREF_KEY_ACCESS_TOKEN)
            remove(PREF_KEY_REFRESH_TOKEN)
            apply()
        }
    }
}