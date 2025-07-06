package com.example.campfire.auth.domain.usecase

import com.example.campfire.auth.data.remote.dto.request.RegisterRequest // Or your domain model for request
import com.example.campfire.auth.domain.repository.AuthRepository
import com.example.campfire.auth.domain.repository.RegisterResult
import javax.inject.Inject


class RegisterUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        phone: String,
        password: String
    ): RegisterResult {
        // Basic local validation
        if (email.isBlank() || phone.isBlank() || password.isBlank()) {
            return RegisterResult.GenericError(message = "Email, phone, and password cannot be empty.")
        }
        
        // The primary responsibility is to interact with the repository
        val request = RegisterRequest(
            email = email.trim(),
            phone = phone.trim(),
            password = password
        )
        
        return authRepository.registerUser(request)
    }
}