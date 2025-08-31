package com.example.campfire.auth.domain.usecase

import com.example.campfire.auth.domain.model.AuthAction
import com.example.campfire.auth.domain.model.VerifyOTPResult
import com.example.campfire.auth.domain.repository.AuthRepository
import javax.inject.Inject


/**
 * Use case for verifying a One-Time Password (OTP).
 * It includes basic validation for the OTP format before attempting verification.
 */
class VerifyOTPUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    
    suspend operator fun invoke(
        phoneNumber: String,
        otpCode: String,
        authAction: AuthAction
    ): VerifyOTPResult {
        // 1. Validate OTP format
        if (otpCode.isBlank()) {
            return VerifyOTPResult.InvalidOTPFormat.Empty(message = ERROR_OTP_EMPTY)
        }
        if (otpCode.length != OTP_LENGTH) {
            return VerifyOTPResult.InvalidOTPFormat.IncorrectLength(
                message = String.format(
                    ERROR_INVALID_OTP_DIGIT_COUNT,
                    OTP_LENGTH
                )
            )
        }
        if (!otpCode.all { it.isDigit() }) {
            return VerifyOTPResult.InvalidOTPFormat.NonNumeric(message = ERROR_INVALID_OTP_DIGITS)
        }
        
        // 2. Validate phone number (basic check, more comprehensive validation might be elsewhere)
        if (phoneNumber.isBlank()) {
            throw IllegalArgumentException(ERROR_PHONE_NUMBER_EMPTY)
        }
        
        // 3. If validations pass, proceed to repository call
        return authRepository.verifyOTP(
            phoneNumber = phoneNumber,
            otpCode = otpCode,
            authAction = authAction
        )
    }
    
    companion object {
        const val OTP_LENGTH = 6
        
        // Errors
        private const val ERROR_OTP_EMPTY =
            "OTP cannot be empty."
        private const val ERROR_INVALID_OTP_DIGIT_COUNT =
            "OTP must be '%s' digits."
        private const val ERROR_INVALID_OTP_DIGITS =
            "OTP must contain only digits."
        private const val ERROR_PHONE_NUMBER_EMPTY =
            "Phone number cannot be empty."
    }
}