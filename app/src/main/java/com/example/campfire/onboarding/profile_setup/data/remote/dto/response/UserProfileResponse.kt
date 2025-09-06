package com.example.campfire.onboarding.profile_setup.data.remote.dto.response

import com.google.gson.annotations.SerializedName


/**
 * Data Transfer Object (DTO) representing the user's public profile data
 * as received from the backend API.
 * This class directly maps to the `UserPublic` schema defined in the FastAPI backend.
 * It's primarily used for deserializing the JSON response when fetching user profile information.
 */
data class UserProfileResponse(
    /** The unique identifier for the user. */
    @SerializedName("id")
    val id: Long,
    
    /** The user's phone number, typically including the country code. */
    @SerializedName("phone_number")
    val phoneNumber: String,
    
    /** The user's first name. Null on initial profile setup. */
    @SerializedName("first_name")
    val firstName: String?,
    
    /** The user's last name. Null on initial profile setup. */
    @SerializedName("last_name")
    val lastName: String?,
    
    /** The user's email address. Null on initial profile setup. */
    @SerializedName("email")
    val email: String?,
    
    /**
     * The user's date of birth as a string in "YYYY-MM-DD" format.
     * Null on initial profile setup.
     */
    @SerializedName("date_of_birth")
    val dateOfBirth: String?,
    
    /**
     * Flag indicating whether the user has enabled notifications.
     * `true` if notifications are enabled, `false` otherwise.
     */
    @SerializedName("is_enable_notifications")
    val isEnableNotifications: Boolean,
    
    /**
     * Flag indicating if the user's profile is considered complete
     * based on backend criteria (e.g., essential fields are filled).
     * `true` if complete, `false` otherwise.
     */
    @SerializedName("is_profile_complete")
    val isProfileComplete: Boolean,
    
    /**
     * Flag indicating if the user has completed any initial app-specific setup steps.
     * `true` if complete, `false` otherwise.
     */
    @SerializedName("is_app_setup_complete")
    val isAppSetupComplete: Boolean,
    
    /**
     * Flag indicating whether the user's account is currently active.
     * `true` if active, `false` otherwise.
     */
    @SerializedName("is_active")
    val isActive: Boolean,
    
    /**
     * ISO 8601 timestamp string representing the user's last login time.
     * Example: "2023-10-27T10:30:00Z".
     * Null if the user has never logged in or if this information is not available.
     */
    @SerializedName("last_login_at")
    val lastLoginAt: String?,
    
    /**
     * ISO 8601 timestamp string representing when the user profile was last updated.
     */
    @SerializedName("updated_at")
    val updatedAt: String,
    
    /**
     * ISO 8601 timestamp string representing when the user profile was created.
     */
    @SerializedName("created_at")
    val createdAt: String
)