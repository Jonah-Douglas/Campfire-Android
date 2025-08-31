package com.example.campfire.auth.presentation

import androidx.compose.ui.text.input.TextFieldValue
import com.example.campfire.auth.domain.model.AuthAction
import com.example.campfire.core.presentation.UserMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


/**
 * Defines the contract between the Authentication UI (Screens) and the ViewModel.
 */
interface AuthContract : IAuthScreenActions, IAuthScreenState

/**
 * Defines actions that the UI can trigger on the ViewModel.
 */
interface IAuthScreenActions {
    
    // --- Phone Number Input Changes ---
    
    /**
     * Called when a new country/region is selected by the user.
     * @param regionCode The selected region code (e.g., "US", "GB").
     */
    fun onRegionSelected(regionCode: String)
    
    /**
     * Called when the national (local) phone number input value changes.
     * @param newNationalNumber The new TextFieldValue for the national number.
     */
    fun onNationalNumberInputValueChanged(newNationalNumber: TextFieldValue)
    
    
    /**
     * Called to clear any input-related errors, typically when the user starts typing again.
     */
    fun clearErrorsOnInput()
    
    // --- OTP Sending ---
    
    /**
     * Called when the user attempts to send an OTP to the entered phone number.
     */
    fun attemptSendOTP()
    
    /**
     * Called when the user requests to resend the OTP.
     */
    fun resendOTP()
    
    // --- Context Setting ---
    
    /**
     * Sets the current operational context for authentication (e.g., Login or Register).
     * This is crucial for guiding the authentication flow.
     * @param action The authentication action (e.g., LOGIN, REGISTER).
     * @param phoneNumberE164 The E.164 formatted phone number, if relevant to the context. Log hashed.
     * @param isNewContext True if this call establishes a completely new operational context,
     *                     false if it's an update to an existing one.
     */
    fun setAuthOperationContext(action: AuthAction, phoneNumberE164: String?, isNewContext: Boolean)
    
    /**
     * Clears the result of the last send OTP operation.
     * Useful for resetting UI state after navigating away or handling the result.
     */
    fun handleAuthAction(action: AuthAction)
    
    /**
     * Clears the result of the last send OTP operation.
     * Useful for resetting UI state after navigating away or handling the result.
     */
    fun clearSendOTPResult()
    
    // --- OTP Verification ---
    
    /**
     * Called when the OTP code input value changes.
     * @param newCode The new TextFieldValue for the OTP code.
     */
    fun onOTPCodeChanged(newCode: TextFieldValue)
    
    
    /**
     * Called when the user attempts to verify the entered OTP.
     */
    fun verifyOTP()
    
    
    /**
     * Clears the result of the last verify OTP operation.
     */
    fun clearVerifyOTPResult()
}

/**
 * Defines the state exposed by the ViewModel that the UI can observe.
 * Changes to these StateFlows/Flows in the ViewModel should be logged to trace UI updates.
 */
interface IAuthScreenState {
    /**
     * UI state related to the OTP sending process.
     */
    val sendOTPUIState: StateFlow<SendOTPUIState>
    
    /**
     * UI state related to the OTP verification process.
     */
    val verifyOTPUIState: StateFlow<VerifyOTPUIState>
    
    /**
     * The phone number (E.164 format) for which an OTP has been successfully sent (or UserAlreadyExists occurred)
     * and is awaiting verification. This is used to pass the phone number to the
     * OTP verification screen. It should be cleared if OTP verification fails or
     * if the user navigates away/restarts the phone number entry process.
     */
    val currentPhoneNumberForVerification: StateFlow<String?>
    
    /**
     * A flow of user-facing messages (Snackbars, Toasts) to be displayed by the UI.
     */
    val userMessages: Flow<UserMessage>
    
    /**
     * A flow of navigation events to be handled by the UI (Navigator).
     */
    val authNavigationEvents: Flow<AuthNavigationEvent>
}