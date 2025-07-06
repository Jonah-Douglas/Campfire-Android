package com.example.campfire.auth.domain.usecase

import com.example.campfire.auth.domain.repository.AuthRepository
import com.example.campfire.auth.domain.repository.LoginResult
import javax.inject.Inject


/**
 * Use case for logging in a user manually.
 */
class LoginUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): LoginResult {
        if (email.isBlank()) {
            return LoginResult.GenericError(message = "Email cannot be empty.")
        }
        if (password.isBlank()) {
            return LoginResult.GenericError(message = "Password cannot be empty.")
        }
        
        return authRepository.login(email, password)
    }
}