package com.example.campfire.auth.presentation.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.presentation.components.UnderlinedInputText
import com.example.campfire.core.presentation.components.UnderlinedTextField
import com.example.campfire.core.presentation.components.UsPhoneNumberVisualTransformation
import com.example.campfire.core.presentation.utils.getFlagEmojiForRegionCode
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
    val phoneNumberTransformation = remember { UsPhoneNumberVisualTransformation() }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UnderlinedInputText(
            value = uiState.displayCountryDialCode.ifEmpty { "--" },
            onClick = {
                Firelog.d("PhoneNumberInputRow: Country code clicked.")
                onCountryCodeClick()
            },
            modifier = Modifier
                .weight(0.35f)
                .padding(vertical = 8.dp),
            textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Start),
            underlineColor = colorResource(id = R.color.onPrimaryContainer),
            showDropdownArrow = true,
            dropdownArrowContentDescription = stringResource(R.string.select_country_code_description),
            leadingIcon = {
                Text(
                    text = getFlagEmojiForRegionCode(uiState.selectedRegionCode),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        )
        Spacer(Modifier.width(16.dp))
        UnderlinedTextField(
            value = uiState.nationalNumberInput,
            onValueChange = onNationalNumberChange,
            modifier = Modifier
                .weight(0.65f)
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.headlineSmall,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    Firelog.d("PhoneNumberInputRow: Keyboard 'Done' action triggered.")
                    localFocusManager.clearFocus()
                    onDoneAction()
                }
            ),
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
    onNavigateToPickCountry: () -> Unit,
) {
    val uiState by viewModel.sendOTPUIState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    
    Firelog.i("Composing with AuthAction: $authAction. ViewModel hash: ${viewModel.hashCode()}")
    
    LaunchedEffect(key1 = authAction) {
        viewModel.setAuthOperationContext(
            action = authAction,
            phoneNumberE164 = null,
            isNewContext = true
        )
        scope.launch { nationalNumberFocusRequester.requestFocus() }
    }
    
    // Handle UserMessages (Snackbars, Toasts) from ViewModel
    LaunchedEffect(Unit) {
        Firelog.d("LaunchedEffect (UserMessages): Starting to collect user messages.")
        viewModel.userMessages.collectLatest { message ->
            Firelog.i("Received UserMessage: ${message::class.simpleName}")
            snackbarHostState.currentSnackbarData?.dismiss()
            when (message) {
                is UserMessage.Snackbar -> {
                    Firelog.d("Showing Snackbar (ResId: ${message.messageResId}, Args: ${message.args.joinToString()})")
                    snackbarHostState.showSnackbar(
                        message = context.getString(
                            message.messageResId,
                            *message.args.toTypedArray()
                        ),
                        duration = SnackbarDuration.Short
                    )
                }
                
                is UserMessage.SnackbarString -> {
                    Firelog.d("Showing Snackbar (String: ${message.message.take(50)}...)")
                    snackbarHostState.showSnackbar(
                        message = message.message,
                        duration = SnackbarDuration.Short
                    )
                }
                
                is UserMessage.Toast -> {
                    Firelog.d("Showing Toast (ResId: ${message.messageResId}, Args: ${message.args.joinToString()})")
                    Toast.makeText(
                        context,
                        context.getString(message.messageResId, *message.args.toTypedArray()),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                is UserMessage.ToastString -> {
                    Firelog.d("Showing Toast (String: ${message.message.take(50)}...)")
                    Toast.makeText(context, message.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // Handle Navigation Events from ViewModel
    LaunchedEffect(Unit) {
        Firelog.d("LaunchedEffect (AuthNavigationEvents): Starting to collect navigation events.")
        viewModel.authNavigationEvents.collectLatest { event ->
            Firelog.i("Received AuthNavigationEvent: ${event::class.simpleName}")
            when (event) {
                is AuthNavigationEvent.ToOTPVerifiedScreen -> {
                    Firelog.i("Triggering navigation to VerifyOTP. Phone (hash): ${event.phoneNumber.hashCode()}, Action: ${event.originatingAction}")
                    onNavigateToVerifyOTP(event.phoneNumber, event.originatingAction)
                }
                
                is AuthNavigationEvent.NavigateToPickCountry -> {
                    Firelog.i("Triggering navigation to PickCountry via ViewModel event.")
                    onNavigateToPickCountry()
                }
                
                else -> {
                    Firelog.e("Received unhandled navigation event: $event")
                }
            }
        }
    }
    
    LaunchedEffect(uiState.sendOTPResult) {
        val result = uiState.sendOTPResult
        if (result != null) {
            Firelog.i("Observed sendOTPResult: ${result::class.simpleName}. Details: $result")
        }
    }
    
    BackHandler(enabled = !uiState.isLoading) {
        Firelog.d("Back button pressed. Clearing sendOTPResult and navigating back.")
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
                            Firelog.d("Opening 'What if my number changes' link.")
                            try {
                                uriHandler.openUri("https://www.example.com/number-change-info") // Replace with actual URL
                            } catch (e: Exception) {
                                Firelog.e("Failed to open URI for number change info.", e)
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
                            Firelog.i("Send Code FAB clicked. Attempting to send OTP.")
                            keyboardController?.hide()
                            viewModel.attemptSendOTP()
                        } else {
                            Firelog.d("Send Code FAB clicked, but UI is loading. Action ignored.")
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
                .padding(horizontal = 16.dp),
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
            
            val errorTextHeight = 20.dp
            if (uiState.validationError != null) {
                Text(
                    text = uiState.validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp), // Space between error and input fields
                    textAlign = TextAlign.End
                )
            } else if (uiState.sendOTPResult is SendOTPResult.Generic) {
                Text(
                    text = (uiState.sendOTPResult as SendOTPResult.Generic).message
                        ?: stringResource(R.string.error_generic_api),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp), // Space between error and input fields
                    textAlign = TextAlign.Start
                )
            } else {
                // JD TODO: Maybe animate the appearance.
                Spacer(modifier = Modifier.height(errorTextHeight + 8.dp)) // height of text + padding
            }
            
            PhoneNumberInputRow(
                uiState = uiState,
                onNationalNumberChange = { newValue ->
                    viewModel.onNationalNumberInputValueChanged(newValue)
                    viewModel.clearErrorsOnInput() // New ViewModel function
                },
                onCountryCodeClick = {
                    Firelog.d("Country code input area clicked. Hiding keyboard and navigating to PickCountry.")
                    keyboardController?.hide()
                    viewModel.clearErrorsOnInput() // New ViewModel function
                    onNavigateToPickCountry()
                },
                focusRequester = nationalNumberFocusRequester,
                onDoneAction = {
                    if (!uiState.isLoading) {
                        Firelog.i("Phone number input 'Done' action. Attempting to send OTP.")
                        viewModel.attemptSendOTP()
                    } else {
                        Firelog.d("Phone number input 'Done' action, but UI is loading. Action ignored.")
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
            onNavigateToPickCountry = {},
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
            onNavigateToPickCountry = {},
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
            onNavigateToPickCountry = {},
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
            onNavigateToPickCountry = {},
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
            onNavigateToPickCountry = {},
        )
    }
}