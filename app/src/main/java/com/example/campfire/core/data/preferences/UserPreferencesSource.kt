package com.example.campfire.core.data.preferences

import kotlinx.coroutines.flow.Flow


interface UserPreferencesSource {
    
    /**
     * A [Flow] emitting the current state of the "is core profile setup complete" preference.
     * Emits `true` if the user has completed the core profile setup, `false` otherwise.
     */
    val isProfileSetupComplete: Flow<Boolean>
    
    /**
     * A [Flow] emitting the current state of the "is app setup complete" preference.
     * Emits `true` if the user has completed the app-specific setup, `false` otherwise.
     */
    val isAppSetupComplete: Flow<Boolean>
    
    /**
     * Sets the core profile setup completion status.
     * @param completed True if completed, false otherwise.
     */
    suspend fun setProfileSetupComplete(completed: Boolean)
    
    /**
     * Sets the app setup completion status.
     * @param completed True if completed, false otherwise.
     */
    suspend fun setAppSetupComplete(completed: Boolean)
    
    /**
     * Clears both core profile and app setup completion flags.
     */
    suspend fun clearOnboardingFlags()
}

