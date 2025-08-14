package com.example.campfire.auth.data.remote.dto.response

import com.google.gson.annotations.SerializedName


/**
 * Represents the response from a successful token refresh operation.
 *
 * @property accessToken The new access token.
 * @property refreshToken An optional new refresh token. If provided, the client should
 *   replace its existing refresh token with this new one (token rotation).
 *   If null, the client should continue using its current refresh token.
 */
data class RefreshedTokensResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String?
)