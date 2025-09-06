package com.example.campfire.auth.domain.repository

import com.example.campfire.auth.domain.model.AuthAction
import com.example.campfire.auth.domain.model.LogoutResult
import com.example.campfire.auth.domain.model.SendOTPResult
import com.example.campfire.auth.domain.model.VerifyOTPResult


/**
 * Interface defining the contract for authentication-related operations.
 * This repository handles tasks such as sending OTPs, verifying OTPs, and user logout.
 */
interface AuthRepository {
    
    /**
     * Attempts to log the current user out.
     * This involves clearing local session data and invalidating server-side tokens.
     *
     * @return [LogoutResult] indicating the outcome of the logout operation.
     */
    suspend fun logout(): LogoutResult
    
    /**
     * Sends a One-Time Password (OTP) to the provided phone number for a specific authentication action.
     *
     * @param phoneNumber The phone number to which the OTP should be sent.
     *                    It should typically include the country code.
     * @param authAction The authentication action being performed (e.g., SIGN_UP, LOGIN, PASSWORD_RESET).
     *                   This might influence the OTP message or server-side logic.
     * @return [SendOTPResult] indicating whether the OTP was successfully sent or if an error occurred.
     */
    suspend fun sendOTP(phoneNumber: String, authAction: AuthAction): SendOTPResult
    
    /**
     * Verifies the One-Time Password (OTP) entered by the user against the one sent
     * to the specified phone number.
     *
     * @param phoneNumber The phone number associated with the OTP.
     * @param otpCode The OTP code entered by the user.
     * @return [VerifyOTPResult] indicating whether the OTP is valid, invalid, or if an error occurred.
     */
    suspend fun verifyOTP(phoneNumber: String, otpCode: String): VerifyOTPResult
}