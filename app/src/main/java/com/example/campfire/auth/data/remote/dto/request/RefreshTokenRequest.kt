package com.example.campfire.auth.data.remote.dto.request

import com.google.gson.annotations.SerializedName


/**
 * Data class for refresh token request
 */
data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String,
)