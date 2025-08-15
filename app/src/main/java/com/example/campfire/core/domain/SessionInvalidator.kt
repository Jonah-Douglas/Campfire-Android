package com.example.campfire.core.domain


/**
 * Defines the contract for services responsible for handling the invalidation
 * of the user's session.
 *
 * Implementations of this interface are expected to:
 * - Clear locally stored authentication tokens or session identifiers
 * - Reset any in-memory user-specific state
 * - Notify relevant parts of the application (e.g., UI, navigation)
 *   that the user needs to be logged out and redirected  to the login screen
 */
interface SessionInvalidator {
    
    /**
     * Invalidates the current user session and triggers the application's logout process.
     *
     * This function should be called when it's determined that the user's session
     * is no longer valid (e.g., due to token expiry, server-side revocation,
     * or explicit user logout action).
     *
     * Implementations should ensure this operation is idempotent.
     */
    suspend fun invalidateSessionAndTriggerLogout()
}