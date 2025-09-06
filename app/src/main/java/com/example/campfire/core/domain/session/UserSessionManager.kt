package com.example.campfire.core.domain.session

import com.example.campfire.core.domain.model.SessionEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


/**
 * Defines the contract for managing the user's session state and related lifecycle events.
 * This manager is the central authority for:
 *  - Knowing whether a user is currently authenticated.
 *  - Tracking the completion status of essential onboarding steps (profile and app setup).
 *  - Persisting and updating these states.
 *  - Emitting events for significant session changes, like invalidation.
 *
 * Implementations of this interface are typically responsible for interacting with
 * persistent storage (e.g., DataStore, SharedPreferences with encryption) to store and
 * retrieve session information.
 */
interface UserSessionManager {
    
    /**
     * A [Flow] that emits [SessionEvent]s when significant session lifecycle changes occur.
     * This can be used to react to events like session invalidation.
     * @see SessionEvent
     */
    val sessionEvents: Flow<SessionEvent>
    
    /**
     * Provides a [StateFlow] indicating the current authentication status of the user.
     * `true` if the user is considered authenticated, `false` otherwise.
     * This flow will emit new values whenever the authentication state changes.
     *
     * @return A [StateFlow] of [Boolean] representing the authentication status.
     */
    fun isAuthenticatedFlow(): StateFlow<Boolean>
    
    /**
     * Provides a [StateFlow] indicating whether the user has completed their core profile setup.
     * `true` if profile setup is complete, `false` otherwise.
     * This state typically persists across application restarts and user sessions until explicitly changed.
     *
     * @return A [StateFlow] of [Boolean] representing profile setup completion status.
     */
    fun isProfileCompleteFlow(): StateFlow<Boolean>
    
    /**
     * Provides a [StateFlow] indicating whether the user has completed initial app-specific setup.
     * `true` if app setup is complete, `false` otherwise.
     * This state typically persists across application restarts and user sessions until explicitly changed.
     *
     * @return A [StateFlow] of [Boolean] representing app setup completion status.
     */
    fun isAppSetupCompleteFlow(): StateFlow<Boolean>
    
    /**
     * Updates the user's authentication status and onboarding completion flags simultaneously.
     * This is useful during login or when initial session state is established.
     * Implementations should persist these states.
     *
     * @param isAuthenticated The new authentication status.
     * @param isProfileComplete The new profile setup completion status.
     * @param isAppSetupComplete The new app setup completion status.
     */
    suspend fun updateUserLoginAndOnboardingState(
        isAuthenticated: Boolean,
        isProfileComplete: Boolean,
        isAppSetupComplete: Boolean
    )
    
    /**
     * Updates the persisted status of core profile setup completion.
     *
     * @param isComplete `true` if profile setup is now complete, `false` otherwise.
     */
    suspend fun updateProfileSetupComplete(isComplete: Boolean)
    
    /**
     * Updates the persisted status of app-specific setup completion.
     *
     * @param isComplete `true` if app-specific setup is now complete, `false` otherwise.
     */
    suspend fun updateAppSetupComplete(isComplete: Boolean)
    
    /**
     * Clears all persisted user session data.
     * This should effectively log the user out and reset any session-specific flags
     * managed by this manager (like authentication status).
     */
    suspend fun clearUserSession()
    
    /**
     * Notifies the system that the current session has been invalidated externally..
     * This should trigger the emission of a [SessionEvent.SessionInvalidated] event
     * via the [sessionEvents] flow and potentially clear local session data similar to [clearUserSession].
     */
    suspend fun notifySessionInvalidated()
}