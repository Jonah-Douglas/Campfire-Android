package com.example.campfire.auth.domain.repository

import com.example.campfire.core.data.auth.AuthTokens


sealed class LoginResult {
    data class Success(val tokens: AuthTokens) : LoginResult()
    object InvalidCredentialsError : LoginResult()
    object UserInactiveError : LoginResult()
    data class NetworkError(val message: String?) : LoginResult()
    data class GenericError(val code: Int? = null, val message: String?) : LoginResult()
}

interface AuthRepository {
    suspend fun login(email: String, password: String): LoginResult
    // JD TODO: Add other methods for register, verifyEmail, verifyPhone, etc.
}