package com.example.campfire.core.data.auth


data class AuthTokens(
    val accessToken: String?,
    val refreshToken: String?,
)

interface IAuthTokenManager {
    fun saveTokens(tokens: AuthTokens)
    fun getTokens(): AuthTokens?
    fun clearTokens()
}