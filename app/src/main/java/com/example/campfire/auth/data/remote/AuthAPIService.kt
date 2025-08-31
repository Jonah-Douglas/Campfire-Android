package com.example.campfire.auth.data.remote

import com.example.campfire.auth.data.remote.dto.request.SendOTPRequest
import com.example.campfire.auth.data.remote.dto.request.VerifyOTPRequest
import com.example.campfire.auth.data.remote.dto.response.OTPResponse
import com.example.campfire.auth.data.remote.dto.response.TokenResponse
import com.example.campfire.core.data.remote.dto.response.APIResponse
import retrofit2.http.Body
import retrofit2.http.POST


interface AuthAPIService {
    /**
     * Sends an OTP (One-Time Password) to the provided phone number.
     * The API response will indicate success and may contain a relevant message.
     */
    @POST("auth/request-otp")
    suspend fun sendOtp(@Body request: SendOTPRequest): APIResponse<OTPResponse>
    
    /**
     * Verifies the OTP code sent to the phone number.
     * On success, returns an ApiResponse containing TokenResponse in its data field.
     */
    @POST("auth/verify-otp")
    suspend fun verifyOTP(@Body request: VerifyOTPRequest): APIResponse<TokenResponse>
    
    /**
     * Logs out the current user.
     * The API response will indicate success and may contain a relevant message.
     */
    @POST("auth/logout")
    suspend fun logoutUser(): APIResponse<Unit>
}