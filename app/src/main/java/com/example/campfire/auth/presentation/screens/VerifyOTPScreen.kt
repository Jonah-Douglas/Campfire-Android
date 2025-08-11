package com.example.campfire.auth.presentation.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campfire.R
import com.example.campfire.auth.domain.repository.VerifyOTPResult
import com.example.campfire.auth.presentation.AuthContract
import com.example.campfire.auth.presentation.AuthViewModel
import com.example.campfire.auth.presentation.UserMessage
import com.example.campfire.auth.presentation.VerifyOTPUIState
import com.example.campfire.auth.presentation.navigation.AuthAction
import kotlinx.coroutines.flow.collectLatest


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyOTPScreen(
    phoneNumberFromNav: String,
    authActionFromNav: AuthAction,
    viewModel: AuthContract = hiltViewModel<AuthViewModel>(),
    onNavigateBack: () -> Unit,
) {
    val verifyUIState by viewModel.verifyOTPUIState.collectAsState()
    val sendUIState by viewModel.sendOTPUIState.collectAsState()
    val currentDisplayPhoneNumber by viewModel.currentPhoneNumberForVerification.collectAsState()
    
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Initialize ViewModel with Nav Args
    LaunchedEffect(key1 = authActionFromNav, key2 = phoneNumberFromNav) {
        viewModel.setAuthOperationContext(
            action = authActionFromNav,
            phoneNumberE164 = phoneNumberFromNav,
            isNewContext = true
        )
    }
    
    // Observe UserMessages from ViewModel
    LaunchedEffect(Unit) {
        viewModel.userMessages.collectLatest { message ->
            when (message) {
                is UserMessage.Snackbar -> snackbarHostState.showSnackbar(
                    context.getString(message.messageResId, *message.args.toTypedArray())
                )
                
                is UserMessage.Toast -> Toast.makeText(
                    context,
                    context.getString(message.messageResId, *message.args.toTypedArray()),
                    Toast.LENGTH_SHORT
                ).show()
                
                is UserMessage.SnackbarString -> snackbarHostState.showSnackbar(message.message)
                is UserMessage.ToastString -> Toast.makeText(
                    context,
                    message.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    BackHandler(enabled = !verifyUIState.isLoading) {
        onNavigateBack()
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            val fabEnabled = !verifyUIState.isLoading &&
                    verifyUIState.otpCode.text.length == 6 &&
                    verifyUIState.otpCode.text.all { it.isDigit() }
            FloatingActionButton(
                onClick = {
                    if (fabEnabled) {
                        viewModel.verifyOTP()
                    }
                },
                containerColor = if (fabEnabled) colorResource(id = R.color.onPrimaryContainer) else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (fabEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 16.dp),
                elevation = FloatingActionButtonDefaults.elevation()
            ) {
                if (verifyUIState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = colorResource(id = R.color.onPrimaryContainer),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.verify_button)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(64.dp))
            
            Text(
                text = stringResource(
                    R.string.enter_your_verification_code,
                    currentDisplayPhoneNumber ?: phoneNumberFromNav
                ),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(24.dp))
            
            OutlinedTextField(
                value = verifyUIState.otpCode,
                onValueChange = viewModel::onOTPCodeChanged,
                label = { Text(stringResource(R.string.otp_code_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                isError = verifyUIState.verifyOTPResult is VerifyOTPResult.OTPIncorrect ||
                        verifyUIState.verifyOTPResult is VerifyOTPResult.OTPExpired,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    textAlign = TextAlign.Center,
                    letterSpacing = MaterialTheme.typography.headlineSmall.letterSpacing * 2
                )
            )
            
            Spacer(Modifier.weight(1f))
            
            TextButton(
                onClick = {
                    if (!sendUIState.isLoading && !verifyUIState.isLoading) {
                        viewModel.resendOTP()
                    }
                },
                enabled = !sendUIState.isLoading && !verifyUIState.isLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                if (sendUIState.isLoading) { // Check resend loading state
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.resending_code_button),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else {
                    Text(
                        stringResource(R.string.resend_code_button),
                        color = colorResource(id = R.color.onPrimaryContainer)
                    )
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Preview(showBackground = true, name = "Verify OTP Screen - Idle")
@Composable
fun VerifyOTPScreenIdlePreview() {
    MaterialTheme {
        VerifyOTPScreen(
            phoneNumberFromNav = "1234567890",
            authActionFromNav = AuthAction.LOGIN,
            viewModel = FakeVerifyOTPViewModel(
                initialPhoneNumber = "1234567890",
                initialVerifyState = VerifyOTPUIState(otpCode = TextFieldValue(""))
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Verify OTP Screen - Code Entered")
@Composable
fun VerifyOTPScreenCodeEnteredPreview() {
    MaterialTheme {
        VerifyOTPScreen(
            phoneNumberFromNav = "1234567890",
            authActionFromNav = AuthAction.LOGIN,
            viewModel = FakeVerifyOTPViewModel(
                initialPhoneNumber = "1234567890",
                initialVerifyState = VerifyOTPUIState(otpCode = TextFieldValue("123456"))
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Verify OTP Screen - Loading")
@Composable
fun VerifyOTPScreenLoadingPreview() {
    MaterialTheme {
        VerifyOTPScreen(
            phoneNumberFromNav = "1234567890",
            authActionFromNav = AuthAction.LOGIN,
            viewModel = FakeVerifyOTPViewModel(
                initialPhoneNumber = "1234567890",
                initialVerifyState = VerifyOTPUIState(
                    otpCode = TextFieldValue("123456"),
                    isLoading = true
                )
            ),
            onNavigateBack = {}
        )
    }
}