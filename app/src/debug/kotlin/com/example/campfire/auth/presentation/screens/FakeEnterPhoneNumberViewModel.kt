package com.example.campfire.auth.presentation.screens // Or your appropriate test/preview package

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.example.campfire.auth.domain.repository.SendOTPResult
import com.example.campfire.auth.presentation.AuthContract
import com.example.campfire.auth.presentation.AuthNavigationEvent
import com.example.campfire.auth.presentation.CompleteProfileUIState
import com.example.campfire.auth.presentation.SendOTPUIState
import com.example.campfire.auth.presentation.UserMessage
import com.example.campfire.auth.presentation.VerifyOTPUIState
import com.example.campfire.auth.presentation.navigation.AuthAction
import com.example.campfire.core.domain.model.PhoneNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale


class FakeEnterPhoneNumberViewModel(
    initialState: SendOTPUIState = SendOTPUIState(
        selectedRegionCode = Locale.getDefault().country.takeIf { it.isNotBlank() } ?: "US",
        displayCountryDialCode = "+" + (PhoneNumber.getDialingCodeForRegion(
            Locale.getDefault().country.takeIf { it.isNotBlank() } ?: "US"
        ).takeIf { it != 0 } ?: "1")
    )
) : ViewModel(), AuthContract {
    
    private val _fakeSendOTPUIState = MutableStateFlow(initialState)
    override val sendOTPUIState: StateFlow<SendOTPUIState> = _fakeSendOTPUIState.asStateFlow()
    
    private val _userMessageChannel = MutableSharedFlow<UserMessage>()
    override val userMessages: Flow<UserMessage> = _userMessageChannel.asSharedFlow()
    
    private val _authNavigationEventChannel = MutableSharedFlow<AuthNavigationEvent>()
    override val authNavigationEvents: Flow<AuthNavigationEvent> =
        _authNavigationEventChannel.asSharedFlow()
    
    private val _fakeCurrentPhoneNumberForOTP = MutableStateFlow<String?>(null)
    override val currentPhoneNumberForVerification: StateFlow<String?> =
        _fakeCurrentPhoneNumberForOTP.asStateFlow()
    
    
    // --- SendOTPUIState related methods ---
    override fun onRegionSelected(regionCode: String) {
        val dialCode = PhoneNumber.getDialingCodeForRegion(regionCode)
        _fakeSendOTPUIState.update {
            it.copy(
                selectedRegionCode = regionCode.uppercase(Locale.ROOT),
                displayCountryDialCode = if (dialCode != 0) "+$dialCode" else it.displayCountryDialCode,
                validationError = if (dialCode == 0) "Preview: Invalid region selected" else null,
                sendOTPResult = null
            )
        }
    }
    
    override fun onNationalNumberInputValueChanged(newNationalNumber: TextFieldValue) {
        _fakeSendOTPUIState.update {
            it.copy(
                nationalNumberInput = newNationalNumber,
                validationError = null,
                sendOTPResult = null
            )
        }
    }
    
    override fun attemptSendOTP() {
        val currentState = _fakeSendOTPUIState.value
        val nationalNum = currentState.nationalNumberInput.text
        
        // Basic validation for preview purposes
        if (currentState.selectedRegionCode.isBlank() || currentState.displayCountryDialCode.isBlank()) {
            _fakeSendOTPUIState.update {
                it.copy(
                    isLoading = false,
                    validationError = "Preview: Country code is missing."
                )
            }
            return
        }
        
        if (nationalNum.length < 7 || !nationalNum.all { it.isDigit() }) { // Example validation
            _fakeSendOTPUIState.update {
                it.copy(
                    isLoading = false,
                    validationError = "Preview: Phone number is too short or invalid."
                )
            }
            return
        }
        
        // Simulate successful attempt
        _fakeSendOTPUIState.update { it.copy(isLoading = true, validationError = null) }
        
        val e164PreviewNumber = "${currentState.displayCountryDialCode}${nationalNum}"
        _fakeCurrentPhoneNumberForOTP.value = e164PreviewNumber
    }
    
    override fun setAuthOperationContext(
        action: AuthAction,
        phoneNumberE164: String?,
        isNewContext: Boolean
    ) {
        if (isNewContext) {
            val initialPhoneNumber = if (!phoneNumberE164.isNullOrBlank()) {
                PhoneNumber.fromE164(phoneNumberE164)
            } else {
                null
            }
            
            val defaultDeviceRegion = Locale.getDefault().country.takeIf { it.isNotBlank() } ?: "US"
            val region = initialPhoneNumber?.regionCode ?: defaultDeviceRegion
            val dialCode = PhoneNumber.getDialingCodeForRegion(region)
            val displayDialCode = if (dialCode != 0) "+$dialCode"
            else PhoneNumber.getDialingCodeForRegion("US").let { "+$it" }
            
            _fakeSendOTPUIState.value = _fakeSendOTPUIState.value.copy(
                selectedRegionCode = region,
                displayCountryDialCode = displayDialCode,
                nationalNumberInput = TextFieldValue(
                    initialPhoneNumber?.nationalNumber?.toString() ?: ""
                ),
                parsedPhoneNumber = initialPhoneNumber,
                sendOTPResult = null,
                validationError = null,
                isLoading = false
            )
            _fakeCurrentPhoneNumberForOTP.value =
                phoneNumberE164
        }
    }
    
    override fun clearSendOTPResult() {
        _fakeSendOTPUIState.update {
            it.copy(sendOTPResult = null, validationError = null, isLoading = false)
        }
    }
    
    private val _fakeVerifyOTPUIState = MutableStateFlow(VerifyOTPUIState())
    override val verifyOTPUIState: StateFlow<VerifyOTPUIState> = _fakeVerifyOTPUIState.asStateFlow()
    
    private val _fakeCompleteProfileUiState = MutableStateFlow(CompleteProfileUIState())
    override val completeProfileUIState: StateFlow<CompleteProfileUIState> =
        _fakeCompleteProfileUiState.asStateFlow()
    
    override fun onOTPCodeChanged(newCode: TextFieldValue) {}
    override fun resendOTP() {}
    override fun verifyOTP() {}
    override fun clearVerifyOTPResult() {}
    
    override fun onFirstNameChanged(newName: TextFieldValue) {}
    override fun onLastNameChanged(newName: TextFieldValue) {}
    override fun onEmailChanged(newEmail: TextFieldValue) {}
    override fun onDateOfBirthInputChanged(newDobInput: TextFieldValue) {}
    override fun onEnableNotificationsChanged(enabled: Boolean) {}
    override fun completeUserProfile() {}
    override fun clearCompleteProfileResult() {}
    
    
    // --- Helper methods for testing/previews ---
    
    /**
     * Directly sets the SendOTPUIState for use in Previews or tests.
     */
    fun setSendOTPUIState(newState: SendOTPUIState) {
        _fakeSendOTPUIState.value = newState
        // Update related states if necessary
        if (newState.sendOTPResult is SendOTPResult.Success || newState.sendOTPResult is SendOTPResult.UserAlreadyExists) {
            // Attempt to derive phone number if possible, or require it to be set explicitly for the preview
            val e164 = newState.parsedPhoneNumber?.e164Format
            _fakeCurrentPhoneNumberForOTP.value = e164
        } else if (newState.sendOTPResult == null && newState.validationError == null) {
            // If state is reset, clear current phone number unless parsedPhoneNumber is still valid
            _fakeCurrentPhoneNumberForOTP.value = newState.parsedPhoneNumber?.e164Format
        }
    }
    
    /**
     * Directly sets the value for currentPhoneNumberForVerification.
     * Useful for previews of screens that depend on this, like VerifyOTPScreen,
     * or for AppNavigation previews testing navigation logic.
     */
    fun setFakeCurrentPhoneNumberForVerification(phone: String?) {
        _fakeCurrentPhoneNumberForOTP.value = phone
    }
    
    /**
     * Simulates emitting a UserMessage.
     */
    suspend fun emitUserMessage(message: UserMessage) {
        _userMessageChannel.emit(message)
    }
    
    /**
     * Simulates emitting an AuthNavigationEvent.
     */
    suspend fun emitAuthNavigationEvent(event: AuthNavigationEvent) {
        _authNavigationEventChannel.emit(event)
    }
}
