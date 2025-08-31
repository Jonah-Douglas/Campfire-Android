package com.example.campfire.auth.domain.usecase

import com.example.campfire.auth.domain.model.LogoutResult
import com.example.campfire.auth.domain.repository.AuthRepository
import javax.inject.Inject


/**
 * Use case for a User logging themselves out.
 */
class HandleUserInitiatedLogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): LogoutResult {
        return authRepository.logout()
    }
}