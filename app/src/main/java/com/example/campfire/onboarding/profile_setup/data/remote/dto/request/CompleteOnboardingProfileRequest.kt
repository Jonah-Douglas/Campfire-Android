package com.example.campfire.onboarding.profile_setup.data.remote.dto.request

import com.google.gson.annotations.SerializedName


/**
 * Data class for the request to complete the profile setup.
 */
data class CompleteOnboardingProfileRequest(
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("email")
    val email: String,
    /**
     * Date of birth in "YYYY-MM-DD" format.
     * Example: "1990-01-15"
     */
    @SerializedName("date_of_birth")
    val dateOfBirth: String,
    @SerializedName("enable_notifications")
    val enableNotifications: Boolean
)