package com.example.campfire.auth.data.remote.dto.request

import com.google.gson.annotations.SerializedName


/**
 * Data class for the OTP verification request
 */
data class SendOTPRequest(
    @SerializedName("phone_number")
    val phoneNumber: String
)