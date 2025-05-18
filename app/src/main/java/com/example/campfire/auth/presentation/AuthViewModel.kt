package com.example.campfire.auth.presentation

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campfire.auth.data.remote.AuthApiService
import com.example.campfire.auth.data.remote.dto.request.LoginRequest
import com.example.campfire.auth.data.remote.dto.request.RegisterRequest
import com.example.campfire.auth.data.remote.dto.request.VerifyEmailRequest
import com.example.campfire.auth.data.remote.dto.request.VerifyPhoneRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel // ViewModel for handling authentication logic
class AuthViewModel @Inject constructor(
    private val apiService: AuthApiService
) : ViewModel() {
    //use saved states.
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
    
    private var _message = mutableStateOf("")
    val message: State<String> = _message
    
    private var _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private var _isPasswordVisible = mutableStateOf(false)
    val isPasswordVisible = _isPasswordVisible
    
    //Functions to update the state
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
    
    fun updateMessage(value: String) {
        _message.value = value
    }
    
    @Suppress("unused")
    fun updateIsLoading(value: Boolean) {
        _isLoading.value = value
    }
    
    fun togglePasswordVisibility() {
        _isPasswordVisible.value = !_isPasswordVisible.value
    }
    
    // Function to handle user registration
    fun registerUser(context: Context, coroutineScope: CoroutineScope) {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = "Registering..."
            
            try {
                // JD TODO: Hardcode these for now I think, populate them correctly later
                val request = RegisterRequest(
                    email.value.text,
                    phone.value.text,
                    password.value.text
                )
                val response = apiService.registerUser(request).execute()
                
                if (response.isSuccessful && response.body() != null) {
                    _message.value = response.body()?.data?.message ?: "Registration successful"
                    // Consider navigating to email verification screen here
                } else {
                    _message.value = "Registration failed: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Function to handle email verification
    fun verifyEmail(context: Context, coroutineScope: CoroutineScope) {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = "Verifying Email..."
            
            try {
                // JD TODO: Hardcode these for now I think, populate them correctly later
                val request = VerifyEmailRequest(
                    email.value.text,
                    emailCode.value.text
                )
                val response = apiService.verifyEmail(request).execute()
                
                if (response.isSuccessful && response.body() != null) {
                    _message.value =
                        response.body()?.data?.message ?: "Email verification successful!"
                    // Consider navigating to phone verification here
                } else {
                    _message.value = "Verify Email failed: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Function to handle phone verification
    fun verifyPhone(context: Context, coroutineScope: CoroutineScope) {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = "Verifying Phone..."
            
            try {
                // JD TODO: Hardcode these for now I think, populate them correctly later
                val request = VerifyPhoneRequest(
                    phone.value.text,
                    phoneCode.value.text
                )
                val response = apiService.verifyPhone(request).execute()
                
                if (response.isSuccessful && response.body() != null) {
                    _message.value =
                        response.body()?.data?.message ?: "Phone verification successful!"
                    // Consider navigating to login or main app screen
                } else {
                    _message.value = "Verify Phone failed: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loginUser(context: Context, coroutineScope: CoroutineScope) {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = "Logging in..."
            
            try {
                // JD TODO: Hardcode these for now I think, populate them correctly later
                val request = LoginRequest(
                    email.value.text,
                    password.value.text
                )
                val response = apiService.loginUser(request).execute()
                
                if (response.isSuccessful && response.body() != null) {
                    _message.value = response.body()?.data?.message
                        ?: "Login successful! Token: ${response.body()?.data?.token}."
                    //  Store the token and navigate to main part of the app.
                    // For demonstration, we'll just display it.
                } else {
                    _message.value = "Login failed"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun resetInputFields() {
        _email.value = TextFieldValue("")
        _phone.value = TextFieldValue("")
        _password.value = TextFieldValue("")
        _emailCode.value = TextFieldValue("")
        _phoneCode.value = TextFieldValue("")
        _message.value = ""
    }
}