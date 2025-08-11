package com.example.campfire.auth.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campfire.auth.domain.repository.Field
import com.example.campfire.auth.presentation.AuthContract
import com.example.campfire.auth.presentation.AuthNavigationEvent
import com.example.campfire.auth.presentation.AuthViewModel
import kotlinx.coroutines.flow.collectLatest

@Suppress("unused")
@Composable
fun CompleteProfileScreen(
    viewModel: AuthContract = hiltViewModel<AuthViewModel>(),
    onNavigateToMainApp: () -> Unit
) {
    val uiState by viewModel.completeProfileUIState.collectAsState()
    LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Observe UserMessages
    LaunchedEffect(Unit) {
        viewModel.userMessages.collectLatest { message ->
            // ... (handle UserMessage like in VerifyOTPScreen)
        }
    }
    
    // Observe NavigationEvents
    LaunchedEffect(Unit) {
        viewModel.authNavigationEvents.collectLatest { event ->
            if (event is AuthNavigationEvent.ToMainApp) {
                onNavigateToMainApp()
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // ... TopAppBar etc. ...
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // --- First Name ---
            OutlinedTextField(
                value = uiState.firstName,
                onValueChange = viewModel::onFirstNameChanged,
                label = { Text("First Name") },
                isError = uiState.fieldErrors.containsKey(Field.FIRST_NAME)
            )
            uiState.fieldErrors[Field.FIRST_NAME]?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            // --- Last Name ---
            // ... similar for lastName, email, dateOfBirthInput, enableNotifications ...
            
            // --- Date of Birth (Example with a DatePicker dialog interaction) ---
            // You'd have a button to show a DatePickerDialog.
            // When date is selected, call viewModel.onDateOfBirthInputChanged with the formatted string
            // or directly set the LocalDate if your dialog gives it back.
            
            // --- Enable Notifications ---
            Row {
                Checkbox(
                    checked = uiState.enableNotifications,
                    onCheckedChange = viewModel::onEnableNotificationsChanged
                )
                Text("Enable Notifications")
            }
            
            
            Button(onClick = viewModel::completeUserProfile, enabled = !uiState.isLoading) {
                if (uiState.isLoading) CircularProgressIndicator() else Text("Complete Profile")
            }
        }
    }
}