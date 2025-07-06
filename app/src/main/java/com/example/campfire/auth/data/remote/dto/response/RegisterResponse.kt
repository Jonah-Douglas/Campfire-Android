package com.example.campfire.auth.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Represents the top-level API response for a user registration attempt.
 * This structure assumes the API wraps the main content within a 'data' object
 * and might include a general status or message at the top level.
 */
data class RegisterApiResponse(
    @SerializedName("status")
    val status: String?,
    
    @SerializedName("message") // A general message from the API regarding the operation
    val message: String?,
    
    @SerializedName("data")
    val data: RegistrationData?
)

/**
 * Contains the specific data payload for a successful user registration.
 * This is expected to be nested within the 'data' field of [RegisterApiResponse].
 */
data class RegistrationData(
    @SerializedName("user_id")
    val userId: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("requires_email_verification")
    val requiresVerification: Boolean?,
    @SerializedName("message")
    val specificDataMessage: String?
)