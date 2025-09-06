package com.example.campfire.auth.domain.model

import com.example.campfire.auth.data.local.AuthTokens


data class AuthFlowSuccessDetails(
    val tokens: AuthTokens,
    val isNewUser: Boolean,
    val isProfileComplete: Boolean,
    val isAppSetupComplete: Boolean
)


/**
 * Represents the result of an attempt to verify an OTP.
 */
sealed interface VerifyOTPResult {
    /**
     * OTP verification was successful, and tokens were issued.
     * Contains details about the user's state for onboarding.
     */
    data class Success(val details: AuthFlowSuccessDetails) : VerifyOTPResult
    
    // Client-side validation errors for OTP format
    sealed interface InvalidOTPFormat : VerifyOTPResult {
        val message: String
        
        data class Empty(override val message: String) : InvalidOTPFormat
        data class IncorrectLength(override val message: String) : InvalidOTPFormat
        data class NonNumeric(override val message: String) : InvalidOTPFormat
    }
    
    // Server-side specific OTP issues
    data class OTPIncorrect(val message: String? = null) : VerifyOTPResult
    data class OTPExpired(val message: String? = null) : VerifyOTPResult
    data class RateLimited(val retryAfterSeconds: Int? = null, val message: String? = null) :
        VerifyOTPResult
    
    // General errors
    data class Network(val message: String? = null) : VerifyOTPResult
    data class Generic(val code: Int? = null, val message: String? = null) : VerifyOTPResult
}