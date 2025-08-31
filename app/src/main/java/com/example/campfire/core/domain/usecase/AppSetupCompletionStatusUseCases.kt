package com.example.campfire.core.domain.usecase

import com.example.campfire.core.data.preferences.UserPreferencesSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


/**
 * Use case to get the completion status of the app setup.
 */
class GetAppSetupCompletionStatusUseCase @Inject constructor(
    private val userPreferencesSource: UserPreferencesSource
) {
    operator fun invoke(): Flow<Boolean> {
        return userPreferencesSource.isAppSetupComplete
    }
}

/**
 * Use case to set the completion status of the app setup.
 */
class SetAppSetupCompletionStatusUseCase @Inject constructor(
    private val userPreferencesSource: UserPreferencesSource
) {
    suspend operator fun invoke(completed: Boolean) {
        userPreferencesSource.setAppSetupComplete(completed)
    }
}