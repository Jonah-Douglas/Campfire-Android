package com.example.campfire.core.domain.model


/**
 * Represents distinct events related to the user's session lifecycle.
 * These events are typically emitted by a [com.example.campfire.core.domain.session.UserSessionManager]
 * (or a similar session managing component) to signal significant changes in the session state
 * that other parts of the application might need to react to.
 */
sealed class SessionEvent {
    /**
     * Indicates that the current user session has become invalid.
     * This could be due to reasons such as:
     *  - Token expiry or revocation.
     *  - Explicit user logout.
     *  - Server-side invalidation of the session.
     *
     * Consumers of this event should typically clear any local session data
     * and navigate the user to an authentication screen.
     */
    object SessionInvalidated : SessionEvent()
}