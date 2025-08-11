package com.example.campfire.auth.data.remote

import com.example.campfire.auth.data.remote.dto.request.CompleteProfileRequest
import com.example.campfire.auth.data.remote.dto.request.SendOTPRequest
import com.example.campfire.auth.data.remote.dto.request.VerifyOTPRequest
import com.example.campfire.auth.data.remote.dto.response.ApiResponse
import com.example.campfire.auth.data.remote.dto.response.OTPResponse
import com.example.campfire.auth.data.remote.dto.response.TokenResponse
import com.example.campfire.auth.data.remote.dto.response.UserResponse
import retrofit2.http.Body
import retrofit2.http.POST


interface AuthApiService {
    /**
     * Sends an OTP (One-Time Password) to the provided phone number.
     * The API response will indicate success and may contain a relevant message.
     */
    @POST("auth/request-otp")
    suspend fun sendOtp(@Body request: SendOTPRequest): ApiResponse<OTPResponse>
    
    /**
     * Verifies the OTP code sent to the phone number.
     * On success, returns an ApiResponse containing TokenResponse in its data field.
     */
    @POST("auth/verify-otp")
    suspend fun verifyOTP(@Body request: VerifyOTPRequest): ApiResponse<TokenResponse>
    
    /**
     * Finishes the base user account setup.
     * On success, returns an ApiResponse containing UserResponse in its data field.
     */
    @POST("/me/complete-profile")
    suspend fun completeUserProfile(@Body request: CompleteProfileRequest): ApiResponse<UserResponse>
    
    /**
     * Logs out the current user.
     * The API response will indicate success and may contain a relevant message.
     */
    @POST("auth/logout")
    suspend fun logoutUser(): ApiResponse<Unit>
    
    // JD TODO: Update the email calls here to be similar to the phone verification code process (if I want them at all)
//    @POST("auth/verify-email")
//    fun verifyEmail(@Body request: VerifyEmailRequest): ApiResponse<TokenResponse>>
}