package com.example.campfire.auth.data.remote.dto.response


data class RefreshedTokensResponse(
    val accessToken: String,
    val refreshToken: String?
)
