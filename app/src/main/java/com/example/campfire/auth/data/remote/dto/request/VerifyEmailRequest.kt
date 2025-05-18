package com.example.campfire.auth.data.remote.dto.request


// Data class for email verification request
data class VerifyEmailRequest(
    val email: String,
    val code: String
)