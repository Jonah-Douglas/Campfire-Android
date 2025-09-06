package com.example.campfire.core.domain.model

import java.time.LocalDate
import java.time.LocalDateTime


/**
 * Represents a user within the Campfire application.
 * This model holds all essential information pertaining to a user's profile,
 * preferences, and status within the system.
 *
 * @property id The unique identifier for the user.
 * @property phone The user's primary phone number, including country code. See [PhoneNumber].
 * @property firstName The user's first name. Nullable for base profile setup.
 * @property lastName The user's last name. Nullable for base profile setup.
 * @property email The user's email address. Nullable for base profile setup.
 * @property dateOfBirth The user's date of birth. Nullable for base profile setup.
 * @property enableNotifications Flag indicating whether the user has enabled push notifications.
 *           `true` if notifications are enabled, `false` otherwise.
 * @property isProfileComplete Flag indicating if the user has completed the essential
 *           parts of their profile setup (e.g., name, potentially DoB or email depending on app requirements).
 *           `true` if complete, `false` otherwise.
 * @property isAppSetupComplete Flag indicating if the user has completed any initial
 *           app-specific setup steps beyond basic profile creation.
 *           `true` if complete, `false` otherwise.
 * @property isActive Flag indicating whether the user's account is currently active.
 *           An inactive user might be soft-deleted or suspended.
 *           `true` if active, `false` otherwise.
 * @property lastLoginAt Timestamp of the user's last login. Nullable if the user has never logged in
 *           or if this information is not tracked.
 * @property updatedAt Timestamp of when the user record was last updated.
 * @property createdAt Timestamp of when the user record was initially created.
 */
data class User(
    val id: Long,
    val phone: PhoneNumber,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val dateOfBirth: LocalDate?,
    val enableNotifications: Boolean,
    val isProfileComplete: Boolean,
    val isAppSetupComplete: Boolean,
    val isActive: Boolean,
    val lastLoginAt: LocalDateTime?,
    val updatedAt: LocalDateTime,
    val createdAt: LocalDateTime,
)