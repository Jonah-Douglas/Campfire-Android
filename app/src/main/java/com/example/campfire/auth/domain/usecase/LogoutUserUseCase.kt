package com.example.campfire.auth.domain.usecase

import com.example.campfire.auth.domain.repository.AuthRepository
import com.example.campfire.auth.domain.repository.LogoutResult
import javax.inject.Inject


/**
 * Use case for logging out a user manually.
 */
class LogoutUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): LogoutResult {
        return authRepository.logout()
    }
}