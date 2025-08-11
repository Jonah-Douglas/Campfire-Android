package com.example.campfire.auth.data.remote.dto.response

import com.google.gson.annotations.SerializedName


/**
 * Data class for Login Response
 */
data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("is_new_user")
    val isNewUser: Boolean,
    @SerializedName("token_type")
    val tokenType: String
)