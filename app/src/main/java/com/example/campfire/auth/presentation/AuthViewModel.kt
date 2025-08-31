package com.example.campfire.auth.presentation

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campfire.R
import com.example.campfire.auth.domain.model.AuthAction
import com.example.campfire.auth.domain.model.SendOTPResult
import com.example.campfire.auth.domain.model.VerifyOTPResult
import com.example.campfire.auth.domain.usecase.SendOTPUseCase
import com.example.campfire.auth.domain.usecase.VerifyOTPUseCase
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.domain.model.PhoneNumber
import com.example.campfire.core.presentation.UserMessage
import com.example.campfire.core.presentation.UserMessage.Snackbar
import com.example.campfire.core.presentation.UserMessage.SnackbarString
import com.example.campfire.core.presentation.UserMessage.Toast
import com.example.campfire.core.presentation.UserMessage.ToastString
import com.example.campfire.core.presentation.utils.getFlagEmojiForRegionCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject


data class CountryUIModel(
    val name: String,        // e.g., "United States"
    val regionCode: String,  // e.g., "US"
    val dialCode: String,    // e.g., "+1"
    val flagEmoji: String    // e.g., "🇺🇸"
)

data class SendOTPUIState(
    val currentAuthAction: AuthAction = AuthAction.LOGIN,
    val selectedRegionCode: String = "",
    val displayCountryDialCode: String = "+1",
    val nationalNumberInput: TextFieldValue = TextFieldValue(""),
    val isLoading: Boolean = false,
    val sendOTPResult: SendOTPResult? = null,
    val parsedPhoneNumber: PhoneNumber? = null,
    val validationError: String? = null,
    val availableCountries: List<CountryUIModel> = emptyList()
)

data class VerifyOTPUIState(
    val otpCode: TextFieldValue = TextFieldValue(""),
    val isLoading: Boolean = false,
    val verifyOTPResult: VerifyOTPResult? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sendOTPUseCase: SendOTPUseCase,
    private val verifyOTPUseCase: VerifyOTPUseCase
) : ViewModel(), AuthContract {
    
    private val _sendOTPUIState = MutableStateFlow(SendOTPUIState())
    override val sendOTPUIState: StateFlow<SendOTPUIState> = _sendOTPUIState.asStateFlow()
    
    private val _verifyOTPUIState = MutableStateFlow(VerifyOTPUIState())
    override val verifyOTPUIState: StateFlow<VerifyOTPUIState> = _verifyOTPUIState.asStateFlow()
    
    private val _phoneNumberForVerification = MutableStateFlow<String?>(null)
    override val currentPhoneNumberForVerification: StateFlow<String?>
        get() = _phoneNumberForVerification.asStateFlow()
    
    private val _userMessageChannel = Channel<UserMessage>(Channel.BUFFERED)
    override val userMessages: Flow<UserMessage> = _userMessageChannel.receiveAsFlow()
    
    private val _authNavigationEventChannel = Channel<AuthNavigationEvent>(Channel.BUFFERED)
    override val authNavigationEvents: Flow<AuthNavigationEvent> =
        _authNavigationEventChannel.receiveAsFlow()
    
    private var currentAuthActionInternal: AuthAction = AuthAction.LOGIN
    private var currentFullPhoneNumberTarget: String? = null
    
    // Simple hashing function for PII
    private fun String.toSha256(): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(this.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
            .take(16) // Shorten for logs
    }
    
    init {
        Firelog.i("ViewModel initialized. Loading available countries.")
        loadAvailableCountries()
        val defaultRegion = Locale.getDefault().country
        val countries = _sendOTPUIState.value.availableCountries
        val defaultCountryIsSupported = countries.any { it.regionCode == defaultRegion }
        
        if (defaultRegion.isNotBlank() && defaultCountryIsSupported) {
            Firelog.d("Default region '$defaultRegion' is supported. Applying.")
            onRegionSelected(defaultRegion)
        } else {
            val fallbackRegion = countries.firstOrNull()?.regionCode ?: "US"
            Firelog.i("Applying fallback region '$fallbackRegion'.")
            onRegionSelected(fallbackRegion)
            
            if (defaultRegion.isNotBlank() && !defaultCountryIsSupported) {
                Firelog.w("Device default region '$defaultRegion' is not in the configured available countries, used fallback '$fallbackRegion'.")
            } else if (defaultRegion.isBlank()) {
                Firelog.d("Device default region is blank, used fallback '$fallbackRegion'.")
            }
        }
    }
    
    /**
     * Loads the available countries for phone number selection.
     */
    private fun loadAvailableCountries() {
        Firelog.d("loadAvailableCountries called.")
        val countries = PhoneNumber.getSupportedRegions().mapNotNull { regionCode ->
            val localeForRegion = Locale.Builder().setRegion(regionCode).build()
            val countryName = localeForRegion.displayCountry
            
            val dialCode = PhoneNumber.getDialingCodeForRegion(regionCode)
            if (dialCode != 0) {
                CountryUIModel(
                    name = countryName.ifEmpty { regionCode },
                    regionCode = regionCode.uppercase(Locale.ROOT),
                    dialCode = "+$dialCode",
                    flagEmoji = getFlagEmojiForRegionCode(regionCode)
                )
            } else {
                Firelog.v("Region '$regionCode' has no dial code, excluding.")
                null
            }
        }.sortedBy { it.name }
        
        _sendOTPUIState.update {
            Firelog.d("Updating SendOTPUIState: ${countries.size} countries loaded.")
            it.copy(availableCountries = countries)
        }
    }
    
    override fun onRegionSelected(regionCode: String) {
        Firelog.i("onRegionSelected: New region '$regionCode'")
        val dialCode = PhoneNumber.getDialingCodeForRegion(regionCode)
        if (dialCode != 0) {
            _sendOTPUIState.update {
                Firelog.d("Updating SendOTPUIState: selectedRegionCode='$regionCode', displayCountryDialCode='+${dialCode}', clearing errors.")
                it.copy(
                    selectedRegionCode = regionCode.uppercase(Locale.ROOT),
                    displayCountryDialCode = "+$dialCode",
                    validationError = null,
                    sendOTPResult = null
                )
            }
        } else {
            val errorMsg = "Could not set country code for $regionCode"
            _sendOTPUIState.update {
                Firelog.w("Updating SendOTPUIState: validationError='$errorMsg' for region '$regionCode'")
                it.copy(validationError = errorMsg)
            }
            Firelog.w("Could not get dial code for region: $regionCode")
        }
    }
    
    override fun onNationalNumberInputValueChanged(newNationalNumber: TextFieldValue) {
        Firelog.v("onNationalNumberInputValueChanged: new text length ${newNationalNumber.text.length}, selection ${newNationalNumber.selection}")
        _sendOTPUIState.update {
            it.copy(
                nationalNumberInput = newNationalNumber,
                validationError = null,
                sendOTPResult = null
            )
        }
    }
    
    override fun clearErrorsOnInput() {
        if (_sendOTPUIState.value.validationError != null || _sendOTPUIState.value.sendOTPResult is SendOTPResult.Generic) {
            _sendOTPUIState.update {
                it.copy(
                    validationError = null,
                    sendOTPResult = if (it.sendOTPResult is SendOTPResult.Generic) null else it.sendOTPResult
                )
            }
        }
    }
    
    /**
     * Sets the operational context for authentication actions (sending or verifying OTP).
     * This should be called when the relevant screen (EnterPhoneNumberScreen or VerifyOTPScreen)
     * is initialized or its key arguments (action, phoneNumber) change.
     *
     * @param action The current authentication action (LOGIN, REGISTER).
     * @param phoneNumberE164 The phone number associated with the current action.
     * @param isNewContext If true, implies a full reset of related UI states.
     */
    override fun setAuthOperationContext(
        action: AuthAction,
        phoneNumberE164: String?,
        isNewContext: Boolean
    ) {
        val phoneHashForLog = phoneNumberE164?.toSha256() ?: "null"
        Firelog.i("setAuthOperationContext: action=$action, phoneE164(hash)=$phoneHashForLog, isNewContext=$isNewContext. Current countries: ${_sendOTPUIState.value.availableCountries.size}")
        
        currentAuthActionInternal = action
        currentFullPhoneNumberTarget = phoneNumberE164
        _phoneNumberForVerification.value = phoneNumberE164
        
        if (isNewContext) {
            Firelog.d("setAuthOperationContext: New context - resetting phone input and verify OTP states.")
            val initialPhoneNumber = if (!phoneNumberE164.isNullOrBlank()) {
                PhoneNumber.fromE164(phoneNumberE164)
            } else {
                null
            }
            
            // Determine the region to select. It should ideally use the already loaded countries.
            val currentCountries = _sendOTPUIState.value.availableCountries
            val defaultDeviceRegion = Locale.getDefault().country.takeIf { it.isNotBlank() } ?: "US"
            
            var regionToSelect = initialPhoneNumber?.regionCode
                ?: _sendOTPUIState.value.selectedRegionCode.takeIf { it.isNotBlank() }
                ?: defaultDeviceRegion
            
            // Validate regionToSelect against available countries
            if (currentCountries.isNotEmpty() && !currentCountries.any {
                    it.regionCode.equals(
                        regionToSelect,
                        ignoreCase = true
                    )
                }) {
                Firelog.d("Region to select '$regionToSelect' not in current countries. Defaulting to first available.")
                regionToSelect = currentCountries.first().regionCode
            } else if (currentCountries.isEmpty() && PhoneNumber.getDialingCodeForRegion(
                    regionToSelect
                ) == 0
            ) {
                Firelog.d("Region to select '$regionToSelect' not valid and no countries loaded. Defaulting to US.")
                regionToSelect = "US" // Fallback if no countries and initial region is invalid
            }
            val dialCode = PhoneNumber.getDialingCodeForRegion(regionToSelect)
            
            _sendOTPUIState.update { currentState ->
                val newNationalNumberText = initialPhoneNumber?.nationalNumber?.toString()
                    ?: currentState.nationalNumberInput.text
                Firelog.d(
                    "Updating SendOTPUIState for new context: action=$action, selectedRegion=$regionToSelect, dialCode=+$dialCode, nationalNum='${
                        newNationalNumberText.take(
                            5
                        )
                    }...' (len ${newNationalNumberText.length})"
                )
                
                currentState.copy(
                    currentAuthAction = action,
                    selectedRegionCode = regionToSelect.uppercase(Locale.ROOT),
                    displayCountryDialCode = if (dialCode != 0) "+$dialCode" else PhoneNumber.getDialingCodeForRegion(
                        "US"
                    ).let { "+$it" },
                    nationalNumberInput = TextFieldValue(
                        initialPhoneNumber?.nationalNumber?.toString()
                            ?: currentState.nationalNumberInput.text
                    ),
                    parsedPhoneNumber = initialPhoneNumber ?: currentState.parsedPhoneNumber,
                    isLoading = false,
                    sendOTPResult = null,
                    validationError = null,
                )
            }
            
            _verifyOTPUIState.value = VerifyOTPUIState()
            Firelog.d("VerifyOTPUIState reset for new context.")
        } else {
            _sendOTPUIState.update { currentState ->
                Firelog.d("Updating SendOTPUIState for existing context: new action=$action")
                currentState.copy(currentAuthAction = action)
            }
        }
        Firelog.d("setAuthOperationContext END. Countries AFTER: ${_sendOTPUIState.value.availableCountries.size}")
    }
    
    // --- Send OTP ---
    
    override fun attemptSendOTP() {
        val currentState = _sendOTPUIState.value
        val nationalNum = currentState.nationalNumberInput.text
        val phoneForLog = "${currentState.displayCountryDialCode}${nationalNum}".toSha256()
        
        Firelog.i("attemptSendOTP: Triggered for phone (hash): $phoneForLog, action: $currentAuthActionInternal")
        
        if (currentState.selectedRegionCode.isBlank() || currentState.displayCountryDialCode.isBlank()) {
            val errorMsg = "Please select a country code."
            Firelog.w("attemptSendOTP: Validation failed - $errorMsg")
            _sendOTPUIState.update {
                Firelog.d("Updating SendOTPUIState: validationError='$errorMsg'")
                it.copy(validationError = errorMsg)
            }
            viewModelScope.launch { _userMessageChannel.send(SnackbarString(errorMsg)) }
            return
        }
        if (nationalNum.isBlank()) {
            val errorMsg = "Please enter your phone number."
            Firelog.w("attemptSendOTP: Validation failed - $errorMsg")
            _sendOTPUIState.update {
                Firelog.d("Updating SendOTPUIState: validationError='$errorMsg'")
                it.copy(validationError = errorMsg)
            }
            viewModelScope.launch { _userMessageChannel.send(SnackbarString(errorMsg)) }
            return
        }
        val countryDialCodeNumeric = currentState.displayCountryDialCode.filter { it.isDigit() }
        val constructedPhoneNumber = PhoneNumber.fromCountryCodeAndNationalNumber(
            countryCodeString = countryDialCodeNumeric,
            nationalNumberString = nationalNum
        )
        
        if (!constructedPhoneNumber.isValid) {
            val errorMsg = "The phone number entered is not valid."
            Firelog.w("attemptSendOTP: Validation failed - $errorMsg for input (hash): $phoneForLog. Parsed: $constructedPhoneNumber")
            _sendOTPUIState.update {
                Firelog.d("Updating SendOTPUIState: validationError='$errorMsg', parsedPhoneNumber=$constructedPhoneNumber")
                it.copy(
                    validationError = errorMsg,
                    parsedPhoneNumber = constructedPhoneNumber
                )
            }
            viewModelScope.launch { _userMessageChannel.send(SnackbarString(errorMsg)) }
            return
        }
        
        // Number is valid, get E.164 format
        val e164NumberToSend = constructedPhoneNumber.e164Format
        if (e164NumberToSend == null) {
            val errorMsg =
                "Could not format valid phone number for sending OTP." // Should be R.string
            Firelog.e("attemptSendOTP: Phone number $constructedPhoneNumber marked valid but e164Format is null! Input (hash): $phoneForLog")
            _sendOTPUIState.update {
                Firelog.d("Updating SendOTPUIState: validationError='$errorMsg'")
                it.copy(validationError = errorMsg, isLoading = false)
            }
            viewModelScope.launch { _userMessageChannel.send(SnackbarString(errorMsg)) }
            return
        }
        
        // Log hash of the number being sent
        val e164HashForLog = e164NumberToSend.toSha256()
        Firelog.i("attemptSendOTP: Phone number is valid. Proceeding to send OTP to E.164 (hash): $e164HashForLog")
        
        _sendOTPUIState.update {
            Firelog.d("Updating SendOTPUIState: isLoading=true, clearing errors, setting parsedPhoneNumber=$constructedPhoneNumber")
            it.copy(
                isLoading = true,
                validationError = null,
                parsedPhoneNumber = constructedPhoneNumber,
                sendOTPResult = null
            )
        }
        
        currentFullPhoneNumberTarget = e164NumberToSend
        _phoneNumberForVerification.value = e164NumberToSend
        
        viewModelScope.launch {
            Firelog.d("Calling sendOTPUseCase for E.164 (hash): $e164HashForLog, action: $currentAuthActionInternal")
            val result = sendOTPUseCase(
                phoneNumber = constructedPhoneNumber.e164Format,
                authAction = currentAuthActionInternal
            )
            Firelog.i("sendOTPUseCase returned: ${result::class.simpleName}")
            
            _sendOTPUIState.update {
                Firelog.d("Updating SendOTPUIState: isLoading=false, sendOTPResult=${result::class.simpleName}")
                it.copy(isLoading = false, sendOTPResult = result)
            }
            
            if (result is SendOTPResult.Success) {
                Firelog.i("OTP Sent successfully to E.164 (hash): $e164HashForLog. Navigating to OTP verification.")
                _authNavigationEventChannel.send(
                    AuthNavigationEvent.ToOTPVerificationScreen(
                        phoneNumber = e164NumberToSend,
                        originatingAction = currentAuthActionInternal
                    )
                )
                Firelog.d("Sent AuthNavigationEvent: ToOTPVerifiedScreen for E.164 (hash): $e164HashForLog")
            } else if (result is SendOTPResult.Generic) {
                viewModelScope.launch {
                    _userMessageChannel.send(
                        SnackbarString(
                            result.message ?: ERROR_GENERIC
                        )
                    )
                }
                Firelog.w("SendOTPResult.Generic: ${result.message}")
            }
        }
    }
    
    override fun clearSendOTPResult() {
        _sendOTPUIState.update { it.copy(sendOTPResult = null) }
    }
    
    // --- Verify OTP ---
    
    override fun onOTPCodeChanged(newCode: TextFieldValue) {
        Firelog.v("onOTPCodeChanged: new OTP length ${newCode.text.length}, text (last char): '${newCode.text.lastOrNull()}'")
        if (newCode.text.length <= OTP_LENGTH && newCode.text.all { it.isDigit() }) {
            _verifyOTPUIState.update {
                it.copy(otpCode = newCode, verifyOTPResult = null)
            }
        }
    }
    
    override fun verifyOTP() {
        val otpCode = _verifyOTPUIState.value.otpCode.text
        val phoneToVerify = currentFullPhoneNumberTarget
        
        if (phoneToVerify == null) {
            Firelog.e("verifyOTP: currentFullPhoneNumberTarget is null. Cannot proceed.")
            viewModelScope.launch { _userMessageChannel.send(Snackbar(R.string.error_phone_invalid_generic)) }
            return
        }
        
        val action = currentAuthActionInternal
        val phoneHashForLog = phoneToVerify.toSha256()
        Firelog.i("verifyOTP: Triggered for phone (hash): $phoneHashForLog, action: $action, OTP length: ${otpCode.length}")
        
        viewModelScope.launch {
            _verifyOTPUIState.update {
                it.copy(
                    isLoading = true,
                    verifyOTPResult = null,
                    errorMessage = null
                )
            }
            val result =
                verifyOTPUseCase(
                    phoneNumber = phoneToVerify,
                    otpCode = otpCode,
                    authAction = action
                )
            Firelog.i("verifyOTPUseCase for phone (hash) $phoneHashForLog returned: ${result::class.simpleName}")
            _verifyOTPUIState.update { it.copy(isLoading = false, verifyOTPResult = result) }
            
            when (result) {
                is VerifyOTPResult.SuccessLogin -> {
                    Firelog.i("VerifyOTPResult.SuccessLogin for phone (hash) $phoneHashForLog. Navigating to Main App.")
                    _userMessageChannel.send(Toast(R.string.login_successful))
                    _authNavigationEventChannel.send(AuthNavigationEvent.ToFeed)
                }
                
                is VerifyOTPResult.SuccessRegistration -> {
                    Firelog.i("VerifyOTPResult.SuccessRegistration for phone (hash) $phoneHashForLog. Navigating to Onboarding.")
                    _userMessageChannel.send(Toast(R.string.otp_verified_proceed_profile))
                    _authNavigationEventChannel.send(AuthNavigationEvent.ToOnboarding)
                }
                
                // JD TODO: Make sure I am doing something with the isProfileComplete (might need to return the full user object from the backend, def need to see if the profile is complete and if the user completed app setup too)
                is VerifyOTPResult.SuccessButUserExistedDuringRegistration -> {
                    Firelog.i("VerifyOTPResult.SuccessButUserExistedDuringRegistration for phone (hash) $phoneHashForLog.")
                    val isProfileComplete =
                        false//result.isProfileComplete // Assuming result carries this
                    // OR checkUserRepository.isUserProfileComplete(result.userId)
                    
                    if (isProfileComplete) {
                        _userMessageChannel.send(ToastString("Welcome back! Logged you in."))
                        _authNavigationEventChannel.send(AuthNavigationEvent.ToFeed)
                    } else {
                        _userMessageChannel.send(ToastString("Welcome back! Let's finish your profile."))
                        _authNavigationEventChannel.send(AuthNavigationEvent.ToOnboarding)
                    }
                }
                
                // Handle errors
                is VerifyOTPResult.InvalidOTPFormat.Empty -> {
                    Firelog.w("VerifyOTPResult.InvalidOTPFormat for phone (hash) $phoneHashForLog.")
                    _userMessageChannel.send(Snackbar(R.string.error_otp_format_empty))
                }
                
                is VerifyOTPResult.InvalidOTPFormat.IncorrectLength -> {
                    Firelog.w("VerifyOTPResult.InvalidOTPFormat for phone (hash) $phoneHashForLog.")
                    _userMessageChannel.send(Snackbar(R.string.error_otp_format_length))
                }
                
                is VerifyOTPResult.InvalidOTPFormat.NonNumeric -> {
                    Firelog.w("VerifyOTPResult.InvalidOTPFormat for phone (hash) $phoneHashForLog.")
                    _userMessageChannel.send(Snackbar(R.string.error_otp_format_nonnumeric))
                }
                
                is VerifyOTPResult.OTPIncorrect -> {
                    Firelog.w("VerifyOTPResult.OTPIncorrect for phone (hash) $phoneHashForLog.")
                    _userMessageChannel.send(Snackbar(R.string.error_otp_incorrect))
                }
                
                is VerifyOTPResult.OTPExpired -> {
                    Firelog.w("VerifyOTPResult.OTPExpired for phone (hash) $phoneHashForLog.")
                    _userMessageChannel.send(Snackbar(R.string.error_otp_expired))
                }
                
                is VerifyOTPResult.RateLimited -> {
                    Firelog.w("VerifyOTPResult.RateLimited for phone (hash) $phoneHashForLog: ${result.message}")
                    _userMessageChannel.send(
                        SnackbarString(
                            result.message ?: ERROR_RATE_LIMITED_VERIFYING_OTP
                        )
                    )
                }
                
                is VerifyOTPResult.Network -> {
                    Firelog.w("VerifyOTPResult.Network for phone (hash) $phoneHashForLog: ${result.message}")
                    _userMessageChannel.send(
                        SnackbarString(
                            result.message ?: ERROR_NETWORK_VERIFY_OTP
                        )
                    )
                }
                
                is VerifyOTPResult.Generic -> {
                    Firelog.e("VerifyOTPResult.Generic for phone (hash) $phoneHashForLog: ${result.message}")
                    _userMessageChannel.send(
                        SnackbarString(
                            result.message ?: ERROR_GENERIC
                        )
                    )
                }
            }
        }
    }
    
    override fun resendOTP() {
        val phoneToResend = currentFullPhoneNumberTarget
        if (phoneToResend == null) {
            Firelog.e("resendOTP: currentFullPhoneNumberTarget is null. Cannot proceed.")
            viewModelScope.launch { _userMessageChannel.send(Snackbar(R.string.error_phone_invalid_generic)) }
            return
        }
        
        val action = currentAuthActionInternal
        val phoneHashForLog = phoneToResend.toSha256()
        Firelog.i("resendOTP: Triggered for phone (hash): $phoneHashForLog, action: $action")
        
        viewModelScope.launch {
            _sendOTPUIState.update { it.copy(isLoading = true, sendOTPResult = null) }
            val result = sendOTPUseCase(phoneToResend, action)
            Firelog.i("resendOTP (sendOTPUseCase) for phone (hash) $phoneHashForLog returned: ${result::class.simpleName}")
            _sendOTPUIState.update { it.copy(isLoading = false, sendOTPResult = result) }
            
            when (result) {
                is SendOTPResult.Success -> {
                    Firelog.i("OTP Resent successfully to E.164 (hash): $phoneHashForLog.")
                    _userMessageChannel.send(Toast(R.string.send_otp_successful))
                }
                
                is SendOTPResult.InvalidPhoneNumber -> _userMessageChannel.send(
                    Snackbar(
                        R.string.error_phone_invalid_generic
                    )
                )
                
                is SendOTPResult.RateLimited -> _userMessageChannel.send(
                    SnackbarString(
                        result.message ?: ERROR_RATE_LIMITED_SENDING_OTP
                    )
                )
                
                is SendOTPResult.UserAlreadyExists -> _userMessageChannel.send(
                    SnackbarString(
                        result.message ?: ERROR_PHONE_NUMBER_REGISTERED
                    )
                )
                
                is SendOTPResult.UserNotFound -> _userMessageChannel.send(
                    SnackbarString(
                        result.message ?: ERROR_PHONE_NUMBER_NOT_FOUND
                    )
                )
                
                is SendOTPResult.Network -> _userMessageChannel.send(
                    SnackbarString(
                        result.message ?: ERROR_NETWORK_SEND_OTP
                    )
                )
                
                is SendOTPResult.Generic -> _userMessageChannel.send(
                    SnackbarString(
                        result.message ?: ERROR_GENERIC
                    )
                )
            }
        }
        
        _verifyOTPUIState.update { it.copy(verifyOTPResult = null, otpCode = TextFieldValue("")) }
    }
    
    override fun clearVerifyOTPResult() {
        _verifyOTPUIState.update { it.copy(verifyOTPResult = null, errorMessage = null) }
    }
    
    override fun handleAuthAction(action: AuthAction) {
        viewModelScope.launch {
            _authNavigationEventChannel.send(AuthNavigationEvent.ToEnterPhoneNumberScreen(action))
        }
    }
    
    companion object {
        private const val OTP_LENGTH = 6
        
        private const val ERROR_RATE_LIMITED_SENDING_OTP = "Rate limited for sending OTP."
        private const val ERROR_RATE_LIMITED_VERIFYING_OTP = "Rate limited for verifying OTP."
        private const val ERROR_PHONE_NUMBER_REGISTERED = "This phone number is already registered."
        private const val ERROR_PHONE_NUMBER_NOT_FOUND = "This phone number is not found for login."
        private const val ERROR_NETWORK_SEND_OTP = "Network error sending OTP."
        private const val ERROR_NETWORK_VERIFY_OTP = "Network error verifying OTP."
        private const val ERROR_GENERIC = "Could not send OTP."
    }
}