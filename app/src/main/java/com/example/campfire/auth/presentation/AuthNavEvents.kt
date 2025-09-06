package com.example.campfire.auth.presentation

import com.example.campfire.auth.domain.model.AuthAction


/**
 * Represents navigation events within the authentication feature.
 * These events are typically triggered by user actions or ViewModel logic
 * to guide the user through the authentication flow.
 */
sealed class AuthNavigationEvent {
    /**
     * Navigates to the screen where the user is prompted to enter their phone number.
     * @param action The authentication action being performed (e.g., SIGN_UP, LOGIN).
     */
    data class ToEnterPhoneNumberScreen(val action: AuthAction) : AuthNavigationEvent()
    
    /**
     * Navigates to the screen where the user can select their country and country code.
     */
    object NavigateToPickCountry : AuthNavigationEvent()
    
    /**
     * Navigates to the screen where the user enters the One-Time Password (OTP)
     * sent to their phone.
     * @param phoneNumber The phone number to which the OTP was sent.
     * @param originatingAction The initial authentication action that triggered OTP verification.
     */
    data class ToOTPVerificationScreen(val phoneNumber: String, val originatingAction: AuthAction) :
        AuthNavigationEvent()
    
    /**
     * Navigates to the profile setup screen, typically for new users after successful
     * phone number verification.
     */
    object ToProfileSetup : AuthNavigationEvent()
    
    /**
     * Navigates to app setup screens that need to be completed after
     * profile setup or for returning users if setup is incomplete.
     */
    object ToAppSetup : AuthNavigationEvent()
    
    /**
     * Navigates to the main content feed of the application, signifying successful
     * completion of the authentication and setup flow.
     */
    object ToFeed : AuthNavigationEvent()
}