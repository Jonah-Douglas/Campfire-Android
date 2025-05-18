package com.example.campfire.auth.data.remote.dto.request


// Data class for phone verification request
data class VerifyPhoneRequest(
    val phone: String,
    val code: String
)