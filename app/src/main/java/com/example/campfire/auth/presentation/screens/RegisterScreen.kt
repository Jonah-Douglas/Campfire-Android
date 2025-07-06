package com.example.campfire.auth.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campfire.R
import com.example.campfire.auth.domain.repository.RegisterResult
import com.example.campfire.auth.presentation.AuthViewModel
import com.example.campfire.auth.presentation.AuthViewModel.RegistrationField


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegistrationSuccess: (requiresEmailVerification: Boolean) -> Unit,
    onNavigateBackToEntry: () -> Unit
) {
    BackHandler(enabled = true) { }
    
    val viewModel: AuthViewModel = hiltViewModel()
    
    // States from your ViewModel
    val emailState by viewModel.email
    val phoneState by viewModel.phone
    val passwordState by viewModel.password
    val confirmPasswordState by viewModel.confirmPassword
    val isPasswordVisible by viewModel.isPasswordVisible
    val registrationFieldErrors by viewModel.registrationFieldErrors
    
    // State from the new registrationUIState approach
    val registrationUIState by viewModel.registrationUIState.collectAsState()
    val isLoading = registrationUIState.isLoading // Use isLoading from the UI state
    
    val focusManager = LocalFocusManager.current
    
    // --- Message Dialog Logic (for more disruptive errors/messages) ---
    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("Message") }
    var dialogMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(registrationUIState.registrationResult, registrationUIState.errorMessage) {
        val result = registrationUIState.registrationResult
        val errorMsg = registrationUIState.errorMessage
        
        if (result != null) {
            when (result) {
                is RegisterResult.Success -> {} // Success is handled by onRegistrationSuccess callback.
                
                is RegisterResult.EmailAlreadyExistsError -> {
                    dialogTitle = "Registration Failed"
                    dialogMessage = result.message ?: "Email already exists."
                    showDialog = true
                }
                
                is RegisterResult.WeakPasswordError -> {
                    dialogTitle = "Registration Failed"
                    dialogMessage = result.message ?: "Password is too weak."
                    showDialog = true
                }
                
                is RegisterResult.NetworkError -> {
                    dialogTitle = "Network Error"
                    dialogMessage = result.message ?: "A network error occurred."
                    showDialog = true
                }
                
                is RegisterResult.GenericError -> {
                    dialogTitle = "Error"
                    dialogMessage = result.message ?: "An unexpected error occurred."
                    showDialog = true
                }
            }
            viewModel.clearRegistrationResult()
        } else if (errorMsg != null) {
            dialogTitle = "Error"
            dialogMessage = errorMsg
            showDialog = true
            viewModel.clearErrorMessage()
        }
    }
    
    if (showDialog && dialogMessage != null) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                dialogMessage = null
            },
            title = { Text(dialogTitle) },
            text = { Text(dialogMessage!!) },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    dialogMessage = null
                }) { Text("OK") }
            }
        )
    }
    // --- End Message Dialog Logic ---
    
    // Clear errors when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearRegistrationState()
        }
    }
    
    // Trigger navigation on successful registration
    LaunchedEffect(registrationUIState.registrationResult) {
        val result = registrationUIState.registrationResult
        if (result is RegisterResult.Success) {
            onRegistrationSuccess(result.requiresEmailVerification)
            // No need to call viewModel.acknowledgeOperationSuccess() if it just clears the flag,
            // as clearRegistrationResult() should handle resetting the state.
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.register_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBackToEntry) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues) // Apply padding from Scaffold
                .fillMaxSize() // Fill available space after Scaffold
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Email Field
            FieldWithError(
                value = emailState.text,
                label = stringResource(R.string.register_email_label),
                errorMessage = registrationFieldErrors[RegistrationField.EMAIL],
                onValueChange = { viewModel.updateEmail(TextFieldValue(it)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                enabled = !isLoading
            )
            
            // Phone Field
            FieldWithError(
                value = phoneState.text,
                label = stringResource(R.string.register_phone_label),
                errorMessage = registrationFieldErrors[RegistrationField.PHONE],
                onValueChange = { viewModel.updatePhone(TextFieldValue(it)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                enabled = !isLoading
            )
            
            // Password Field
            FieldWithError(
                value = passwordState.text,
                label = stringResource(R.string.register_password_label),
                errorMessage = registrationFieldErrors[RegistrationField.PASSWORD],
                onValueChange = { viewModel.updatePassword(TextFieldValue(it)) },
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onPasswordVisibilityToggle = viewModel::togglePasswordVisibility,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                enabled = !isLoading
            )
            
            // Confirm Password Field
            FieldWithError(
                value = confirmPasswordState.text,
                label = stringResource(R.string.register_confirm_password_label),
                errorMessage = registrationFieldErrors[RegistrationField.CONFIRM_PASSWORD],
                onValueChange = { viewModel.updateConfirmPassword(TextFieldValue(it)) },
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onPasswordVisibilityToggle = viewModel::togglePasswordVisibility,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (!isLoading) viewModel.registerUser()
                }),
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.registerUser()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.register_button_text))
                }
            }
            
            TextButton(
                onClick = onNavigateToLogin,
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.register_login_prompt))
            }
        }
    }
}

// Helper Composable for TextFields with error display (Your existing one, slight mods for clarity)
@Composable
private fun FieldWithError(
    value: String,
    label: String,
    errorMessage: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityToggle: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    enabled: Boolean = true
) {
    Column(modifier.fillMaxWidth()) { // Ensure Column takes full width for proper alignment
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPassword && onPasswordVisibilityToggle != null) {
                {
                    IconButton(onClick = onPasswordVisibilityToggle, enabled = enabled) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (isPasswordVisible) R.string.login_hide_password_description
                                else R.string.login_show_password_description
                            )
                        )
                    }
                }
            } else null,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            isError = errorMessage != null,
            enabled = enabled
        )
        
        // Error message display area with consistent height
        val errorTextHeight =
            MaterialTheme.typography.bodySmall.lineHeight.value.dp + 4.dp
        Box(
            modifier = Modifier
                .height(errorTextHeight)
                .padding(start = 16.dp, top = 2.dp)
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}