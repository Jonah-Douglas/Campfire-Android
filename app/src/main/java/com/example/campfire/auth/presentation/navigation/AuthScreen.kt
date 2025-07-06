package com.example.campfire.auth.presentation.navigation

import com.example.campfire.core.presentation.navigation.NavigationDestination


/**
 * Defines the screen destinations within the authentication feature graph.
 */
sealed class AuthScreen(override val route: String) : NavigationDestination {
    object Entry : AuthScreen("entry")
    object Login : AuthScreen("login")
    object Register : AuthScreen("register")
    object EmailVerification : AuthScreen("email_verification")
    object PhoneVerification : AuthScreen("phone_verification")
}