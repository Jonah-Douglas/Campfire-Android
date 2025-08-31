package com.example.campfire.auth.domain.model


/**
 * Represents the result of an attempt to send an OTP.
 */
sealed interface SendOTPResult {
    data object Success : SendOTPResult
    
    data class InvalidPhoneNumber(val message: String? = null) : SendOTPResult
    data class RateLimited(val message: String? = null) : SendOTPResult
    data class UserAlreadyExists(val message: String? = null) : SendOTPResult
    data class UserNotFound(val message: String? = null) : SendOTPResult
    data class Network(val message: String? = null) : SendOTPResult
    data class Generic(val message: String? = null) : SendOTPResult
}