package com.example.campfire.onboarding.profile_setup.domain.repository

import com.example.campfire.onboarding.profile_setup.domain.model.CompleteProfileSetupResult
import java.time.LocalDate


interface ProfileSetupRepository {
    suspend fun completeProfileSetup(
        firstName: String,
        lastName: String,
        email: String,
        dateOfBirth: LocalDate,
        enableNotifications: Boolean
    ): CompleteProfileSetupResult
}