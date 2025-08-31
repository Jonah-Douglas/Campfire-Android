package com.example.campfire.auth.presentation

import com.example.campfire.auth.domain.model.AuthAction


sealed class AuthNavigationEvent {
    data class ToEnterPhoneNumberScreen(val action: AuthAction) : AuthNavigationEvent()
    object NavigateToPickCountry : AuthNavigationEvent()
    data class ToOTPVerificationScreen(val phoneNumber: String, val originatingAction: AuthAction) :
        AuthNavigationEvent()
    
    object ToOnboarding : AuthNavigationEvent()
    object ToFeed : AuthNavigationEvent()
}