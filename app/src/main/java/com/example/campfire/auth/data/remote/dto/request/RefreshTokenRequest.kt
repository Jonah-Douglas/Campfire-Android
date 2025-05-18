package com.example.campfire.auth.data.remote.dto.request


data class RefreshTokenRequest(
    val accessToken: String,
    val tokenType: String
)
