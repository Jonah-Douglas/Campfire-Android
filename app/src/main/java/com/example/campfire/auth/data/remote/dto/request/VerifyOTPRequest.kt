package com.example.campfire.auth.data.remote.dto.request

import com.google.gson.annotations.SerializedName


/**
 * Data class for verifying a phone via OTP request
 */
data class VerifyOTPRequest(
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("otp_code")
    val verificationCode: String
)