package com.example.campfire.auth.domain.model

import com.example.campfire.auth.data.local.AuthTokens


/**
 * Represents the result of an attempt to verify an OTP.
 */
sealed interface VerifyOTPResult {
    data class SuccessLogin(val tokens: AuthTokens) : VerifyOTPResult
    data class SuccessRegistration(val tokens: AuthTokens) : VerifyOTPResult
    
    /**
     * OTP was valid, but the user already existed during a registration attempt.
     * This might lead to treating it as a login or prompting the user.
     */
    data class SuccessButUserExistedDuringRegistration(val tokens: AuthTokens) : VerifyOTPResult
    
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