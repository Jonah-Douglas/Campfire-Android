package com.example.campfire.auth.data.remote.dto.response

import com.google.gson.annotations.SerializedName


// Generic API response wrapper
data class ApiResponse<T>(
    val success: Boolean,
    @SerializedName("data")
    val data: T? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("error")
    val error: String? = null // Added for more robust error handling
)