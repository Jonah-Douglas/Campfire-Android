package com.example.campfire.auth.domain.repository

import com.example.campfire.auth.data.remote.dto.request.RegisterRequest
import com.example.campfire.core.data.auth.AuthTokens


sealed interface RegisterResult {
    data class Success(val message: String?, val requiresEmailVerification: Boolean) :
        RegisterResult
    
    data class EmailAlreadyExistsError(val message: String? = "This email is already registered.") :
        RegisterResult
    
    data class WeakPasswordError(val message: String? = "Password is too weak.") : RegisterResult
    data class NetworkError(val message: String?) : RegisterResult
    data class GenericError(val code: Int? = null, val message: String?) : RegisterResult
}

sealed interface LoginResult {
    data class Success(val tokens: AuthTokens) : LoginResult
    
    data class InvalidCredentialsError(val message: String? = "Invalid email or password.") :
        LoginResult
    
    data class UserInactiveError(val message: String? = "This user account is inactive.") :
        LoginResult
    
    data class NetworkError(val message: String? = "A network error occurred.") : LoginResult
    data class GenericError(val message: String? = "An unexpected error occurred.") : LoginResult
}

sealed class LogoutResult {
    object Success : LogoutResult()
    data class NetworkError(val message: String?) : LogoutResult()
    data class GenericError(val code: Int? = null, val message: String?) : LogoutResult()
}

interface AuthRepository {
    suspend fun login(email: String, password: String): LoginResult
    suspend fun logout(): LogoutResult
    suspend fun registerUser(registerRequest: RegisterRequest): RegisterResult
    // JD TODO: Add other methods for verifyEmail, verifyPhone, etc.
}