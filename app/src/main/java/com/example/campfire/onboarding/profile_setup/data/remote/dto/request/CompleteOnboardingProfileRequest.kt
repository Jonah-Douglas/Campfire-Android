package com.example.campfire.onboarding.profile_setup.data.remote.dto.request

import com.google.gson.annotations.SerializedName


/**
 * Data class for the request to complete user registration
 */
data class CompleteOnboardingProfileRequest(
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("date_of_birth")
    val dateOfBirth: String,
    @SerializedName("enable_notifications")
    val enableNotifications: Boolean
)