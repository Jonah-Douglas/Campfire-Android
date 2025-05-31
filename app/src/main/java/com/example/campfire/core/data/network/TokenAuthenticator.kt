package com.example.campfire.core.data.network

import android.util.Log
import com.example.campfire.auth.data.remote.TokenRefreshApiService
import com.example.campfire.auth.data.remote.dto.request.RefreshTokenRequest
import com.example.campfire.core.data.auth.AuthTokenStorage
import com.example.campfire.core.data.auth.AuthTokens
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton


private const val MAX_RETRIES = 2

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStorage: AuthTokenStorage,
    private val tokenRefreshApiService: dagger.Lazy<TokenRefreshApiService>
) : Authenticator {
    
    override fun authenticate(route: Route?, response: Response): Request? {
        // 1. Check if we should even attempt (e.g., already tried, or request didn't have token)
        val originalAccessTokenFromRequest =
            response.request.header("Authorization")?.substringAfter("Bearer ")
        
        if (responseCount(response) >= MAX_RETRIES || originalAccessTokenFromRequest == null) {
            return null
        }
        
        // 2. Get the current tokens (synchronously)
        val currentTokens = runBlocking { tokenStorage.getTokens() }
        val currentRefreshToken = currentTokens?.refreshToken
        
        if (currentRefreshToken == null) {
            // JD TODO: Add logout call here (user then needs to login to receive new access and refresh token)
            Log.w("TokenAuthenticator", "No refresh token found. Clearing tokens.")
            runBlocking { tokenStorage.clearTokens() }
            return null
        }
        
        // 3. Synchronized block to prevent multiple concurrent refresh attempts
        synchronized(this) {
            // Check if another thread already refreshed the token while we were waiting for the lock
            // Compare the access token currently in storage with the one from the failing request.
            // If they are different, it means a refresh likely already happened.
            val potentiallyNewTokensAfterLock = runBlocking { tokenStorage.getTokens() }
            val newAccessTokenAfterLock = potentiallyNewTokensAfterLock?.accessToken
            
            if (newAccessTokenAfterLock != null && newAccessTokenAfterLock != originalAccessTokenFromRequest) {
                Log.d("TokenAuthenticator", "Token refresh already in progress.")
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessTokenAfterLock")
                    .build()
            }
            
            // If the access token in storage is STILL the one that failed, proceed with refresh.
            // This also handles the case where newAccessTokenAfterLock is null, meaning we still need to refresh.
            
            Log.d("TokenAuthenticator", "Attempting token refresh.")
            // 4. Perform the token refresh (synchronously)
            try {
                // Ensure the refresh token used here is the one we fetched before the lock,
                // or re-fetch if there's a concern it might have been cleared by another process.
                // Using currentRefreshToken fetched before the synchronized block is generally fine.
                val refreshCall = tokenRefreshApiService.get() // Get instance from Lazy
                    .refreshAuthToken(RefreshTokenRequest(refreshToken = currentRefreshToken))
                
                val refreshAPIResponse = refreshCall.execute() // Synchronous execution
                
                if (refreshAPIResponse.isSuccessful && refreshAPIResponse.body() != null) {
                    val newTokens = refreshAPIResponse.body()!!
                    Log.i("TokenAuthenticator", "Token refresh successful.")
                    
                    // Persist new tokens (synchronously or via runBlocking if storage is suspend)
                    runBlocking {
                        tokenStorage.saveTokens(
                            AuthTokens(
                                accessToken = newTokens.accessToken,
                                // Use new refresh token if provided by API, otherwise keep existing
                                refreshToken = newTokens.refreshToken ?: currentRefreshToken
                            )
                        )
                    }
                    
                    // 5. Retry the original request with the new token
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.accessToken}")
                        .build()
                } else {
                    // Refresh failed (e.g., invalid refresh token)
                    Log.e(
                        "TokenAuthenticator",
                        "Token refresh failed. Code: ${refreshAPIResponse.code()}, Message: ${refreshAPIResponse.message()}"
                    )
                    if (refreshAPIResponse.code() == 401 || refreshAPIResponse.code() == 403) {
                        Log.w("TokenAuthenticator", "Refresh token rejected. Clearing tokens.")
                        runBlocking { tokenStorage.clearTokens() }
                        // Consider emitting an event for UI to react (e.g., navigate to login)
                    }
                    
                    return null
                }
            } catch (e: Exception) {
                // Network error during refresh or other exception, do not retry request
                Log.w("TokenAuthenticator", "Refresh token rejected. Clearing tokens.")
                return null
            }
        }
    }
    
    private fun responseCount(response: Response): Int {
        var currentResponse = response
        var result = 1
        while (currentResponse.priorResponse != null) {
            currentResponse = currentResponse.priorResponse!!
            result++
        }
        
        return result
    }
}