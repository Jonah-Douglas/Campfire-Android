package com.example.campfire.auth.presentation

import androidx.compose.ui.text.input.TextFieldValue
import com.example.campfire.auth.presentation.navigation.AuthAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


interface AuthContract : IAuthScreenActions, IAuthScreenState

interface IAuthScreenActions {
    // --- Phone Number Input Changes ---
    fun onRegionSelected(regionCode: String)
    fun onNationalNumberInputValueChanged(newNationalNumber: TextFieldValue)
    
    // --- OTP Sending ---
    fun attemptSendOTP()
    fun resendOTP()
    
    // --- Context Setting ---
    fun setAuthOperationContext(action: AuthAction, phoneNumberE164: String?, isNewContext: Boolean)
    fun clearSendOTPResult()
    
    // --- OTP Verification ---
    fun onOTPCodeChanged(newCode: TextFieldValue)
    fun verifyOTP()
    fun clearVerifyOTPResult()
    
    // --- Profile Completion ---
    fun onFirstNameChanged(newName: TextFieldValue)
    fun onLastNameChanged(newName: TextFieldValue)
    fun onEmailChanged(newEmail: TextFieldValue)
    fun onDateOfBirthInputChanged(newDobInput: TextFieldValue)
    fun onEnableNotificationsChanged(enabled: Boolean)
    fun completeUserProfile()
    fun clearCompleteProfileResult()
}

interface IAuthScreenState {
    val sendOTPUIState: StateFlow<SendOTPUIState>
    val verifyOTPUIState: StateFlow<VerifyOTPUIState>
    val completeProfileUIState: StateFlow<CompleteProfileUIState>
    
    /**
     * The phone number for which an OTP has been successfully sent (or UserAlreadyExists occurred)
     * and is awaiting verification. This is used to pass the phone number to the
     * OTP verification screen. It should be cleared if OTP verification fails or
     * if the user navigates away/restarts the phone number entry process.
     */
    val currentPhoneNumberForVerification: StateFlow<String?>
    val userMessages: Flow<UserMessage>
    val authNavigationEvents: Flow<AuthNavigationEvent>
}