package com.example.campfire.auth.presentation

// TODO: Create and import UseCases for VerifyEmail, VerifyPhone
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campfire.auth.data.remote.AuthApiService
import com.example.campfire.auth.data.remote.dto.request.VerifyEmailRequest
import com.example.campfire.auth.data.remote.dto.request.VerifyPhoneRequest
import com.example.campfire.auth.domain.repository.LoginResult
import com.example.campfire.auth.domain.repository.RegisterResult
import com.example.campfire.auth.domain.usecase.LoginUserUseCase
import com.example.campfire.auth.domain.usecase.RegisterUserUseCase
import com.example.campfire.auth.presentation.AuthViewModel.RegistrationField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class RegistrationUIState(
    val isLoading: Boolean = false,
    val registrationResult: RegisterResult? = null,
    val errorMessage: String? = null, // For general errors not tied to a specific result
    val fieldErrors: Map<RegistrationField, String?> = emptyMap()
)

data class LoginUIState(
    val isLoading: Boolean = false,
    val loginResult: LoginResult? = null,
    val errorMessage: String? = null // For dialog/general errors
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUserUseCase: LoginUserUseCase,
    private val apiService: AuthApiService, // TODO: Replace direct ApiService usage with UseCases
    // TODO: Inject VerifyEmailUseCase, VerifyPhoneUseCase
    private val registerUserUseCase: RegisterUserUseCase
) : ViewModel() {
    
    // --- UI State ---
    private val _email = mutableStateOf(TextFieldValue(""))
    val email: State<TextFieldValue> = _email
    
    private val _phone = mutableStateOf(TextFieldValue(""))
    val phone: State<TextFieldValue> = _phone
    
    private val _password = mutableStateOf(TextFieldValue(""))
    val password: State<TextFieldValue> = _password
    
    private val _confirmPassword = mutableStateOf(TextFieldValue(""))
    val confirmPassword: State<TextFieldValue> = _confirmPassword
    
    private val _emailCode = mutableStateOf(TextFieldValue(""))
    val emailCode: State<TextFieldValue> = _emailCode
    
    private val _phoneCode = mutableStateOf(TextFieldValue(""))
    val phoneCode: State<TextFieldValue> = _phoneCode
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    private val _registrationUIState = MutableStateFlow(RegistrationUIState())
    val registrationUIState: StateFlow<RegistrationUIState> = _registrationUIState.asStateFlow()
    
    private val _loginUIState = MutableStateFlow(LoginUIState())
    val loginUIState: StateFlow<LoginUIState> = _loginUIState.asStateFlow()
    
    private val _registrationFieldErrors =
        mutableStateOf<Map<RegistrationField, String?>>(emptyMap())
    val registrationFieldErrors: State<Map<RegistrationField, String?>> = _registrationFieldErrors
    
    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isPasswordVisible = mutableStateOf(false)
    val isPasswordVisible: State<Boolean> = _isPasswordVisible
    
    enum class RegistrationField {
        EMAIL, PHONE, PASSWORD, CONFIRM_PASSWORD, GENERAL
    }
    
    // --- Update Functions for UI State ---
    fun updateEmail(value: TextFieldValue) {
        _email.value = value
        clearFieldError(RegistrationField.EMAIL)
    }
    
    fun updatePhone(value: TextFieldValue) {
        _phone.value = value
        clearFieldError(RegistrationField.PHONE)
    }
    
    fun updatePassword(value: TextFieldValue) {
        _password.value = value
        clearFieldError(RegistrationField.PASSWORD)
        if (_confirmPassword.value.text.isNotEmpty()) {
            clearFieldError(RegistrationField.CONFIRM_PASSWORD)
        }
    }
    
    fun updateConfirmPassword(value: TextFieldValue) {
        _confirmPassword.value = value
        clearFieldError(RegistrationField.CONFIRM_PASSWORD)
    }
    
    fun updateEmailCode(value: TextFieldValue) {
        _emailCode.value = value
    }
    
    fun updatePhoneCode(value: TextFieldValue) {
        _phoneCode.value = value
    }
    
    fun togglePasswordVisibility() {
        _isPasswordVisible.value = !_isPasswordVisible.value
    }
    
    
    // --- Action Methods ---
    
    fun loginUser() {
        if (email.value.text.isBlank() || password.value.text.isBlank()) {
            _message.value = "Email and password cannot be empty."
            return // Exit if basic validation fails
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = null
            
            Log.d("AuthViewModel", "Attempting login for: ${email.value.text}")
            val result = loginUserUseCase.invoke(
                email = email.value.text.trim(),
                password = password.value.text
            )
            
            when (result) {
                is LoginResult.Success -> {
                    Log.d(
                        "AuthViewModel",
                        "Login Successful. Access Token: ${result.tokens.accessToken}"
                    )
                    // Message can be null or a success message if desired
                    _message.value = "Login successful!" // Optional success message
                }
                
                is LoginResult.InvalidCredentialsError -> {
                    _message.value = "Incorrect email or password."
                    Log.w(
                        "AuthViewModel",
                        "Login Failed: Invalid Credentials for ${email.value.text}"
                    )
                }
                
                is LoginResult.UserInactiveError -> {
                    _message.value = "This user account is inactive."
                    Log.w("AuthViewModel", "Login Failed: User Inactive for ${email.value.text}")
                }
                
                is LoginResult.NetworkError -> {
                    _message.value = result.message ?: "A network error occurred."
                    Log.e("AuthViewModel", "Login Network Error: ${result.message}")
                }
                
                is LoginResult.GenericError -> {
                    _message.value = result.message ?: "An unexpected error occurred."
                    Log.e(
                        "AuthViewModel",
                        "Login Generic Error - Message: ${result.message}"
                    )
                }
            }
            
            _isLoading.value = false
        }
    }
    
    fun registerUser() {
        if (!validateRegistrationInput()) {
            _registrationUIState.update {
                it.copy(
                    isLoading = false,
                )
            }
            return
        }
        
        _registrationUIState.update {
            it.copy(
                isLoading = true,
                registrationResult = null,
                errorMessage = null,
                fieldErrors = emptyMap()
            )
        }
        
        _registrationFieldErrors.value = emptyMap()
        _message.value = null
        
        viewModelScope.launch {
            Log.d("AuthViewModel", "Attempting registration via UseCase for: ${email.value.text}")
            
            // Use the RegisterUserUseCase
            val result = registerUserUseCase.invoke(
                email = email.value.text.trim(),
                phone = phone.value.text.trim(),
                password = password.value.text
            )
            
            _registrationUIState.update { currentState ->
                when (result) {
                    is RegisterResult.Success -> {
                        _message.value = result.message
                        currentState.copy(
                            isLoading = false,
                            registrationResult = result,
                            fieldErrors = emptyMap() // Ensure errors are cleared
                        )
                    }
                    
                    is RegisterResult.EmailAlreadyExistsError -> {
                        _registrationFieldErrors.value = mapOf(
                            RegistrationField.EMAIL to (result.message ?: "Email already exists.")
                        )
                        _message.value = "Registration failed."
                        currentState.copy(
                            isLoading = false,
                            registrationResult = result,
                            fieldErrors = emptyMap() // Ensure errors are cleared
                        )
                    }
                    
                    is RegisterResult.WeakPasswordError -> {
                        _registrationFieldErrors.value = mapOf(
                            RegistrationField.PASSWORD to (result.message
                                ?: "Password is too weak.")
                        )
                        _message.value = "Registration failed."
                        currentState.copy(
                            isLoading = false,
                            registrationResult = result,
                            fieldErrors = emptyMap() // Ensure errors are cleared
                        )
                    }
                    
                    is RegisterResult.NetworkError, is RegisterResult.GenericError -> {
                        // JD TODO: Do I want this message for a network error?
                        // _message.value = result.message ?: "An error occurred."
                        currentState.copy(
                            isLoading = false,
                            registrationResult = result, // Store the result
                            // If it's a GenericError that implies a field issue (though unlikely for GENERIC):
                            // fieldErrors = if (result is RegisterResult.GenericError) mapOf(RegistrationField.GENERAL to (result.message ?: "Error")) else currentState.fieldErrors
                            //errorMessage = result.message ?: "An error occurred." // More prominent general error
                        )
                    }
                    // JD TODO: Handle any other specific RegisterResult types I create
                }
            }
            
            _isLoading.value = false
        }
    }
    
    private fun validateRegistrationInput(): Boolean {
        val errors = mutableMapOf<RegistrationField, String?>()
        
        // Email Validation
        if (email.value.text.isBlank()) {
            errors[RegistrationField.EMAIL] = "Email cannot be empty."
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.value.text.trim())
                .matches()
        ) {
            errors[RegistrationField.EMAIL] = "Invalid email format."
        }
        
        // Phone Validation
        if (phone.value.text.isBlank()) {
            errors[RegistrationField.PHONE] = "Phone number cannot be empty."
        } else if (phone.value.text.trim().length < 7) { // Example: Basic length check
            errors[RegistrationField.PHONE] = "Invalid phone number format (too short)."
        }
        // TODO: Add more robust phone validation (e.g., using libphonenumber)
        
        // Password Validation
        if (password.value.text.isBlank()) {
            errors[RegistrationField.PASSWORD] = "Password cannot be empty."
        } else if (password.value.text.length < 8) {
            errors[RegistrationField.PASSWORD] = "Password must be at least 8 characters."
        }
        // TODO: Add other password strength rules (e.g., uppercase, number, special character)
        
        // Confirm Password Validation
        if (confirmPassword.value.text.isBlank()) {
            errors[RegistrationField.CONFIRM_PASSWORD] = "Please confirm your password."
        } else if (password.value.text.isNotBlank() && password.value.text != confirmPassword.value.text) {
            errors[RegistrationField.CONFIRM_PASSWORD] = "Passwords do not match."
        }
        
        return if (errors.any { it.value != null }) {
            _registrationFieldErrors.value = errors
            _message.value =
                "Please correct the errors above." // General prompt if there are field errors
            false
        } else {
            _registrationFieldErrors.value = emptyMap()
            _message.value = null // Clear general prompt if all valid
            true
        }
    }
    
    
    fun verifyEmail(onResult: (success: Boolean, requiresPhoneVerification: Boolean) -> Unit) {
        // TODO: Ideally, this should use a VerifyEmailUseCase
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = null
            var wasSuccessful = false
            var needsPhoneVerification = false // Determine this based on API or logic
            
            Log.d(
                "AuthViewModel",
                "Attempting email verification for: ${email.value.text} with code: ${emailCode.value.text}"
            )
            try {
                // TODO: Replace with VerifyEmailUseCase
                val request =
                    VerifyEmailRequest(email.value.text.trim(), emailCode.value.text.trim())
                val response = apiService.verifyEmail(request).execute()
                
                if (response.isSuccessful && response.body() != null) {
                    _message.value =
                        response.body()?.data?.message ?: "Email verification successful!"
                    wasSuccessful = true
                    // JD TODO: Implement with email verification
                    //needsPhoneVerification = response.body()?.data?.requiresPhoneVerification ?: false
                    Log.i("AuthViewModel", "Email Verification Successful for ${email.value.text}")
                } else {
                    val errorBody = response.errorBody()?.string()
                    // TODO: Map errorBody to specific error messages for the code field if possible
                    _message.value = "Verify Email failed: ${errorBody ?: "Unknown error"}"
                    Log.w(
                        "AuthViewModel",
                        "Email Verification Failed: ${response.code()} - $errorBody"
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "VerifyEmail exception", e)
                _message.value = "Error during email verification: ${e.message}"
            } finally {
                _isLoading.value = false
                onResult(wasSuccessful, needsPhoneVerification)
            }
        }
    }
    
    fun verifyPhone(onResult: (success: Boolean) -> Unit) {
        // TODO: Ideally, this should use a VerifyPhoneUseCase
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = null
            var wasSuccessful = false
            
            Log.d(
                "AuthViewModel",
                "Attempting phone verification for: ${phone.value.text} with code: ${phoneCode.value.text}"
            )
            try {
                // TODO: Replace with VerifyPhoneUseCase
                val request =
                    VerifyPhoneRequest(phone.value.text.trim(), phoneCode.value.text.trim())
                val response = apiService.verifyPhone(request).execute()
                
                if (response.isSuccessful && response.body() != null) {
                    _message.value =
                        response.body()?.data?.message ?: "Phone verification successful!"
                    wasSuccessful = true
                    Log.i("AuthViewModel", "Phone Verification Successful for ${phone.value.text}")
                } else {
                    val errorBody = response.errorBody()?.string()
                    // TODO: Map errorBody to specific error messages for the code field if possible
                    _message.value = "Verify Phone failed: ${errorBody ?: "Unknown error"}"
                    Log.w(
                        "AuthViewModel",
                        "Phone Verification Failed: ${response.code()} - $errorBody"
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "VerifyPhone exception", e)
                _message.value = "Error during phone verification: ${e.message}"
            } finally {
                _isLoading.value = false
                onResult(wasSuccessful)
            }
        }
    }
    
    // --- Helper Functions ---
    private fun clearFieldError(field: RegistrationField) {
        if (_registrationFieldErrors.value.containsKey(field)) {
            _registrationFieldErrors.value = _registrationFieldErrors.value.toMutableMap().apply {
                remove(field)
            }
        }
        
        // If all field errors are cleared, also clear the general "Please correct..." message
        if (_registrationFieldErrors.value.values.all { it == null }) {
            if (_message.value == "Please correct the errors above.") {
                _message.value = null
            }
        }
    }
    
    fun clearMessage() {
        _message.value = null
    }
    
    fun clearRegistrationState() {
        _registrationFieldErrors.value = emptyMap()
        _confirmPassword.value = TextFieldValue("")
    }
    
    fun resetAllInputFieldsAndErrors() {
        _email.value = TextFieldValue("")
        _phone.value = TextFieldValue("")
        _password.value = TextFieldValue("")
        _confirmPassword.value = TextFieldValue("")
        _emailCode.value = TextFieldValue("")
        _phoneCode.value = TextFieldValue("")
        _isPasswordVisible.value = false
        _registrationFieldErrors.value = emptyMap()
        _message.value = null
    }
    
    @Suppress("unused")
    fun updateIsLoading(value: Boolean) {
        _isLoading.value = value
    }
    
    fun clearRegistrationResult() {
        _registrationUIState.update { it.copy(registrationResult = null, errorMessage = null) }
    }
    
    fun clearLoginResult() {
        _loginUIState.update { it.copy(loginResult = null, errorMessage = null) }
    }
    
    fun clearErrorMessage() {
        _registrationUIState.update { it.copy(errorMessage = null) }
        _loginUIState.update { it.copy(errorMessage = null) }
    }
}