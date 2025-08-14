package com.example.campfire.auth.data.remote.dto.response

import com.google.gson.annotations.SerializedName


/**
 * Represents the response from an API call that initiates an OTP process.
 *
 * @property message A confirmation message (e.g., "OTP sent successfully.").
 * @property debugOTP The actual OTP code, intended ONLY for use in debug/testing environments
 *   to bypass actual OTP delivery. This should be null or absent in production.
 */
data class OTPResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("debug_otp")
    val debugOTP: String? = null
)