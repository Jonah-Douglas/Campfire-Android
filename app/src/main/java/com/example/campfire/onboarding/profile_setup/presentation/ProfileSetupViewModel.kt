package com.example.campfire.onboarding.profile_setup.presentation

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campfire.R
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.presentation.UserMessage
import com.example.campfire.onboarding.profile_setup.domain.model.CompleteProfileSetupResult
import com.example.campfire.onboarding.profile_setup.domain.model.ProfileSetupField
import com.example.campfire.onboarding.profile_setup.domain.usecase.CompleteProfileSetupUseCase
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
import javax.inject.Inject


data class CompleteProfileUIState(
    val isLoading: Boolean = false,
    val completeProfileSetupResult: CompleteProfileSetupResult? = null,
    val fieldErrors: Map<ProfileSetupField, String> = emptyMap(),
    // Profile fields
    val firstName: TextFieldValue = TextFieldValue(""),
    val lastName: TextFieldValue = TextFieldValue(""),
    val email: TextFieldValue = TextFieldValue(""),
    val dateOfBirth: LocalDate? = null,
    val dateOfBirthInput: TextFieldValue = TextFieldValue(""),
    val enableNotifications: Boolean = true
)

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val completeProfileSetupUseCase: CompleteProfileSetupUseCase,
) : ViewModel(), ProfileSetupContract {
    
    private val _onboardingUIState = MutableStateFlow(CompleteProfileUIState())
    override val onboardingUIState: StateFlow<CompleteProfileUIState> =
        _onboardingUIState.asStateFlow()
    
    private val _userMessageChannel =
        Channel<UserMessage>(Channel.BUFFERED)
    override val userMessages: Flow<UserMessage> =
        _userMessageChannel.receiveAsFlow()
    
    private val _profileSetupNavigationEventChannel =
        Channel<ProfileSetupNavigationEvent>(Channel.BUFFERED)
    override val profileSetupNavigationEvents: Flow<ProfileSetupNavigationEvent> =
        _profileSetupNavigationEventChannel.receiveAsFlow()
    
    init {
        Firelog.i("OnboardingViewModel initialized.")
        // Example: If a partial User object or ID is passed via navigation
        // val userId: String? = savedStateHandle[USER_ID_ARG]
        // if (userId != null) {
        //     Firelog.d("User ID received: $userId. Fetching initial data if necessary.")
        //     // Potentially load initial data if the User object from auth isn't complete
        //     // and needs to be fetched or if you only passed an ID.
        // }
        // For example, if you passed the phone number and need to prefill something,
        // you could retrieve it from savedStateHandle.
    }
    
    
    override fun onFirstNameChanged(newName: TextFieldValue) {
        Firelog.v("onFirstNameChanged: ${newName.text}")
        _onboardingUIState.update {
            it.copy(
                firstName = newName,
                fieldErrors = it.fieldErrors - ProfileSetupField.FIRST_NAME
            )
        }
    }
    
    override fun onLastNameChanged(newName: TextFieldValue) {
        Firelog.v("onLastNameChanged: ${newName.text}")
        _onboardingUIState.update {
            it.copy(
                lastName = newName,
                fieldErrors = it.fieldErrors - ProfileSetupField.LAST_NAME
            )
        }
    }
    
    override fun onEmailChanged(newEmail: TextFieldValue) {
        Firelog.v("onEmailChanged: ${newEmail.text}")
        _onboardingUIState.update {
            it.copy(
                email = newEmail,
                fieldErrors = it.fieldErrors - ProfileSetupField.EMAIL
            )
        }
    }
    
    override fun onDateOfBirthInputChanged(newDobInput: TextFieldValue) {
        Firelog.v("onDateOfBirthInputChanged: ${newDobInput.text}")
        _onboardingUIState.update {
            val dob = try {
                // Ensure your date format is what you expect from the input
                // Consider using a DatePicker for better UX and less parsing error.
                if (newDobInput.text.isNotBlank()) LocalDate.parse(
                    newDobInput.text,
                    DateTimeFormatter.ISO_LOCAL_DATE
                ) else null
            } catch (e: DateTimeParseException) {
                Firelog.w("Invalid DOB format: ${newDobInput.text}", e)
                null // Will trigger validation error if input is not blank
            }
            it.copy(
                dateOfBirthInput = newDobInput,
                dateOfBirth = dob,
                fieldErrors = it.fieldErrors - ProfileSetupField.DATE_OF_BIRTH
            )
        }
    }
    
    override fun onEnableNotificationsChanged(enabled: Boolean) {
        Firelog.d("onEnableNotificationsChanged: $enabled")
        _onboardingUIState.update { it.copy(enableNotifications = enabled) }
    }
    
    override fun completeUserOnboarding() {
        val uiState = _onboardingUIState.value
        Firelog.i("completeUserOnboarding called. FirstName: ${uiState.firstName.text.isNotBlank()}, Email: ${uiState.email.text.isNotBlank()}, DOB: ${uiState.dateOfBirth != null}")
        
        val fieldErrors = mutableMapOf<ProfileSetupField, String>()
        if (uiState.firstName.text.isBlank()) fieldErrors[ProfileSetupField.FIRST_NAME] =
            ERROR_FIRST_NAME_MISSING
        if (uiState.lastName.text.isBlank()) fieldErrors[ProfileSetupField.LAST_NAME] =
            ERROR_LAST_NAME_MISSING
        if (uiState.email.text.isBlank()) fieldErrors[ProfileSetupField.EMAIL] =
            ERROR_EMAIL_MISSING // Add email validation (format)
        
        // Validate DOB: it must be parseable if input is not blank, or it must not be blank if required.
        if (uiState.dateOfBirthInput.text.isNotBlank() && uiState.dateOfBirth == null) {
            fieldErrors[ProfileSetupField.DATE_OF_BIRTH] = ERROR_DOB_INVALID
        } else if (uiState.dateOfBirthInput.text.isBlank()) { // Or check uiState.dateOfBirth == null directly if DOB is mandatory
            fieldErrors[ProfileSetupField.DATE_OF_BIRTH] = ERROR_DOB_MISSING
        }
        // Add more specific validations (e.g., email format, valid date ranges)
        
        if (fieldErrors.isNotEmpty()) {
            Firelog.w("Validation errors found: $fieldErrors")
            _onboardingUIState.update { it.copy(fieldErrors = fieldErrors) }
            viewModelScope.launch {
                _userMessageChannel.send(
                    UserMessage.Snackbar(
                        R.string.error_validation_check_fields
                    )
                )
            }
            return
        }
        
        // Assuming a user ID would be available if this is an update to an existing skeleton user
        // If it's creating a new user record post-auth, the backend handles ID generation.
        // For this example, let's assume the use case handles knowing the target user (e.g., from auth context).
        viewModelScope.launch {
            Firelog.d("Attempting to complete onboarding...")
            _onboardingUIState.update {
                it.copy(
                    isLoading = true,
                    completeProfileSetupResult = null,
                    fieldErrors = emptyMap()
                )
            }
            
            // The user ID might need to be retrieved from an auth manager/repository
            // if not directly passed or available from initialUser.
            // For now, let's assume the use case can derive it or doesn't need it explicitly passed here.
            val result = completeProfileSetupUseCase(
                // userId = currentUserId, // This might be needed by your use case
                firstName = uiState.firstName.text.trim(),
                lastName = uiState.lastName.text.trim(),
                email = uiState.email.text.trim(),
                dateOfBirth = uiState.dateOfBirth!!, // Non-null due to prior validation
                enableNotifications = uiState.enableNotifications
            )
            Firelog.i("Complete onboarding result: ${result::class.simpleName}")
            
            _onboardingUIState.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    completeProfileSetupResult = result,
                    fieldErrors = (result as? CompleteProfileSetupResult.Validation)?.errors
                        ?: emptyMap()
                )
            }
            
            when (result) {
                is CompleteProfileSetupResult.Success -> {
                    Firelog.i("Onboarding successful for user: ${result.user.id}")
                    _userMessageChannel.send(
                        UserMessage.Toast(
                            R.string.profile_completed_success
                        )
                    ) // Re-use or create new string
                    _profileSetupNavigationEventChannel.send(ProfileSetupNavigationEvent.ToFeedsScreen)
                }
                
                is CompleteProfileSetupResult.Validation -> {
                    Firelog.w("Onboarding validation errors from use case: ${result.errors}")
                    _userMessageChannel.send(
                        UserMessage.Snackbar(
                            R.string.error_validation_check_fields
                        )
                    )
                }
                
                is CompleteProfileSetupResult.EmailAlreadyExists -> {
                    Firelog.w("Email already exists: ${uiState.email.text}")
                    _userMessageChannel.send(
                        UserMessage.Snackbar(
                            R.string.error_email_already_exists
                        )
                    ) // Re-use or new string
                }
                
                is CompleteProfileSetupResult.Network -> {
                    Firelog.e("Network error during onboarding: ${result.message}")
                    _userMessageChannel.send(
                        UserMessage.SnackbarString(
                            result.message ?: ERROR_NETWORK_ONBOARDING
                        )
                    )
                }
                
                is CompleteProfileSetupResult.Generic -> {
                    Firelog.e("Generic error during onboarding: ${result.message}")
                    _userMessageChannel.send(
                        UserMessage.SnackbarString(
                            result.message ?: ERROR_ONBOARDING_GENERIC
                        )
                    )
                }
            }
        }
    }
    
    override fun clearCompleteOnboardingResult() { // Renamed to match contract
        Firelog.d("clearCompleteOnboardingResult called.")
        _onboardingUIState.update {
            it.copy(
                completeProfileSetupResult = null,
                fieldErrors = emptyMap() // Also clear field errors on explicit clear
            )
        }
    }
    
    companion object {
        // Error messages specific to onboarding, or re-use/move R.string constants
        private const val ERROR_NETWORK_ONBOARDING = "Network error during setup."
        private const val ERROR_FIRST_NAME_MISSING = "First name required."
        private const val ERROR_LAST_NAME_MISSING = "Last name required."
        private const val ERROR_EMAIL_MISSING = "Email required."
        private const val ERROR_DOB_MISSING = "Date of birth required."
        private const val ERROR_DOB_INVALID = "Date of birth is invalid."
        private const val ERROR_ONBOARDING_GENERIC = "Could not complete setup."
    }
}
