package com.example.campfire.auth.domain.usecase

import android.util.Log
import com.example.campfire.auth.domain.repository.AuthRepository
import com.example.campfire.auth.domain.repository.LoginResult
import com.example.campfire.core.data.auth.AuthTokenStorage
import javax.inject.Inject


class LoginUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStorage: AuthTokenStorage,
) {
    suspend operator fun invoke(email: String, password: String): LoginResult {
        if (email.isBlank()) {
            return LoginResult.GenericError(message = "Email cannot be empty.")
        }
        if (password.isBlank()) {
            return LoginResult.GenericError(message = "Password cannot be empty.")
        }
        
        // Make call to perform repo login
        val repositoryResult = authRepository.login(email, password)
        
        if (repositoryResult is LoginResult.Success) {
            try {
                tokenStorage.saveTokens(repositoryResult.tokens)
            } catch (e: Exception) {
                Log.e("LoginUserUseCase", "Failed to save tokens", e)
            }
        }
        
        return repositoryResult
    }
}