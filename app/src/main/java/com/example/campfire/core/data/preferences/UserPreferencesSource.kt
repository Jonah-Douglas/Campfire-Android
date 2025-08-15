package com.example.campfire.core.data.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Interface defining the contract for accessing and managing user-specific preferences.
 *
 * This abstraction allows for different implementations of preference storage
 * and facilitates testing by enabling the use of fake or mock implementations.
 */
interface UserPreferencesSource {
    
    /**
     * A [Flow] emitting the current state of the "is entry complete" preference.
     *
     * This flow will emit `true` if the user has completed the initial entry/setup,
     * and `false` otherwise.
     */
    val isEntryComplete: Flow<Boolean>
}
