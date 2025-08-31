package com.example.campfire.auth.domain.repository

import com.example.campfire.auth.domain.model.AuthAction
import com.example.campfire.auth.domain.model.LogoutResult
import com.example.campfire.auth.domain.model.SendOTPResult
import com.example.campfire.auth.domain.model.VerifyOTPResult


interface AuthRepository {
    suspend fun logout(): LogoutResult
    suspend fun sendOTP(phoneNumber: String, authAction: AuthAction): SendOTPResult
    suspend fun verifyOTP(
        phoneNumber: String,
        otpCode: String,
        authAction: AuthAction
    ): VerifyOTPResult
}