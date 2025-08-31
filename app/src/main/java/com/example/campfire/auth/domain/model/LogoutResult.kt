package com.example.campfire.auth.domain.model


/**
 * Represents the result of an attempt to logout a user.
 */
sealed interface LogoutResult {
    data object Success : LogoutResult
    data class Network(val message: String?) : LogoutResult
    data class Generic(val code: Int? = null, val message: String?) : LogoutResult
}