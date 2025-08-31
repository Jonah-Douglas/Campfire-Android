package com.example.campfire.auth.domain.usecase

import com.example.campfire.auth.domain.model.AuthAction
import com.example.campfire.auth.domain.model.SendOTPResult
import com.example.campfire.auth.domain.repository.AuthRepository
import javax.inject.Inject


/**
 * Use case for sending a One-Time Password (OTP) to a user's phone number.
 * It includes basic validation for the phone number format.
 */
class SendOTPUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    
    suspend operator fun invoke(phoneNumber: String, authAction: AuthAction): SendOTPResult {
        val trimmedPhoneNumber = phoneNumber.trim()
        
        if (trimmedPhoneNumber.isBlank()) {
            return SendOTPResult.InvalidPhoneNumber(message = ERROR_PHONE_NUMBER_EMPTY)
        }
        
        if (trimmedPhoneNumber.filter { it.isDigit() }.length < MIN_PHONE_LENGTH) {
            return SendOTPResult.InvalidPhoneNumber(
                message = String.format(
                    ERROR_INVALID_PHONE_NUMBER,
                    MIN_PHONE_LENGTH
                )
            )
        }
        
        // No issues found with local validation, proceed to repository
        return authRepository.sendOTP(trimmedPhoneNumber, authAction)
    }
    
    companion object {
        private const val MIN_PHONE_LENGTH = 10
        
        // Errors
        private const val ERROR_PHONE_NUMBER_EMPTY =
            "Phone number cannot be empty."
        private const val ERROR_INVALID_PHONE_NUMBER =
            "Please enter a valid phone number with at least '%s' digits."
    }
}