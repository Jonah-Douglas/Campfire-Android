package com.example.campfire.auth.data.remote

import com.example.campfire.auth.data.remote.dto.request.RefreshTokenRequest
import com.example.campfire.auth.data.remote.dto.response.RefreshedTokensResponse
import com.example.campfire.core.data.remote.dto.response.APIResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


/**
 * Service dedicated to refreshing authentication tokens.
 * Maintained as its own service outside of the AuthAPIService to uphold SRP (Single Responsibility Principle),
 * address its different security considerations, and manage unique error handling scenarios for token refresh.
 * This is often used by an OkHttp Authenticator or similar mechanisms.
 */
interface TokenRefreshAPIService {
    
    /**
     * Attempts to refresh the authentication tokens using a provided refresh token.
     *
     * This method returns a Retrofit `Call` object to allow for synchronous execution via OkHttp's Authenticator.
     *
     * The response is wrapped in `ApiResponse` to maintain consistency with other API calls,
     * allowing standardized checking of success, data, and error messages.
     *
     * @param request The refresh token request payload, containing the current refresh token.
     * @return A Retrofit `Call` that, when executed, will yield an `ApiResponse`
     *         containing the `RefreshedTokensResponse` (with new access and potentially refresh tokens)
     *         in its `data` field, or an error if the refresh fails.
     */
    @POST("auth/refresh")
    fun refreshAuthToken(@Body request: RefreshTokenRequest): Call<APIResponse<RefreshedTokensResponse>>
}