package com.example.campfire.auth.data.remote.dto.request


// Data class for the registration request
data class RegisterRequest(
    val email: String,
    val phone: String,
    val password: String
)