package com.example.campfire.auth.data.remote.dto.response


// Generic API response wrapper
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null // Added for more robust error handling
)