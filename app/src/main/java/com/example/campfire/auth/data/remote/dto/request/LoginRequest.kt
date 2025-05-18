package com.example.campfire.auth.data.remote.dto.request


//Data class for login request
data class LoginRequest(
    val email: String,
    val password: String
)