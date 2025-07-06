package com.example.campfire.auth.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campfire.R
import com.example.campfire.auth.domain.repository.LoginResult
import com.example.campfire.auth.presentation.AuthViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (requiresEmailVerification: Boolean) -> Unit,
    onNavigateBackToEntry: () -> Unit
) {
    BackHandler(enabled = true) { }
    
    val viewModel: AuthViewModel = hiltViewModel()
    
    val emailState by viewModel.email
    val passwordState by viewModel.password
    val isPasswordVisible by viewModel.isPasswordVisible
    val localMessage by viewModel.message.collectAsState()
    val loginUIState by viewModel.loginUIState.collectAsState()
    val isLoading = loginUIState.isLoading
    
    val focusManager = LocalFocusManager.current
    
    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("Login Failed") }
    var dialogMessageContent by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(loginUIState.loginResult, loginUIState.errorMessage) {
        val result = loginUIState.loginResult
        val errorMsg = loginUIState.errorMessage
        
        if (result != null) {
            when (result) {
                is LoginResult.Success -> { }
                is LoginResult.InvalidCredentialsError -> {
                    dialogTitle = "Login Failed"; dialogMessageContent = result.message ?: "Invalid email or password."; showDialog = true
                }
                is LoginResult.UserInactiveError -> {
                    dialogTitle = "Account Issue"; dialogMessageContent = result.message ?: "This user account is inactive."; showDialog = true
                }
                is LoginResult.NetworkError -> {
                    dialogTitle = "Network Error"; dialogMessageContent = result.message ?: "A network error occurred."; showDialog = true
                }
                is LoginResult.GenericError -> {
                    dialogTitle = "Error"; dialogMessageContent = result.message ?: "An unexpected error occurred."; showDialog = true
                }
            }
            viewModel.clearLoginResult()
        } else if (errorMsg != null) {
            dialogTitle = "Message"; dialogMessageContent = errorMsg; showDialog = true
            viewModel.clearErrorMessage()
        }
    }
    
    LaunchedEffect(loginUIState.loginResult) {
        if (loginUIState.loginResult is LoginResult.Success) {
            // JD TODO: Verify email
            val successResult = loginUIState.loginResult as LoginResult.Success
            onLoginSuccess(false /* Replace with successResult.requiresEmailVerification if available */)
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearMessage()
            viewModel.clearLoginResult()
            viewModel.clearErrorMessage()
        }
    }
    
    LoginScreenContent(
        emailInput = emailState,
        passwordInput = passwordState,
        isPasswordVisible = isPasswordVisible,
        isLoading = isLoading,
        localMessage = localMessage,
        dialogTitle = dialogTitle,
        dialogMessageContent = dialogMessageContent,
        showDialog = showDialog,
        loginResultForErrors = loginUIState.loginResult,
        
        onEmailChange = { tfv -> viewModel.updateEmail(tfv) },
        onPasswordChange = { tfv -> viewModel.updatePassword(tfv) },
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onLoginClick = {
            focusManager.clearFocus()
            viewModel.clearMessage()
            viewModel.loginUser()
        },
        onNavigateToRegister = {
            viewModel.clearMessage()
            onNavigateToRegister()
        },
        onNavigateBackToEntry = onNavigateBackToEntry,
        onDismissDialog = {
            showDialog = false
            dialogMessageContent = null
            viewModel.clearLoginResult()
        },
        onConfirmDialog = {
            showDialog = false
            dialogMessageContent = null
        },
        onPasswordKeyboardDone = {
            focusManager.clearFocus()
            if (!isLoading && emailState.text.isNotBlank() && passwordState.text.isNotBlank()) {
                viewModel.clearMessage()
                viewModel.loginUser()
            }
        },
        focusManager = focusManager
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreenContent(
    emailInput: TextFieldValue,
    passwordInput: TextFieldValue,
    isPasswordVisible: Boolean,
    isLoading: Boolean,
    localMessage: String?,
    dialogTitle: String,
    dialogMessageContent: String?,
    showDialog: Boolean,
    loginResultForErrors: LoginResult?,
    
    onEmailChange: (TextFieldValue) -> Unit,
    onPasswordChange: (TextFieldValue) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLoginClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateBackToEntry: () -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmDialog: () -> Unit,
    onPasswordKeyboardDone: () -> Unit,
    focusManager: FocusManager
) {
    if (showDialog && dialogMessageContent != null) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            title = { Text(dialogTitle) },
            text = { Text(dialogMessageContent) },
            confirmButton = {
                Button(onClick = onConfirmDialog) { Text(stringResource(R.string.ok_button)) }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.login_title)) },
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
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.login_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = emailInput.text,
                onValueChange = { onEmailChange(TextFieldValue(it)) },
                label = { Text(stringResource(id = R.string.login_email_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                isError = localMessage?.contains("email", ignoreCase = true) == true ||
                        (loginResultForErrors is LoginResult.InvalidCredentialsError),
                singleLine = true,
                enabled = !isLoading
            )
            
            OutlinedTextField(
                value = passwordInput.text,
                onValueChange = { onPasswordChange(TextFieldValue(it)) },
                label = { Text(stringResource(id = R.string.login_password_label)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = onTogglePasswordVisibility,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (isPasswordVisible) R.string.login_hide_password_description
                                else R.string.login_show_password_description
                            )
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    onPasswordKeyboardDone()
                }),
                isError = localMessage?.contains("password", ignoreCase = true) == true ||
                        (loginResultForErrors is LoginResult.InvalidCredentialsError),
                singleLine = true,
                enabled = !isLoading
            )
            
            val currentLocalMessage = localMessage
            if (currentLocalMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentLocalMessage,
                    color = if (loginResultForErrors is LoginResult.Success) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            } else if (loginResultForErrors != null && loginResultForErrors !is LoginResult.Success) {
                Spacer(modifier = Modifier.height(8.dp + MaterialTheme.typography.bodySmall.lineHeight.value.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp + MaterialTheme.typography.bodySmall.lineHeight.value.dp))
            }
            
            
            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && emailInput.text.isNotBlank() && passwordInput.text.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(id = R.string.login_button_text))
                }
            }
            
            TextButton(
                onClick = onNavigateToRegister,
                enabled = !isLoading
            ) {
                Text(stringResource(id = R.string.login_register_prompt))
            }
        }
    }
}

// --- Previews ---

@Preview(showBackground = true)
@Composable
fun LoginScreen_Default() {
    MaterialTheme {
        LoginScreenContent(
            emailInput = TextFieldValue(""),
            passwordInput = TextFieldValue(""),
            isPasswordVisible = false,
            isLoading = false,
            localMessage = null,
            dialogTitle = "Preview Dialog",
            dialogMessageContent = null,
            showDialog = false,
            loginResultForErrors = null,
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onNavigateToRegister = {},
            onNavigateBackToEntry = {},
            onDismissDialog = {},
            onConfirmDialog = {},
            onPasswordKeyboardDone = {},
            focusManager = LocalFocusManager.current
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreen_Loading() {
    MaterialTheme {
        LoginScreenContent(
            emailInput = TextFieldValue("test@example.com"),
            passwordInput = TextFieldValue("password"),
            isPasswordVisible = false,
            isLoading = true,
            localMessage = null,
            dialogTitle = "Preview Dialog",
            dialogMessageContent = null,
            showDialog = false,
            loginResultForErrors = null,
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onNavigateToRegister = {},
            onNavigateBackToEntry = {},
            onDismissDialog = {},
            onConfirmDialog = {},
            onPasswordKeyboardDone = {},
            focusManager = LocalFocusManager.current
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreen_InvalidCredentials() {
    MaterialTheme {
        LoginScreenContent(
            emailInput = TextFieldValue("wrong@example.com"),
            passwordInput = TextFieldValue("wrong pass"),
            isPasswordVisible = false,
            isLoading = false,
            localMessage = "Invalid credentials.",
            dialogTitle = "Login Failed",
            dialogMessageContent = "Invalid email or password.",
            showDialog = false,
            loginResultForErrors = LoginResult.InvalidCredentialsError("Invalid email or password from result."),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onNavigateToRegister = {},
            onNavigateBackToEntry = {},
            onDismissDialog = {},
            onConfirmDialog = {},
            onPasswordKeyboardDone = {},
            focusManager = LocalFocusManager.current
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreen_DialogShown() {
    MaterialTheme {
        LoginScreenContent(
            emailInput = TextFieldValue("test@example.com"),
            passwordInput = TextFieldValue("password"),
            isPasswordVisible = false,
            isLoading = false,
            localMessage = null,
            dialogTitle = "Network Error",
            dialogMessageContent = "Could not connect to the server. Please check your internet connection.",
            showDialog = true,
            loginResultForErrors = LoginResult.NetworkError("Preview network error"),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onNavigateToRegister = {},
            onNavigateBackToEntry = {},
            onDismissDialog = {},
            onConfirmDialog = {},
            onPasswordKeyboardDone = {},
            focusManager = LocalFocusManager.current
        )
    }
}