package com.example.campfire.auth.presentation.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campfire.R
import com.example.campfire.auth.domain.repository.SendOTPResult
import com.example.campfire.auth.presentation.AuthContract
import com.example.campfire.auth.presentation.AuthNavigationEvent
import com.example.campfire.auth.presentation.AuthViewModel
import com.example.campfire.auth.presentation.SendOTPUIState
import com.example.campfire.auth.presentation.UserMessage
import com.example.campfire.auth.presentation.navigation.AuthAction
import com.example.campfire.core.presentation.components.UnderlinedInputText
import com.example.campfire.core.presentation.components.UnderlinedTextField
import com.example.campfire.core.presentation.components.UsPhoneNumberVisualTransformation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


val nationalNumberFocusRequester = FocusRequester()

@Composable
fun PhoneNumberInputRow(
    uiState: SendOTPUIState,
    onNationalNumberChange: (TextFieldValue) -> Unit,
    onCountryCodeClick: () -> Unit,
    focusRequester: FocusRequester,
    onDoneAction: () -> Unit
) {
    val localFocusManager = LocalFocusManager.current
    val isError = uiState.validationError != null ||
            (uiState.sendOTPResult != null && uiState.sendOTPResult !is SendOTPResult.Success)
    val phoneNumberTransformation = remember { UsPhoneNumberVisualTransformation() }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UnderlinedInputText(
            value = uiState.displayCountryDialCode.ifEmpty { "--" },
            onClick = onCountryCodeClick,
            modifier = Modifier
                .weight(0.4f)
                .padding(end = 8.dp),
            textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
            isError = isError,
            underlineColor = colorResource(id = R.color.onPrimaryContainer),
            showDropdownArrow = true
        )
        
        Spacer(Modifier.width(8.dp))
        
        UnderlinedTextField(
            value = uiState.nationalNumberInput,
            onValueChange = { textFieldValue ->
                onNationalNumberChange(textFieldValue)
            },
            modifier = Modifier
                .weight(0.6f)
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.headlineSmall,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    localFocusManager.clearFocus()
                    onDoneAction()
                }
            ),
            isError = isError,
            singleLine = true,
            underlineColor = colorResource(id = R.color.onPrimaryContainer),
            visualTransformation = phoneNumberTransformation
        )
    }
}

@Composable
fun EnterPhoneNumberScreen(
    authAction: AuthAction,
    viewModel: AuthContract = hiltViewModel<AuthViewModel>(),
    onNavigateToVerifyOTP: (phoneNumber: String, authAction: AuthAction) -> Unit,
    onNavigateBack: () -> Unit,
    onShowCountryPicker: () -> Unit
) {
    val uiState by viewModel.sendOTPUIState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    
    // Set operation context when authAction changes or screen is first composed
    LaunchedEffect(key1 = authAction) {
        viewModel.setAuthOperationContext(
            action = authAction,
            phoneNumberE164 = null,
            isNewContext = true
        )
        nationalNumberFocusRequester.requestFocus()
    }
    
    // Handle UserMessages (Snackbars, Toasts) from ViewModel
    LaunchedEffect(Unit) {
        viewModel.userMessages.collectLatest { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            when (message) {
                is UserMessage.Snackbar -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(
                            message.messageResId,
                            *message.args.toTypedArray()
                        ),
                        duration = SnackbarDuration.Short
                    )
                }
                
                is UserMessage.SnackbarString -> {
                    snackbarHostState.showSnackbar(
                        message = message.message,
                        duration = SnackbarDuration.Short
                    )
                }
                
                is UserMessage.Toast -> {
                    Toast.makeText(
                        context,
                        context.getString(message.messageResId, *message.args.toTypedArray()),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                is UserMessage.ToastString -> {
                    Toast.makeText(context, message.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // Handle Navigation Events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.authNavigationEvents.collectLatest { event ->
            when (event) {
                is AuthNavigationEvent.ToOTPVerifiedScreen -> {
                    onNavigateToVerifyOTP(event.phoneNumber, event.originatingAction)
                }
                
                else -> {
                    Log.d("EnterPhoneNumberScreen", "Received unhandled navigation event: $event")
                }
            }
        }
    }
    
    LaunchedEffect(uiState.sendOTPResult) {
        val result = uiState.sendOTPResult
        if (result != null) {
            Log.d("EnterPhoneNumberScreen", "sendOTPResult observed: $result")
        }
    }
    
    BackHandler(enabled = !uiState.isLoading) {
        viewModel.clearSendOTPResult()
        onNavigateBack()
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(start = 16.dp)
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.what_if_my_number_changes_link),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = colorResource(id = R.color.onPrimaryContainer),
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            try {
                                uriHandler.openUri("https://www.example.com/number-change-info") // Replace with actual URL
                            } catch (e: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Could not open link.")
                                }
                            }
                        }
                        .padding(horizontal = 8.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                FloatingActionButton(
                    onClick = {
                        if (!uiState.isLoading) {
                            keyboardController?.hide()
                            viewModel.attemptSendOTP()
                        }
                    },
                    containerColor = if (!uiState.isLoading) colorResource(id = R.color.onPrimaryContainer) else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (!uiState.isLoading) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = colorResource(id = R.color.onPrimaryContainer),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.send_code_button)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Spacer(modifier = Modifier.height(56.dp))
            
            Icon(
                imageVector = Icons.Filled.PhoneAndroid,
                contentDescription = stringResource(R.string.phone_icon_description),
                modifier = Modifier
                    .size(56.dp)
                    .padding(bottom = 16.dp),
                tint = colorResource(id = R.color.onPrimaryContainer)
            )
            
            Text(
                text = stringResource(R.string.enter_your_phone_number_prompt),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Left
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            PhoneNumberInputRow(
                uiState = uiState,
                onNationalNumberChange = viewModel::onNationalNumberInputValueChanged,
                onCountryCodeClick = {
                    keyboardController?.hide()
                    onShowCountryPicker()
                },
                focusRequester = nationalNumberFocusRequester,
                onDoneAction = {
                    if (!uiState.isLoading) {
                        viewModel.attemptSendOTP()
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.send_code_as_text_info),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Left,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- Previews ---

// Helper function for creating a ViewModel for previews
@Composable
private fun previewViewModel(initialState: SendOTPUIState = SendOTPUIState()): AuthContract {
    return remember(initialState) {
        FakeEnterPhoneNumberViewModel(initialState = initialState)
    }
}

@Preview(showBackground = true, name = "Login - Idle")
@Composable
fun EnterPhoneNumberScreenPreview_Idle() {
    MaterialTheme {
        EnterPhoneNumberScreen(
            authAction = AuthAction.LOGIN,
            viewModel = previewViewModel(
                SendOTPUIState(
                    selectedRegionCode = "US",
                    displayCountryDialCode = "+1"
                )
            ),
            onNavigateToVerifyOTP = { phoneNumber, authAction -> },
            onNavigateBack = {},
            onShowCountryPicker = {}
        )
    }
}

@Preview(showBackground = true, name = "Login - Number Entered")
@Composable
fun EnterPhoneNumberScreenPreview_NumberEntered() {
    MaterialTheme {
        EnterPhoneNumberScreen(
            authAction = AuthAction.LOGIN,
            viewModel = previewViewModel(
                SendOTPUIState(
                    selectedRegionCode = "US",
                    displayCountryDialCode = "+1",
                    nationalNumberInput = TextFieldValue("1234567890")
                )
            ),
            onNavigateToVerifyOTP = { phoneNumber, authAction -> },
            onNavigateBack = {},
            onShowCountryPicker = {}
        )
    }
}

@Preview(showBackground = true, name = "Login - Loading")
@Composable
fun EnterPhoneNumberScreenPreview_Loading() {
    MaterialTheme {
        EnterPhoneNumberScreen(
            authAction = AuthAction.LOGIN,
            viewModel = previewViewModel(
                SendOTPUIState(
                    selectedRegionCode = "US",
                    displayCountryDialCode = "+1",
                    nationalNumberInput = TextFieldValue("1234567890"),
                    isLoading = true
                )
            ),
            onNavigateToVerifyOTP = { phoneNumber, authAction -> },
            onNavigateBack = {},
            onShowCountryPicker = {}
        )
    }
}

@Preview(showBackground = true, name = "Login - Validation Error")
@Composable
fun EnterPhoneNumberScreenPreview_ValidationError() {
    MaterialTheme {
        EnterPhoneNumberScreen(
            authAction = AuthAction.LOGIN,
            viewModel = previewViewModel(
                SendOTPUIState(
                    selectedRegionCode = "US",
                    displayCountryDialCode = "+1",
                    nationalNumberInput = TextFieldValue("123"), // Invalid input
                    validationError = "Phone number is too short." // Simulate validation error in UI state
                )
            ),
            onNavigateToVerifyOTP = { phoneNumber, authAction -> },
            onNavigateBack = {},
            onShowCountryPicker = {}
        )
    }
}

@Preview(showBackground = true, name = "Login - API Error")
@Composable
fun EnterPhoneNumberScreenPreview_ApiError() {
    MaterialTheme {
        EnterPhoneNumberScreen(
            authAction = AuthAction.LOGIN,
            viewModel = previewViewModel(
                SendOTPUIState(
                    selectedRegionCode = "US",
                    displayCountryDialCode = "+1",
                    nationalNumberInput = TextFieldValue("1234567890"),
                    sendOTPResult = SendOTPResult.Generic("A network error occurred") // Simulate API error
                )
            ),
            onNavigateToVerifyOTP = { phoneNumber, authAction -> },
            onNavigateBack = {},
            onShowCountryPicker = {}
        )
    }
}