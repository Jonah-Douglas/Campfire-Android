package com.example.campfire.auth.data.local


/**
 * Represents the authentication tokens used by the application.
 * These tokens are typically obtained after a successful login or user
 * creation event and are used to authorize subsequent API requests.
 *
 * @property accessToken The token used to authenticate API requests.
 *                       Short-lived and sent with each protected request.
 *                       Null if no access token is available or the user is not authenticated.
 * @property refreshToken The token used to obtain a new [accessToken] when it expires,
 *                        without requiring the user to log in again.
 *                        Longer-lived than the access token.
 *                        Null if no refresh token is available or this mechanism is not used.
 */
data class AuthTokens(
    val accessToken: String?,
    val refreshToken: String?,
)

/**
 * Defines the contract for managing the storage and retrieval of [AuthTokens].
 * Implementations of this interface are responsible for persisting tokens
 * securely and making them available to the application.
 */
interface IAuthTokenManager {
    
    /**
     * Saves the provided [AuthTokens] to persistent storage.
     * This operation is asynchronous and should be called from a coroutine.
     *
     * @param tokens The [AuthTokens] to be saved.
     */
    suspend fun saveTokens(tokens: AuthTokens)
    
    /**
     * Retrieves the stored [AuthTokens] from persistent storage.
     * This operation is asynchronous and should be called from a coroutine.
     *
     * @return The stored [AuthTokens], or null if no tokens are found or
     *         the user is not authenticated.
     */
    suspend fun getTokens(): AuthTokens?
    
    /**
     * Clears any stored [AuthTokens] from persistent storage.
     * This is typically called during a logout operation.
     * This operation is asynchronous and should be called from a coroutine.
     */
    suspend fun clearTokens()
    
    /**
     * Retrieves the currently available [AuthTokens] synchronously.
     * This method is intended for use in contexts where suspending functions
     * cannot be easily called, such as within an [okhttp3.Interceptor] for OkHttp.
     *
     * **Important:** Implementations should ensure this method returns quickly,
     * typically by providing a cached copy of the tokens. It should not perform
     * blocking I/O operations. The cache should be kept consistent with
     * operations like [saveTokens] and [clearTokens].
     *
     * @return The currently cached [AuthTokens], or null if no tokens are
     *         available in the cache or the user is not authenticated.
     */
    fun getCurrentTokens(): AuthTokens?
}