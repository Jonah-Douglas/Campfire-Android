package com.example.campfire.auth.presentation

import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campfire.R
import com.example.campfire.auth.domain.repository.CompleteProfileResult
import com.example.campfire.auth.domain.repository.Field
import com.example.campfire.auth.domain.repository.SendOTPResult
import com.example.campfire.auth.domain.repository.VerifyOTPResult
import com.example.campfire.auth.domain.usecase.CompleteProfileUseCase
import com.example.campfire.auth.domain.usecase.SendOTPUseCase
import com.example.campfire.auth.domain.usecase.VerifyOTPUseCase
import com.example.campfire.auth.presentation.navigation.AuthAction
import com.example.campfire.core.domain.model.PhoneNumber
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
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

data class CompleteProfileUIState(
    val isLoading: Boolean = false,
    val completeProfileResult: CompleteProfileResult? = null,
    val fieldErrors: Map<Field, String> = emptyMap(),
    // Profile fields
    val firstName: TextFieldValue = TextFieldValue(""),
    val lastName: TextFieldValue = TextFieldValue(""),
    val email: TextFieldValue = TextFieldValue(""),
    val dateOfBirth: LocalDate? = null,
    val dateOfBirthInput: TextFieldValue = TextFieldValue(""),
    val enableNotifications: Boolean = true
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sendOTPUseCase: SendOTPUseCase,
    private val verifyOTPUseCase: VerifyOTPUseCase,
    private val completeProfileUseCase: CompleteProfileUseCase
) : ViewModel(), AuthContract {
    
    private val _sendOTPUIState = MutableStateFlow(SendOTPUIState())
    override val sendOTPUIState: StateFlow<SendOTPUIState> = _sendOTPUIState.asStateFlow()
    
    private val _verifyOTPUIState = MutableStateFlow(VerifyOTPUIState())
    override val verifyOTPUIState: StateFlow<VerifyOTPUIState> = _verifyOTPUIState.asStateFlow()
    
    private val _completeProfileUIState = MutableStateFlow(CompleteProfileUIState())
    override val completeProfileUIState: StateFlow<CompleteProfileUIState> =
        _completeProfileUIState.asStateFlow()
    
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
    
    init {
        loadAvailableCountries()
        // Set initial default country code
        val defaultRegion = Locale.getDefault().country
        
        val countries = _sendOTPUIState.value.availableCountries
        val defaultCountryIsSupported = countries.any { it.regionCode == defaultRegion }
        
        if (defaultRegion.isNotBlank() && defaultCountryIsSupported) {
            onRegionSelected(defaultRegion)
        } else {
            val fallbackRegion = countries.firstOrNull()?.regionCode ?: "US"
            onRegionSelected(fallbackRegion)
            if (defaultRegion.isNotBlank() && !defaultCountryIsSupported) {
                Log.w(
                    LOG_TAG,
                    "Device default region '$defaultRegion' is not in the configured available countries, using fallback '$fallbackRegion'."
                )
            }
        }
    }
    
    /**
     * Loads the available countries for phone number selection.
     */
    private fun loadAvailableCountries() {
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
                null
            }
        }.sortedBy { it.name }
        
        _sendOTPUIState.update { it.copy(availableCountries = countries) }
    }
    
    override fun onRegionSelected(regionCode: String) {
        val dialCode = PhoneNumber.getDialingCodeForRegion(regionCode)
        if (dialCode != 0) {
            _sendOTPUIState.update {
                it.copy(
                    selectedRegionCode = regionCode.uppercase(Locale.ROOT),
                    displayCountryDialCode = "+$dialCode",
                    validationError = null,
                    sendOTPResult = null
                )
            }
        } else {
            _sendOTPUIState.update {
                it.copy(
                    validationError = "Could not set country code for $regionCode"
                )
            }
            Log.w(LOG_TAG, "Could not get dial code for region: $regionCode")
        }
    }
    
    override fun onNationalNumberInputValueChanged(newNationalNumber: TextFieldValue) {
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
        Log.d(
            LOG_TAG,
            "setAuthOperationContext CALLED. isNewContext: $isNewContext. Countries BEFORE: ${_sendOTPUIState.value.availableCountries.size}"
        )
        currentAuthActionInternal = action
        currentFullPhoneNumberTarget = phoneNumberE164
        _phoneNumberForVerification.value = phoneNumberE164
        
        if (isNewContext) {
            val initialPhoneNumber = if (!phoneNumberE164.isNullOrBlank()) {
                PhoneNumber.fromE164(phoneNumberE164)
            } else {
                null
            }
            
            // Determine the region to select. It should ideally use the already loaded countries.
            val currentCountries =
                _sendOTPUIState.value.availableCountries
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
                regionToSelect =
                    currentCountries.first().regionCode
            } else if (currentCountries.isEmpty() && PhoneNumber.getDialingCodeForRegion(
                    regionToSelect
                ) == 0
            ) {
                regionToSelect = "US"
            }
            
            val dialCode = PhoneNumber.getDialingCodeForRegion(regionToSelect)
            
            _sendOTPUIState.update { currentState -> // Use .update
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
        } else {
            _sendOTPUIState.update { currentState ->
                currentState.copy(currentAuthAction = action)
            }
        }
        Log.d(
            LOG_TAG,
            "setAuthOperationContext END. Countries AFTER: ${_sendOTPUIState.value.availableCountries.size}"
        )
    }
    
    // --- Send OTP ---
    override fun attemptSendOTP() {
        val currentState = _sendOTPUIState.value
        val countryDialCodeNumeric = currentState.displayCountryDialCode.filter { it.isDigit() }
        val nationalNum = currentState.nationalNumberInput.text
        
        if (currentState.selectedRegionCode.isBlank() || countryDialCodeNumeric.isBlank()) {
            _sendOTPUIState.update { it.copy(validationError = "Please select a country code.") }
            return
        }
        if (nationalNum.isBlank()) {
            _sendOTPUIState.update { it.copy(validationError = "Please enter your phone number.") }
            return
        }
        
        val constructedPhoneNumber = PhoneNumber.fromCountryCodeAndNationalNumber(
            countryCodeString = countryDialCodeNumeric,
            nationalNumberString = nationalNum
        )
        
        if (!constructedPhoneNumber.isValid) {
            _sendOTPUIState.update {
                it.copy(
                    validationError = "The phone number entered is not valid.",
                    parsedPhoneNumber = constructedPhoneNumber
                )
            }
            return
        }
        
        // Number is valid according to libphonenumber, proceed to send OTP
        _sendOTPUIState.update {
            it.copy(
                isLoading = true,
                validationError = null,
                parsedPhoneNumber = constructedPhoneNumber,
                sendOTPResult = null
            )
        }
        
        val e164NumberToSend = constructedPhoneNumber.e164Format!!
        currentFullPhoneNumberTarget = e164NumberToSend
        _phoneNumberForVerification.value = e164NumberToSend
        
        viewModelScope.launch {
            val result = sendOTPUseCase(
                phoneNumber = constructedPhoneNumber.e164Format!!,
                authAction = currentAuthActionInternal
            )
            _sendOTPUIState.update { it.copy(isLoading = false, sendOTPResult = result) }
            
            if (result is SendOTPResult.Success) {
                val numberSentTo = currentFullPhoneNumberTarget
                if (numberSentTo != null) {
                    Log.d(LOG_TAG, "OTP Sent successfully to $numberSentTo, navigating.")
                    _authNavigationEventChannel.send(
                        AuthNavigationEvent.ToOTPVerifiedScreen(
                            phoneNumber = numberSentTo,
                            originatingAction = currentAuthActionInternal
                        )
                    )
                } else {
                    Log.e(LOG_TAG, "OTP Sent but currentFullPhoneNumberTarget is null.")
                }
            }
        }
    }
    
    override fun clearSendOTPResult() {
        _sendOTPUIState.update { it.copy(sendOTPResult = null) }
    }
    
    // --- Verify OTP ---
    override fun onOTPCodeChanged(newCode: TextFieldValue) {
        if (newCode.text.length <= 6 && newCode.text.all { it.isDigit() }) {
            _verifyOTPUIState.update {
                it.copy(otpCode = newCode, verifyOTPResult = null)
            }
        }
    }
    
    override fun verifyOTP() {
        val otpCode = _verifyOTPUIState.value.otpCode.text
        val phoneToVerify = currentFullPhoneNumberTarget
        
        if (phoneToVerify == null) {
            Log.e(
                "AuthViewModel",
                "verifyOTP: currentFullPhoneNumberTarget is null. Cannot proceed."
            )
            viewModelScope.launch { _userMessageChannel.send(UserMessage.Snackbar(R.string.error_phone_invalid_generic)) }
            return
        }
        
        val action = currentAuthActionInternal
        
        viewModelScope.launch {
            _verifyOTPUIState.update { it.copy(isLoading = true, verifyOTPResult = null) }
            val result =
                verifyOTPUseCase(
                    phoneNumber = phoneToVerify,
                    otpCode = otpCode,
                    authAction = action
                )
            _verifyOTPUIState.update { it.copy(isLoading = false, verifyOTPResult = result) }
            
            when (result) {
                is VerifyOTPResult.SuccessLogin -> {
                    _userMessageChannel.send(UserMessage.Toast(R.string.login_successful))
                    _authNavigationEventChannel.send(AuthNavigationEvent.ToMainApp)
                }
                
                is VerifyOTPResult.SuccessRegistration -> {
                    _userMessageChannel.send(UserMessage.Toast(R.string.otp_verified_proceed_profile))
                    _authNavigationEventChannel.send(AuthNavigationEvent.ToProfileCompletion)
                }
                
                is VerifyOTPResult.SuccessButUserExistedDuringRegistration -> {
                    _userMessageChannel.send(UserMessage.ToastString("Welcome back! Logged you in."))
                    _authNavigationEventChannel.send(AuthNavigationEvent.ToMainApp)
                }
                
                // Handle errors
                is VerifyOTPResult.OTPIncorrect -> _userMessageChannel.send(UserMessage.Snackbar(R.string.error_otp_incorrect))
                is VerifyOTPResult.OTPExpired -> _userMessageChannel.send(UserMessage.Snackbar(R.string.error_otp_expired))
                is VerifyOTPResult.RateLimited -> _userMessageChannel.send(
                    UserMessage.SnackbarString(
                        result.message ?: ERROR_RATE_LIMITED_VERIFYING_OTP
                    )
                )
                
                is VerifyOTPResult.Network -> _userMessageChannel.send(
                    UserMessage.SnackbarString(
                        result.message ?: ERROR_NETWORK_VERIFY_OTP
                    )
                )
                
                is VerifyOTPResult.Generic -> _userMessageChannel.send(
                    UserMessage.SnackbarString(
                        result.message ?: ERROR_GENERIC
                    )
                )
            }
        }
    }
    
    override fun resendOTP() {
        val phoneToResend = currentFullPhoneNumberTarget
        
        if (phoneToResend == null) {
            Log.e(LOG_TAG, "resendOTP: currentFullPhoneNumberTarget is null. Cannot proceed.")
            viewModelScope.launch { _userMessageChannel.send(UserMessage.Snackbar(R.string.error_phone_invalid_generic)) }
            return
        }
        
        val action = currentAuthActionInternal
        
        viewModelScope.launch {
            _sendOTPUIState.update { it.copy(isLoading = true, sendOTPResult = null) }
            val result = sendOTPUseCase(phoneToResend, action)
            _sendOTPUIState.update { it.copy(isLoading = false, sendOTPResult = result) }
            
            when (result) {
                is SendOTPResult.Success -> {}
                is SendOTPResult.InvalidPhoneNumber -> _userMessageChannel.send(
                    UserMessage.Snackbar(
                        R.string.error_phone_invalid_generic
                    )
                )
                
                is SendOTPResult.RateLimited -> _userMessageChannel.send(
                    UserMessage.SnackbarString(
                        result.message ?: ERROR_RATE_LIMITED_SENDING_OTP
                    )
                )
                
                is SendOTPResult.UserAlreadyExists -> _userMessageChannel.send(
                    UserMessage.SnackbarString(
                        result.message ?: ERROR_PHONE_NUMBER_REGISTERED
                    )
                )
                
                is SendOTPResult.UserNotFound -> _userMessageChannel.send(
                    UserMessage.SnackbarString(
                        result.message ?: ERROR_PHONE_NUMBER_NOT_FOUND
                    )
                )
                
                is SendOTPResult.Network -> _userMessageChannel.send(
                    UserMessage.SnackbarString(
                        result.message ?: ERROR_NETWORK_SEND_OTP
                    )
                )
                
                is SendOTPResult.Generic -> _userMessageChannel.send(
                    UserMessage.SnackbarString(
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
    
    // --- Complete Profile---
    override fun onFirstNameChanged(newName: TextFieldValue) {
        _completeProfileUIState.update {
            it.copy(firstName = newName, fieldErrors = it.fieldErrors - Field.FIRST_NAME)
        }
    }
    
    override fun onLastNameChanged(newName: TextFieldValue) {
        _completeProfileUIState.update {
            it.copy(lastName = newName, fieldErrors = it.fieldErrors - Field.LAST_NAME)
        }
    }
    
    override fun onEmailChanged(newEmail: TextFieldValue) {
        _completeProfileUIState.update {
            it.copy(email = newEmail, fieldErrors = it.fieldErrors - Field.EMAIL)
        }
    }
    
    override fun onDateOfBirthInputChanged(newDobInput: TextFieldValue) {
        _completeProfileUIState.update {
            val dob = try {
                LocalDate.parse(newDobInput.text, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: DateTimeParseException) {
                null
            }
            it.copy(
                dateOfBirthInput = newDobInput,
                dateOfBirth = dob,
                fieldErrors = it.fieldErrors - Field.DATE_OF_BIRTH
            )
        }
    }
    
    override fun onEnableNotificationsChanged(enabled: Boolean) {
        _completeProfileUIState.update { it.copy(enableNotifications = enabled) }
    }
    
    override fun completeUserProfile() {
        val uiState = _completeProfileUIState.value
        // Basic validation, enhance as needed
        val fieldErrors = mutableMapOf<Field, String>()
        if (uiState.firstName.text.isBlank()) fieldErrors[Field.FIRST_NAME] =
            ERROR_FIRST_NAME_MISSING
        if (uiState.lastName.text.isBlank()) fieldErrors[Field.LAST_NAME] =
            ERROR_LAST_NAME_MISSING
        if (uiState.email.text.isBlank()) fieldErrors[Field.EMAIL] =
            ERROR_EMAIL_MISSING
        if (uiState.dateOfBirth == null && uiState.dateOfBirthInput.text.isNotBlank()) { // If input exists but couldn't parse
            fieldErrors[Field.DATE_OF_BIRTH] = ERROR_DOB_INVALID
        } else if (uiState.dateOfBirth == null) {
            fieldErrors[Field.DATE_OF_BIRTH] = ERROR_DOB_MISSING
        }
        
        
        if (fieldErrors.isNotEmpty()) {
            _completeProfileUIState.update { it.copy(fieldErrors = fieldErrors) }
            viewModelScope.launch { _userMessageChannel.send(UserMessage.Snackbar(R.string.error_validation_check_fields)) }
            return
        }
        
        viewModelScope.launch {
            _completeProfileUIState.update {
                it.copy(
                    isLoading = true,
                    completeProfileResult = null,
                    fieldErrors = emptyMap()
                )
            }
            val result = completeProfileUseCase(
                firstName = uiState.firstName.text.trim(),
                lastName = uiState.lastName.text.trim(),
                email = uiState.email.text.trim(),
                dateOfBirth = uiState.dateOfBirth,
                enableNotifications = uiState.enableNotifications
            )
            _completeProfileUIState.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    completeProfileResult = result,
                    fieldErrors = (result as? CompleteProfileResult.Validation)?.errors
                        ?: emptyMap()
                )
            }
            
            when (result) {
                is CompleteProfileResult.Success -> {
                    _userMessageChannel.send(UserMessage.Toast(R.string.profile_completed_success))
                    _authNavigationEventChannel.send(AuthNavigationEvent.ToMainApp)
                }
                
                is CompleteProfileResult.Validation -> {
                    _userMessageChannel.send(UserMessage.Snackbar(R.string.error_validation_check_fields))
                }
                
                is CompleteProfileResult.EmailAlreadyExists -> _userMessageChannel.send(
                    UserMessage.Snackbar(
                        R.string.error_email_already_exists
                    )
                )
                
                is CompleteProfileResult.Network -> _userMessageChannel.send(
                    UserMessage.SnackbarString(
                        result.message ?: ERROR_NETWORK_COMPLETE_PROFILE
                    )
                )
                
                is CompleteProfileResult.Generic -> _userMessageChannel.send(
                    UserMessage.SnackbarString(
                        result.message ?: ERROR_COMPLETE_PROFILE_GENERIC
                    )
                )
            }
        }
    }
    
    override fun clearCompleteProfileResult() {
        _completeProfileUIState.update {
            it.copy(
                completeProfileResult = null,
                fieldErrors = emptyMap()
            )
        }
    }
    
    companion object {
        private const val LOG_TAG =
            "AuthViewModel"
        private const val ERROR_RATE_LIMITED_SENDING_OTP =
            "Rate limited for sending OTP."
        private const val ERROR_RATE_LIMITED_VERIFYING_OTP =
            "Rate limited for verifying OTP."
        private const val ERROR_PHONE_NUMBER_REGISTERED =
            "This phone number is already registered."
        private const val ERROR_PHONE_NUMBER_NOT_FOUND =
            "This phone number is not found for login."
        private const val ERROR_NETWORK_SEND_OTP =
            "Network error sending OTP."
        private const val ERROR_NETWORK_VERIFY_OTP =
            "Network error verifying OTP."
        private const val ERROR_NETWORK_COMPLETE_PROFILE =
            "Network error completing profile."
        private const val ERROR_GENERIC =
            "Could not send OTP."
        private const val ERROR_FIRST_NAME_MISSING =
            "First name required."
        private const val ERROR_LAST_NAME_MISSING =
            "Last name required."
        private const val ERROR_EMAIL_MISSING =
            "Email required."
        private const val ERROR_DOB_MISSING =
            "Date of birth required."
        private const val ERROR_DOB_INVALID =
            "Date of birth is invalid."
        private const val ERROR_COMPLETE_PROFILE_GENERIC =
            "Could not complete profile."
    }
}