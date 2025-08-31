package com.example.campfire.auth.presentation.screens

import androidx.compose.ui.text.input.TextFieldValue
import com.example.campfire.auth.domain.model.AuthAction
import com.example.campfire.auth.presentation.AuthContract
import com.example.campfire.auth.presentation.AuthNavigationEvent
import com.example.campfire.auth.presentation.SendOTPUIState
import com.example.campfire.auth.presentation.VerifyOTPUIState
import com.example.campfire.core.presentation.UserMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow


class FakeVerifyOTPViewModel(
    initialVerifyState: VerifyOTPUIState = VerifyOTPUIState(),
    initialSendState: SendOTPUIState = SendOTPUIState(),
    initialPhoneNumber: String? = null,
) : AuthContract {
    override val sendOTPUIState: StateFlow<SendOTPUIState> =
        MutableStateFlow(initialSendState)
    override val verifyOTPUIState: StateFlow<VerifyOTPUIState> =
        MutableStateFlow(initialVerifyState)
    private val _fakeCurrentPhoneNumber = MutableStateFlow(initialPhoneNumber)
    override val currentPhoneNumberForVerification: StateFlow<String?> = _fakeCurrentPhoneNumber
    override val userMessages: Flow<UserMessage> = emptyFlow()
    override val authNavigationEvents: Flow<AuthNavigationEvent> = emptyFlow()
    
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
    override fun handleAuthAction(action: AuthAction) {}
}