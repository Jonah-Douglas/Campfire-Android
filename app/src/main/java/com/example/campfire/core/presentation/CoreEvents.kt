package com.example.campfire.core.presentation

import androidx.annotation.StringRes


sealed class UserMessage {
    data class Snackbar(@StringRes val messageResId: Int, val args: List<Any> = emptyList()) :
        UserMessage()
    
    data class Toast(@StringRes val messageResId: Int, val args: List<Any> = emptyList()) :
        UserMessage()
    
    data class SnackbarString(val message: String) : UserMessage()
    data class ToastString(val message: String) : UserMessage()
}