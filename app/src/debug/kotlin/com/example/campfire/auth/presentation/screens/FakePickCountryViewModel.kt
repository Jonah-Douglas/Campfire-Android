package com.example.campfire.auth.presentation.screens // Or your chosen debug/preview package

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.example.campfire.auth.presentation.AuthContract
import com.example.campfire.auth.presentation.AuthNavigationEvent
import com.example.campfire.auth.presentation.CompleteProfileUIState
import com.example.campfire.auth.presentation.CountryUIModel
import com.example.campfire.auth.presentation.SendOTPUIState
import com.example.campfire.auth.presentation.UserMessage
import com.example.campfire.auth.presentation.VerifyOTPUIState
import com.example.campfire.auth.presentation.navigation.AuthAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow


// Default list of countries for previews if no specific state is provided
val defaultPreviewCountries = listOf(
    CountryUIModel("United States", "US", "+1", "🇺🇸"),
    CountryUIModel("United Kingdom", "GB", "+44", "🇬🇧"),
    CountryUIModel("Canada", "CA", "+1", "🇨🇦"),
    CountryUIModel("Australia", "AU", "+61", "🇦🇺"),
    CountryUIModel("India", "IN", "+91", "🇮🇳"),
    CountryUIModel("Germany", "DE", "+49", "🇩🇪"),
    CountryUIModel("France", "FR", "+33", "🇫🇷"),
    CountryUIModel("Japan", "JP", "+81", "🇯🇵"),
    CountryUIModel("Brazil", "BR", "+55", "🇧🇷"),
    CountryUIModel("South Africa", "ZA", "+27", "🇿🇦")
)

class FakePickCountryViewModel(
    initialSendOTPState: SendOTPUIState = SendOTPUIState(availableCountries = defaultPreviewCountries)
) : ViewModel(), AuthContract {
    
    private val _fakeSendOTPUIState = MutableStateFlow(initialSendOTPState)
    override val sendOTPUIState: StateFlow<SendOTPUIState> = _fakeSendOTPUIState
    
    // --- Implement other AuthContract members with defaults or no-ops ---
    // (Similar to FakeEnterPhoneNumberViewModel for consistency)
    
    private val _userMessageChannel = MutableSharedFlow<UserMessage>()
    override val userMessages: Flow<UserMessage> = _userMessageChannel.asSharedFlow()
    
    private val _authNavigationEventChannel = MutableSharedFlow<AuthNavigationEvent>()
    override val authNavigationEvents: Flow<AuthNavigationEvent> =
        _authNavigationEventChannel.asSharedFlow()
    
    private val _fakeCurrentPhoneNumberForOTP = MutableStateFlow<String?>(null)
    override val currentPhoneNumberForVerification: StateFlow<String?> =
        _fakeCurrentPhoneNumberForOTP.asStateFlow()
    
    private val _fakeVerifyOTPUIState = MutableStateFlow(VerifyOTPUIState())
    override val verifyOTPUIState: StateFlow<VerifyOTPUIState> = _fakeVerifyOTPUIState
    
    private val _fakeCompleteProfileUiState = MutableStateFlow(CompleteProfileUIState())
    override val completeProfileUIState: StateFlow<CompleteProfileUIState> =
        _fakeCompleteProfileUiState
    
    override fun onRegionSelected(regionCode: String) {}
    override fun onNationalNumberInputValueChanged(newNationalNumber: TextFieldValue) {}
    override fun clearErrorsOnInput() {}
    override fun attemptSendOTP() {}
    override fun resendOTP() {}
    override fun setAuthOperationContext(
        action: AuthAction,
        phoneNumberE164: String?,
        isNewContext: Boolean
    ) {
    }
    
    override fun clearSendOTPResult() {}
    override fun onOTPCodeChanged(newCode: TextFieldValue) {}
    override fun verifyOTP() {}
    override fun clearVerifyOTPResult() {}
    override fun onFirstNameChanged(newName: TextFieldValue) {}
    override fun onLastNameChanged(newName: TextFieldValue) {}
    override fun onEmailChanged(newEmail: TextFieldValue) {}
    override fun onDateOfBirthInputChanged(newDobInput: TextFieldValue) {}
    override fun onEnableNotificationsChanged(enabled: Boolean) {}
    override fun completeUserProfile() {}
    override fun clearCompleteProfileResult() {}
}
