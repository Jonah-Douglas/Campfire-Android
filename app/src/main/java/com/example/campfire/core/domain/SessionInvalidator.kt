package com.example.campfire.core.domain


/**
 * Interface responsible for handling the invalidation of the user's session.
 * This typically involves clearing local tokens and notifying the app
 * that the user needs to be logged out.
 */
interface SessionInvalidator {
    suspend fun invalidateSessionAndTriggerLogout()
}