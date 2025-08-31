package com.example.campfire.core.presentation.model


/**
 * Data class representing the critical top-level state of the application,
 * used primarily by the presentation layer to determine initial navigation paths
 * and adapt UI based on authentication and onboarding completion.
 *
 * This state is typically derived from observing a more granular global state manager,
 * like [com.example.campfire.core.presentation.GlobalStateViewModel].
 *
 * @property isAuthenticated True if the user is currently authenticated.
 * @property isProfileSetupComplete True if the user has completed the core profile setup step
 *                                 of onboarding. This flag persists across sessions.
 * @property isAppSetupComplete True if the user has completed the app-specific setup step
 *                              of onboarding. This flag persists across sessions.
 */
data class AppState(
    val isAuthenticated: Boolean,
    val isProfileSetupComplete: Boolean,
    val isAppSetupComplete: Boolean
)
