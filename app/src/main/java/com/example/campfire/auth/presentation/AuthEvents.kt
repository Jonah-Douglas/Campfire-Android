package com.example.campfire.auth.presentation

import androidx.annotation.StringRes
import com.example.campfire.auth.presentation.navigation.AuthAction


sealed class UserMessage {
    data class Snackbar(@StringRes val messageResId: Int, val args: List<Any> = emptyList()) :
        UserMessage()
    
    data class Toast(@StringRes val messageResId: Int, val args: List<Any> = emptyList()) :
        UserMessage()
    
    data class SnackbarString(val message: String) : UserMessage()
    data class ToastString(val message: String) : UserMessage()
}

sealed class AuthNavigationEvent {
    object NavigateToPickCountry : AuthNavigationEvent()
    data class ToOTPVerifiedScreen(val phoneNumber: String, val originatingAction: AuthAction) :
        AuthNavigationEvent()
    
    object ToProfileCompletion : AuthNavigationEvent()
    object ToMainApp : AuthNavigationEvent()
}