package com.example.campfire.core.domain.usecase

import com.example.campfire.core.data.preferences.UserPreferencesSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


/**
 * Use case to get the completion status of the profile setup.
 */
class GetProfileSetupCompletionStatusUseCase @Inject constructor(
    private val userPreferencesSource: UserPreferencesSource
) {
    operator fun invoke(): Flow<Boolean> {
        return userPreferencesSource.isProfileSetupComplete
    }
}

/**
 * Use case to set the completion status of the profile setup.
 */
class SetProfileSetupCompletionStatusUseCase @Inject constructor(
    private val userPreferencesSource: UserPreferencesSource
) {
    suspend operator fun invoke(completed: Boolean) {
        userPreferencesSource.setProfileSetupComplete(completed)
    }
}