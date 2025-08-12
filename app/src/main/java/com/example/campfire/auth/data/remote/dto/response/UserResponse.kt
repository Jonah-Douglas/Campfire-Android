package com.example.campfire.auth.data.remote.dto.response

import com.google.gson.annotations.SerializedName


/**
 * Data Transfer Object representing the user data as received from the backend API.
 * This is typically returned after login, registration completion, or fetching user profile.
 */
data class UserResponse(
    @SerializedName("id")
    val id: Long,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("date_of_birth")
    val dateOfBirth: String,
    @SerializedName("enable_notifications")
    val enableNotifications: Boolean,
    @SerializedName("is_profile_complete")
    val isProfileComplete: Boolean,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("is_superuser")
    val isSuperuser: Boolean,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)