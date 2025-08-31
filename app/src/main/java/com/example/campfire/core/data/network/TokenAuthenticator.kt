package com.example.campfire.core.data.network

import com.example.campfire.auth.data.local.AuthTokens
import com.example.campfire.auth.data.local.IAuthTokenManager
import com.example.campfire.auth.data.remote.TokenRefreshAPIService
import com.example.campfire.auth.data.remote.dto.request.RefreshTokenRequest
import com.example.campfire.auth.data.remote.dto.response.RefreshedTokensResponse
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.data.network.TokenAuthenticator.Companion.MAX_RETRIES
import com.example.campfire.core.data.remote.dto.response.APIResponse
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


/**
 * An OkHttp [Authenticator] that handles automatic refreshing of authentication tokens.
 *
 * When a request fails with an authentication error (typically a 401 Unauthorized),
 * this authenticator attempts to refresh the access token using the current refresh token.
 *
 * Key responsibilities and behaviors:
 * - **Token Refresh:** Uses [TokenRefreshAPIService] to obtain new tokens.
 * - **Synchronization:** Employs a `synchronized` block to prevent multiple concurrent
 *   refresh attempts for the *same* failing access token, reducing server load and race conditions.
 *   It checks if another thread has already refreshed the token before making a new API call.
 * - **Retry Limits:** Limits the number of refresh attempts per original request ([MAX_RETRIES]).
 * - **Session Invalidation:** If token refresh fails persistently, or if no refresh token is available,
 *   it triggers session invalidation via [SessionInvalidator] to log the user out.
 * - **Token Persistence:** Saves newly acquired tokens using [IAuthTokenManager].
 * - **Preconditions:**
 *     - Only attempts refresh if the original failing request had an Authorization header.
 *     - Only attempts refresh if a refresh token is available.
 * - **Error Handling:** Manages various failure scenarios during the refresh process,
 *   including network errors, API errors, and unexpected exceptions.
 *
 * The token refresh process is performed synchronously within the `authenticate` method
 * (using `runBlocking` for coroutine-based dependencies like [IAuthTokenManager] and
 * `execute()` for the Retrofit call) as required by OkHttp's [Authenticator] interface.
 * Session invalidation is launched asynchronously on a dedicated [CoroutineScope].
 *
 * @property sessionInvalidatorProvider A Dagger Lazy provider for [SessionInvalidator]
 *                                      to handle session termination.
 * @property tokenManager An instance of [IAuthTokenManager] for accessing and storing tokens.
 * @property tokenRefreshAPIServiceLazy A Dagger Lazy provider for [TokenRefreshAPIService]
 *                                      to perform the token refresh API call.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionInvalidatorProvider: dagger.Lazy<SessionInvalidator>,
    private val tokenManager: IAuthTokenManager,
    private val tokenRefreshAPIServiceLazy: dagger.Lazy<TokenRefreshAPIService>
) : Authenticator {
    
    
    /**
     * Coroutine scope for launching asynchronous operations like session invalidation,
     * independent of the synchronous nature of the `authenticate` method.
     * Uses [Dispatchers.IO] and a [SupervisorJob] to prevent failures in one
     * launched job from canceling the scope.
     */
    private val authenticatorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Lazily gets an instance of [SessionInvalidator].
     */
    private val sessionInvalidator: SessionInvalidator
        get() = sessionInvalidatorProvider.get()
    
    /**
     * Lazily gets an instance of [TokenRefreshAPIService].
     */
    private val tokenRefreshApiService: TokenRefreshAPIService
        get() = tokenRefreshAPIServiceLazy.get()
    
    /**
     * Called by OkHttp when a request receives an authentication challenge (e.g., a 401 response).
     * This method attempts to refresh the authentication token and retry the original request.
     *
     * The process involves:
     * 1. Checking preconditions: Max retries, presence of original access token, availability of refresh token.
     * 2. Synchronizing to prevent concurrent refreshes for the same outdated token.
     * 3. Checking if another thread already refreshed the token while waiting for the lock.
     * 4. Performing the token refresh API call synchronously.
     * 5. If successful, saving the new tokens and building a new request with the new access token.
     * 6. If unsuccessful, or if preconditions are not met, triggering session invalidation
     *    and returning null (no retry).
     *
     * @param route The route that resulted in the authentication challenge, may be null.
     * @param response The [Response] that triggered the authentication challenge.
     * @return A new [Request] with the refreshed token to retry the original request,
     *         or `null` if the challenge cannot be satisfied (e.g., refresh failed,
     *         no refresh token, max retries exceeded). Returning `null` means no
     *         further attempts will be made for this request by this authenticator.
     */
    override fun authenticate(route: Route?, response: Response): Request? {
        Firelog.d(String.format(LOG_AUTH_REQUIRED, response.request.url))
        
        val originalAccessTokenFromRequest =
            response.request.header(AUTHORIZATION)?.substringAfter(BEARER)
        
        // 1. Precondition checks
        if (responseCount(response) >= MAX_RETRIES) {
            Firelog.w(String.format(LOG_MAX_RETRIES, response.request.url))
            if (originalAccessTokenFromRequest != null) {
                triggerSessionInvalidation(LOG_MAX_RETRIES_REACHED)
            }
            return null
        }
        
        if (originalAccessTokenFromRequest == null) {
            Firelog.d(LOG_MISSING_ACCESS_TOKEN)
            return null
        }
        
        val currentTokens =
            runBlocking { tokenManager.getTokens() } // Blocking as per OkHttp Authenticator needs
        val currentRefreshToken = currentTokens?.refreshToken
        
        if (currentRefreshToken == null) {
            Firelog.w(LOG_INVALIDATE_SESSION)
            triggerSessionInvalidation(LOG_MISSING_REFRESH_TOKEN)
            return null
        }
        
        // 2. Synchronization and re-check
        synchronized(this) {
            // Check if token was refreshed by another thread while waiting for this lock
            val tokensAfterLock = runBlocking { tokenManager.getTokens() }
            val accessTokenInStorageAfterLock = tokensAfterLock?.accessToken
            
            if (accessTokenInStorageAfterLock != null && accessTokenInStorageAfterLock != originalAccessTokenFromRequest) {
                Firelog.d(LOG_TOKEN_ALREADY_REFRESHED)
                return response.request.newBuilder()
                    .header(AUTHORIZATION, "Bearer $accessTokenInStorageAfterLock")
                    .build()
            }
            // Proceed with refresh attempt if token is still the same or null
            
            Firelog.d(String.format(LOG_ATTEMPTING_TOKEN_REFRESH, currentRefreshToken.takeLast(6)))
            try {
                // 3. Perform token refresh (synchronous Retrofit call)
                val refreshCall = tokenRefreshApiService
                    .refreshAuthToken(RefreshTokenRequest(refreshToken = currentRefreshToken))
                
                val refreshAPIResponse: retrofit2.Response<APIResponse<RefreshedTokensResponse>>
                try {
                    refreshAPIResponse = refreshCall.execute()
                } catch (e: IOException) {
                    Firelog.e(String.format(LOG_NETWORK_IO_EXCEPTION, e.message), e)
                    // JD TODO: Maybe invalidate session at this point
                    return null
                }
                
                // 4. Process refresh response
                if (refreshAPIResponse.isSuccessful) {
                    val newAPITokens = refreshAPIResponse.body()?.data
                    if (newAPITokens?.accessToken == null) {
                        Firelog.e(LOG_RESPONSE_BODY_OR_TOKEN_NULL)
                        triggerSessionInvalidation(LOG_REFRESH_NO_TOKEN)
                        runBlocking { tokenManager.clearTokens() }
                        return null
                    }
                    Firelog.i(LOG_REFRESH_SUCCESS)
                    
                    runBlocking { // Persist new tokens
                        tokenManager.saveTokens(
                            AuthTokens(
                                accessToken = newAPITokens.accessToken,
                                refreshToken = newAPITokens.refreshToken ?: currentRefreshToken
                            )
                        )
                    }
                    
                    // 5. Retry the original request with new token
                    return response.request.newBuilder()
                        .header(AUTHORIZATION, "Bearer ${newAPITokens.accessToken}")
                        .build()
                } else {
                    Firelog.e(
                        String.format(
                            LOG_REFRESH_FAILED,
                            refreshAPIResponse.code(),
                            refreshAPIResponse.message()
                        )
                    )
                    
                    // Handle specific error codes that indicate an invalid refresh token
                    if (refreshAPIResponse.code() == 401 || refreshAPIResponse.code() == 403 || refreshAPIResponse.code() == 400) {
                        Firelog.w(LOG_SERVER_REJECTED)
                        runBlocking { tokenManager.clearTokens() }
                        triggerSessionInvalidation(LOG_TOKEN_REJECTED)
                    }
                    
                    return null // Refresh failed, do not retry
                }
            } catch (e: Exception) {
                Firelog.e(String.format(LOG_UNEXPECTED_TOKEN, e.message), e)
                triggerSessionInvalidation(String.format(LOG_REFRESH_EXCEPTION, e.message))
                runBlocking { tokenManager.clearTokens() } // Ensure tokens are cleared
                return null
            }
        }
    }
    
    /**
     * Triggers the session invalidation process asynchronously.
     * This typically involves clearing stored tokens and navigating the user to a login screen.
     * If the primary session invalidation mechanism fails, it attempts a fallback
     * to clear tokens directly via [tokenManager].
     *
     * @param reason A string describing why the session invalidation is being triggered, for logging.
     */
    private fun triggerSessionInvalidation(reason: String) {
        Firelog.i(String.format(LOG_TRIGGER_SESSION_INVALID, reason))
        authenticatorScope.launch {
            try {
                sessionInvalidator.invalidateSessionAndTriggerLogout()
                Firelog.i(LOG_SESSION_INVALIDATED)
            } catch (e: Exception) {
                Firelog.e(String.format(LOG_INVALIDATE_SESSION_FAILED, e.message), e)
                // Fallback: Clear tokens directly if SessionInvalidator fails
                tokenManager.clearTokens()
                Firelog.w(LOG_FALLBACK_CLEAR)
            }
        }
    }
    
    /**
     * Counts the number of times the request has been retried by traversing `priorResponse`.
     * Includes a safety break to prevent infinite loops in case of misconfigured response chains.
     *
     * @param response The current response being evaluated.
     * @return The number of prior responses (i.e., retry attempts for this logical request).
     *         Starts at 1 for the first time this authenticator sees the response.
     */
    private fun responseCount(response: Response): Int {
        var currentResponse = response
        var result = 1 // The current response is the first attempt
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
    
    /**
     * Companion object holding constants for header names and log messages.
     * Using constants for log messages helps in maintaining consistency and
     * potentially localizing or analyzing logs more easily.
     */
    companion object {
        private const val MAX_RETRIES = 2
        
        // HTTP Header related constants
        private const val AUTHORIZATION = "Authorization"
        private const val BEARER = "Bearer "
        
        // Log message constants grouped by the method they relate to.
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