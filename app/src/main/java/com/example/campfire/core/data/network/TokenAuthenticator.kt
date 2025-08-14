package com.example.campfire.core.data.network

import com.example.campfire.auth.data.remote.TokenRefreshAPIService
import com.example.campfire.auth.data.remote.dto.request.RefreshTokenRequest
import com.example.campfire.auth.data.remote.dto.response.ApiResponse
import com.example.campfire.auth.data.remote.dto.response.RefreshedTokensResponse
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.data.auth.AuthTokens
import com.example.campfire.core.data.auth.IAuthTokenManager
import com.example.campfire.core.domain.SessionInvalidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


private const val MAX_RETRIES = 2

@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionInvalidatorProvider: dagger.Lazy<SessionInvalidator>,
    private val tokenManager: IAuthTokenManager,
    private val tokenRefreshAPIServiceLazy: dagger.Lazy<TokenRefreshAPIService>
) : Authenticator {
    
    private val authenticatorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val sessionInvalidator: SessionInvalidator
        get() = sessionInvalidatorProvider.get()
    
    private val tokenRefreshApiService: TokenRefreshAPIService
        get() = tokenRefreshAPIServiceLazy.get()
    
    override fun authenticate(route: Route?, response: Response): Request? {
        Firelog.d(String.format(LOG_AUTH_REQUIRED, response.request.url))
        
        val originalAccessTokenFromRequest =
            response.request.header(AUTHORIZATION)?.substringAfter(BEARER)
        
        // 1. Check if we should even attempt (e.g., already tried, or request didn't have token)
        if (responseCount(response) >= MAX_RETRIES) {
            Firelog.w(String.format(LOG_MAX_RETRIES, response.request.url))
            // Consider triggering session invalidation if retries fail consistently with a valid token attempt
            if (originalAccessTokenFromRequest != null) {
                triggerSessionInvalidation(LOG_MAX_RETRIES_REACHED)
            }
            return null // Do not retry
        }
        
        // If the original request didn't even have an access token,
        // and it's a 401, this authenticator shouldn't handle it.
        if (originalAccessTokenFromRequest == null) {
            Firelog.d(LOG_MISSING_ACCESS_TOKEN)
            return null
        }
        
        // 2. Get the current tokens (synchronously)
        val currentTokens = runBlocking { tokenManager.getTokens() }
        val currentRefreshToken = currentTokens?.refreshToken
        
        if (currentRefreshToken == null) {
            Firelog.w(LOG_INVALIDATE_SESSION)
            triggerSessionInvalidation(LOG_MISSING_REFRESH_TOKEN)
            return null
        }
        
        // 3. Synchronized block to prevent multiple concurrent refresh attempts for the *same* bad token
        synchronized(this) {
            // Check if another thread already refreshed the token while we were waiting for the lock
            // Compare the access token currently in storage with the one from the failing request.
            // If they are different, it means a refresh likely already happened.
            val tokensAfterLock = runBlocking { tokenManager.getTokens() }
            val accessTokenInStorageAfterLock = tokensAfterLock?.accessToken
            
            // If a token exists in storage AND it's different from the one that caused the 401,
            // it means another thread likely succeeded in refreshing it.
            if (accessTokenInStorageAfterLock != null && accessTokenInStorageAfterLock != originalAccessTokenFromRequest) {
                Firelog.d(LOG_TOKEN_ALREADY_REFRESHED)
                return response.request.newBuilder()
                    .header(AUTHORIZATION, "Bearer $accessTokenInStorageAfterLock")
                    .build()
            }
            
            // If accessTokenInStorageAfterLock is null (tokens were cleared) or still matches the failing one, proceed.
            Firelog.d(String.format(LOG_ATTEMPTING_TOKEN_REFRESH, currentRefreshToken.takeLast(6)))
            try {
                // 4. Perform the token refresh (synchronously)
                val refreshCall = tokenRefreshApiService
                    .refreshAuthToken(RefreshTokenRequest(refreshToken = currentRefreshToken))
                
                val refreshAPIResponse: retrofit2.Response<ApiResponse<RefreshedTokensResponse>>
                try {
                    refreshAPIResponse = refreshCall.execute()
                } catch (e: IOException) {
                    Firelog.e(String.format(LOG_NETWORK_IO_EXCEPTION, e.message), e)
                    return null
                }
                
                if (refreshAPIResponse.isSuccessful) {
                    val newAPITokens = refreshAPIResponse.body()?.data
                    if (newAPITokens?.accessToken == null) {
                        Firelog.e(LOG_RESPONSE_BODY_OR_TOKEN_NULL)
                        triggerSessionInvalidation(LOG_REFRESH_NO_TOKEN)
                        runBlocking { tokenManager.clearTokens() } // Clear potentially stale tokens
                        return null
                    }
                    Firelog.i(LOG_REFRESH_SUCCESS)
                    
                    // Persist new tokens
                    runBlocking {
                        tokenManager.saveTokens(
                            AuthTokens(
                                accessToken = newAPITokens.accessToken,
                                // Use new refresh token if provided by API, otherwise keep existing
                                refreshToken = newAPITokens.refreshToken ?: currentRefreshToken
                            )
                        )
                    }
                    
                    // 5. Retry the original request with the new token
                    return response.request.newBuilder()
                        .header(AUTHORIZATION, "Bearer ${newAPITokens.accessToken}")
                        .build()
                } else {
                    // Refresh failed (e.g., invalid refresh token)
                    Firelog.e(
                        String.format(
                            LOG_REFRESH_FAILED,
                            refreshAPIResponse.code(),
                            refreshAPIResponse.message()
                        )
                    )
                    
                    if (refreshAPIResponse.code() == 401 || refreshAPIResponse.code() == 403 || refreshAPIResponse.code() == 400) {
                        Firelog.w(LOG_SERVER_REJECTED)
                        runBlocking { tokenManager.clearTokens() }
                        triggerSessionInvalidation(LOG_TOKEN_REJECTED)
                    }
                    
                    return null
                }
            } catch (e: Exception) {
                // Network error during refresh or other exception, do not retry request
                Firelog.e(String.format(LOG_UNEXPECTED_TOKEN, e.message), e)
                triggerSessionInvalidation(String.format(LOG_REFRESH_EXCEPTION, e.message))
                runBlocking { tokenManager.clearTokens() }
                return null
            }
        }
    }
    
    private fun triggerSessionInvalidation(reason: String) {
        Firelog.i(String.format(LOG_TRIGGER_SESSION_INVALID, reason))
        authenticatorScope.launch {
            try {
                sessionInvalidator.invalidateSessionAndTriggerLogout()
                Firelog.i(LOG_SESSION_INVALIDATED)
            } catch (e: Exception) {
                Firelog.e(String.format(LOG_INVALIDATE_SESSION_FAILED, e.message), e)
                
                // Fallback: Cleared tokens directly via tokenManager after session invalidator failure
                tokenManager.clearTokens()
                Firelog.w(LOG_FALLBACK_CLEAR)
            }
        }
    }
    
    
    private fun responseCount(response: Response): Int {
        var currentResponse = response
        var result = 1
        while (currentResponse.priorResponse != null) {
            currentResponse = currentResponse.priorResponse!!
            result++
            
            if (result > MAX_RETRIES + 5) { // Safety break for extreme chains
                Firelog.e(String.format(LOG_OVER_RETRY_LIMIT, result))
                return result
            }
        }
        
        return result
    }
    
    companion object {
        private const val AUTHORIZATION = "Authorization"
        private const val BEARER = "Bearer "
        
        // --- Authenticate ---
        private const val LOG_AUTH_REQUIRED =
            "Authentication required for: '%s'"
        private const val LOG_MAX_RETRIES =
            "Max retries reached for '%s'"
        private const val LOG_MAX_RETRIES_REACHED =
            "Max retries reached."
        private const val LOG_MISSING_ACCESS_TOKEN =
            "Original request did not contain an access token. Not attempting refresh."
        private const val LOG_INVALIDATE_SESSION =
            "No refresh token available. Triggering session invalidation."
        private const val LOG_MISSING_REFRESH_TOKEN =
            "No refresh token."
        private const val LOG_TOKEN_ALREADY_REFRESHED =
            "Token was already refreshed by another thread. Using new token from storage."
        private const val LOG_ATTEMPTING_TOKEN_REFRESH =
            "Attempting token refresh for token ending with: ...'%s'"
        private const val LOG_NETWORK_IO_EXCEPTION =
            "Network IOException during token refresh: '%s'"
        private const val LOG_RESPONSE_BODY_OR_TOKEN_NULL =
            "Token refresh API call successful but response body or new access token is null."
        private const val LOG_REFRESH_NO_TOKEN =
            "Refresh API success but no new token."
        private const val LOG_REFRESH_SUCCESS =
            "Token refresh successful."
        private const val LOG_REFRESH_FAILED =
            "Token refresh API call failed. Code: '%s', Message: '%s'"
        private const val LOG_SERVER_REJECTED =
            "Refresh token rejected by server. Clearing tokens and invalidating session."
        private const val LOG_TOKEN_REJECTED =
            "Refresh token rejected."
        private const val LOG_UNEXPECTED_TOKEN =
            "Unexpected exception during token refresh attempt: '%s'"
        private const val LOG_REFRESH_EXCEPTION =
            "Exception during refresh: '%s'"
        
        // --- Trigger Session Invalidation ---
        private const val LOG_TRIGGER_SESSION_INVALID =
            "Triggering session invalidation due to: '%s'"
        private const val LOG_SESSION_INVALIDATED =
            "Session invalidation process completed."
        private const val LOG_INVALIDATE_SESSION_FAILED =
            "Failed to trigger session invalidation: '%s'"
        private const val LOG_FALLBACK_CLEAR =
            "Fallback: Cleared tokens directly via tokenManager after session invalidator failure."
        
        // --- Response Count ---
        private const val LOG_OVER_RETRY_LIMIT =
            "Excessive prior responses, breaking count: '%s'"
    }
}