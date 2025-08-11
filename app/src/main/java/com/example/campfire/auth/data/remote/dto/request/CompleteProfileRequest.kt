package com.example.campfire.auth.data.remote.dto.request

import com.google.gson.annotations.SerializedName
import java.time.LocalDate


/**
 * Data class for the request to complete user registration
 */
data class CompleteProfileRequest(
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("date_of_birth")
    val dateOfBirth: LocalDate,
    @SerializedName("enable_notifications")
    val enableNotifications: Boolean,
)