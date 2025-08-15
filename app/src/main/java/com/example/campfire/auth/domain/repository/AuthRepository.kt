package com.example.campfire.auth.domain.repository

import com.example.campfire.auth.data.local.AuthTokens
import com.example.campfire.auth.data.remote.dto.request.CompleteProfileRequest
import com.example.campfire.auth.domain.model.User
import com.example.campfire.auth.presentation.navigation.AuthAction


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

/**
 * Represents the result of an attempt to verify an OTP.
 */
sealed interface VerifyOTPResult {
    data class SuccessLogin(val tokens: AuthTokens) : VerifyOTPResult
    data class SuccessRegistration(val tokens: AuthTokens) : VerifyOTPResult
    data class SuccessButUserExistedDuringRegistration(val tokens: AuthTokens) : VerifyOTPResult
    data class OTPIncorrect(val message: String? = null) : VerifyOTPResult
    data class OTPExpired(val message: String? = null) : VerifyOTPResult
    data class RateLimited(val retryAfterSeconds: Int? = null, val message: String? = null) :
        VerifyOTPResult
    
    data class Network(val message: String? = null) : VerifyOTPResult
    data class Generic(val code: Int? = null, val message: String? = null) : VerifyOTPResult
}

sealed interface CompleteProfileResult {
    data class Success(val user: User) : CompleteProfileResult
    data object EmailAlreadyExists : CompleteProfileResult
    data class Validation(val errors: Map<Field, String>) : CompleteProfileResult
    data class Network(val message: String? = null) : CompleteProfileResult
    data class Generic(val code: Int? = null, val message: String? = null) : CompleteProfileResult
}

enum class Field {
    FIRST_NAME,
    LAST_NAME,
    EMAIL,
    DATE_OF_BIRTH
}

sealed class LogoutResult {
    object Success : LogoutResult()
    data class Network(val message: String?) : LogoutResult()
    data class Generic(val code: Int? = null, val message: String?) : LogoutResult()
}

interface AuthRepository {
    suspend fun logout(): LogoutResult
    suspend fun completeUserProfile(request: CompleteProfileRequest): CompleteProfileResult
    suspend fun sendOTP(phoneNumber: String, authAction: AuthAction): SendOTPResult
    suspend fun verifyOTP(
        phoneNumber: String,
        otpCode: String,
        authAction: AuthAction
    ): VerifyOTPResult
    
    // JD TODO: Add other methods for verifyEmail, etc.
}