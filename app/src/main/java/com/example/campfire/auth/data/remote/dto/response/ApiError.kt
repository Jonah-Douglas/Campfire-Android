package com.example.campfire.auth.data.remote.dto.response


/**
 * Generic Error structure
 */
data class ApiError(
    val code: String,
    val details: String?,
    val fields: Map<String, String>?
)