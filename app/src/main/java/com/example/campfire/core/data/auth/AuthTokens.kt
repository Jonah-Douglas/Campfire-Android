package com.example.campfire.core.data.auth


data class AuthTokens(
    val accessToken: String?,
    val refreshToken: String?
)

interface AuthTokenStorage {
    suspend fun saveTokens(tokens: AuthTokens?)
    suspend fun getTokens(): AuthTokens?
    suspend fun clearTokens()
}