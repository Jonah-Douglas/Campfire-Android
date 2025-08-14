package com.example.campfire.auth.data.remote.dto.response

import com.google.gson.annotations.SerializedName


/**
 * Represents the response from a successful login or initial authentication.
 *
 * @property accessToken The access token for authenticating subsequent API requests.
 * @property refreshToken The refresh token used to obtain new access tokens.
 * @property isNewUser Indicates if the user is logging in for the first time or if their profile is incomplete.
 * @property tokenType The type of the token issued (e.g., "Bearer").
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