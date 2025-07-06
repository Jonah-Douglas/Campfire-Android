package com.example.campfire.auth.data.remote

import com.example.campfire.auth.data.remote.dto.request.RefreshTokenRequest
import com.example.campfire.auth.data.remote.dto.response.RefreshedTokensResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


/**
 * Maintained as its own service outside of the AuthAPIService to uphold SRP, its different security considerations, and unique error handling.
 */
interface TokenRefreshApiService {
    @POST("auth/refresh") // Or your actual refresh token endpoint
    fun refreshAuthToken(@Body request: RefreshTokenRequest): Call<RefreshedTokensResponse>
}