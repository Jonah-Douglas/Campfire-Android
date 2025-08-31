package com.example.campfire.onboarding.profile_setup.presentation

import androidx.compose.ui.text.input.TextFieldValue
import com.example.campfire.core.presentation.UserMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


/**
 * Defines the contract between the Onboarding UI (Screens) and the OnboardingViewModel.
 */
interface ProfileSetupContract : IProfileSetupScreenActions, IProfileSetupScreenState

/**
 * Defines actions that the Onboarding UI can trigger on the OnboardingViewModel.
 */
interface IProfileSetupScreenActions {
    
    /**
     * Called when the first name input value changes.
     * @param newName The new TextFieldValue for the first name.
     */
    fun onFirstNameChanged(newName: TextFieldValue)
    
    /**
     * Called when the last name input value changes.
     * @param newName The new TextFieldValue for the last name.
     */
    fun onLastNameChanged(newName: TextFieldValue)
    
    /**
     * Called when the email input value changes.
     * @param newEmail The new TextFieldValue for the email.
     */
    fun onEmailChanged(newEmail: TextFieldValue)
    
    /**
     * Called when the date of birth input value changes.
     * @param newDobInput The new TextFieldValue for the date of birth.
     */
    fun onDateOfBirthInputChanged(newDobInput: TextFieldValue)
    
    /**
     * Called when the user changes the notification preference.
     * @param enabled True if notifications are enabled, false otherwise.
     */
    fun onEnableNotificationsChanged(enabled: Boolean)
    
    /**
     * Called when the user attempts to complete their onboarding process.
     */
    fun completeUserOnboarding()
    
    /**
     * Clears the result of the last complete onboarding operation.
     */
    fun clearCompleteOnboardingResult()
}

interface IProfileSetupScreenState {
    
    /**
     * UI state related to the onboarding process.
     */
    val onboardingUIState: StateFlow<CompleteProfileUIState>
    
    /**
     * A flow of user-facing messages (Snackbars, Toasts) to be displayed by the UI.
     */
    val userMessages: Flow<UserMessage>
    
    /**
     * A flow of navigation events to be handled by the UI (Navigator).
     */
    val profileSetupNavigationEvents: Flow<ProfileSetupNavigationEvent>
}