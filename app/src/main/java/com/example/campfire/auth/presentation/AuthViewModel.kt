package com.example.campfire.auth.presentation

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campfire.auth.data.remote.AuthApiService
import com.example.campfire.auth.data.remote.dto.request.RegisterRequest
import com.example.campfire.auth.data.remote.dto.request.VerifyEmailRequest
import com.example.campfire.auth.data.remote.dto.request.VerifyPhoneRequest
import com.example.campfire.auth.domain.repository.LoginResult
import com.example.campfire.auth.domain.usecase.LoginUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUserUseCase: LoginUserUseCase,
    // Keep these for register/verify until they are also refactored with use cases
    private val apiService: AuthApiService,
) : ViewModel() {
    private var _email = mutableStateOf(TextFieldValue(""))
    val email: State<TextFieldValue> = _email
    
    private var _phone = mutableStateOf(TextFieldValue(""))
    val phone: State<TextFieldValue> = _phone
    
    private var _password = mutableStateOf(TextFieldValue(""))
    val password: State<TextFieldValue> = _password
    
    private var _emailCode = mutableStateOf(TextFieldValue(""))
    val emailCode: State<TextFieldValue> = _emailCode
    
    private var _phoneCode = mutableStateOf(TextFieldValue(""))
    val phoneCode: State<TextFieldValue> = _phoneCode
    
    private var _message = mutableStateOf<String?>(null)
    val message: State<String?> = _message
    
    private var _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private var _isPasswordVisible = mutableStateOf(false)
    val isPasswordVisible: State<Boolean> = _isPasswordVisible
    
    fun updateEmail(value: TextFieldValue) {
        _email.value = value
    }
    
    fun updatePhone(value: TextFieldValue) {
        _phone.value = value
    }
    
    fun updatePassword(value: TextFieldValue) {
        _password.value = value
    }
    
    fun updateEmailCode(value: TextFieldValue) {
        _emailCode.value = value
    }
    
    fun updatePhoneCode(value: TextFieldValue) {
        _phoneCode.value = value
    }
    
    @Suppress("unused")
    fun updateIsLoading(value: Boolean) {
        _isLoading.value = value
    }
    
    fun togglePasswordVisibility() {
        _isPasswordVisible.value = !_isPasswordVisible.value
    }
    
    // --- Login Function Refactored ---
    fun loginUser() {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = "Logging in..." // Initial message
            
            val result = loginUserUseCase.invoke(
                email = email.value.text,
                password = password.value.text
            )
            
            when (result) {
                is LoginResult.Success -> {
                    _message.value = "Login successful! Welcome."
                    Log.d(
                        "AuthViewModel",
                        "Login Successful. Access Token: ${result.tokens.accessToken}"
                    )
                    // TODO: Navigate to the next screen (e.g., main app screen)
                    // This navigation should be handled by observing a state/event from the Composable
                }
                
                is LoginResult.InvalidCredentialsError -> {
                    _message.value = "Incorrect email or password."
                }
                
                is LoginResult.UserInactiveError -> {
                    _message.value = "This user account is inactive."
                }
                
                is LoginResult.NetworkError -> {
                    _message.value = result.message ?: "A network error occurred."
                }
                
                is LoginResult.GenericError -> {
                    _message.value = result.message ?: "An unexpected error occurred."
                    result.code?.let {
                        Log.e(
                            "AuthViewModel",
                            "Login Generic Error - Code: $it, Message: ${result.message}"
                        )
                    }
                        ?: Log.e(
                            "AuthViewModel",
                            "Login Generic Error - Message: ${result.message}"
                        )
                }
            }
            _isLoading.value = false
        }
    }
    
    // --- Register, VerifyEmail, VerifyPhone remain similar for now ---
    // --- You would ideally create UseCases for these as well ---
    fun registerUser() {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = "Registering..."
            try {
                val request =
                    RegisterRequest(email.value.text, phone.value.text, password.value.text)
                val response = apiService.registerUser(request).execute()
                if (response.isSuccessful && response.body() != null) {
                    _message.value = response.body()?.data?.message ?: "Registration successful"
                } else {
                    _message.value = "Registration failed: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Register exception", e)
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun verifyEmail() {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = "Verifying Email..."
            try {
                val request = VerifyEmailRequest(email.value.text, emailCode.value.text)
                val response = apiService.verifyEmail(request).execute()
                if (response.isSuccessful && response.body() != null) {
                    _message.value =
                        response.body()?.data?.message ?: "Email verification successful!"
                } else {
                    _message.value = "Verify Email failed: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "VerifyEmail exception", e)
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun verifyPhone() {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = "Verifying Phone..."
            try {
                val request = VerifyPhoneRequest(phone.value.text, phoneCode.value.text)
                val response = apiService.verifyPhone(request).execute()
                if (response.isSuccessful && response.body() != null) {
                    _message.value =
                        response.body()?.data?.message ?: "Phone verification successful!"
                } else {
                    _message.value = "Verify Phone failed: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "VerifyPhone exception", e)
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    // --- End of Register, VerifyEmail, VerifyPhone ---
    
    fun clearMessage() {
        _message.value = null
    }
    
    fun resetAllInputFields() {
        _email.value = TextFieldValue("")
        _phone.value = TextFieldValue("")
        _password.value = TextFieldValue("")
        _emailCode.value = TextFieldValue("")
        _phoneCode.value = TextFieldValue("")
        clearMessage()
    }
}