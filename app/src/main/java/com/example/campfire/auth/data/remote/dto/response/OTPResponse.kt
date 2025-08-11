package com.example.campfire.auth.data.remote.dto.response

import com.google.gson.annotations.SerializedName


/**
 * OTP response wrapper
 */
data class OTPResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("debug_otp")
    val debugOTP: String? = null
)