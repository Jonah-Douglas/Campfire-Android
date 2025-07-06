package com.example.campfire.auth.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campfire.auth.presentation.AuthViewModel

// Import verification result if you define one
// import com.example.campfire.auth.domain.repository.EmailVerificationResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    onVerificationSuccessAndNavigateToPhone: () -> Unit, // Or directly to login/main app
    onVerificationFailedMaybeNavigateBack: () -> Unit // Optional
) {
    val viewModel: AuthViewModel = hiltViewModel()
    val email by viewModel.email // Assuming this is set from registration or login flow
    val emailCode by viewModel.emailCode
    // val verificationState by viewModel.emailVerificationUIState.collectAsState() // Example state
    
    // --- Placeholder for isLoading and dialog ---
    val isLoading = false // Replace with viewModel.emailVerificationUIState.isLoading
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit /* Replace with verificationState.result or error */) {
        // Handle result from viewModel.verifyEmail()
        // If success -> onVerificationSuccessAndNavigateToPhone()
        // If failure -> showDialog with message
    }
    
    if (showDialog && dialogMessage != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false; dialogMessage = null },
            title = { Text("Verification") },
            text = { Text(dialogMessage!!) },
            confirmButton = {
                Button(onClick = { showDialog = false; dialogMessage = null }) {
                    Text(
                        "OK"
                    )
                }
            }
        )
    }
    // ---
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Verify Email") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Verify Your Email",
                style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 24.sp)
            )
            Text("Please enter the code sent to ${email.text}") // Display the email from VM
            OutlinedTextField(
                value = emailCode,
                onValueChange = viewModel::updateEmailCode,
                label = { Text("Verification Code") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    // JD TODO: Implement email verification (need to create API resource too)
                    //viewModel.verifyEmail() // This should update a state in ViewModel
                    // Navigation will be handled by LaunchedEffect observing that state
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && emailCode.text.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Verify Email")
                }
            }
            // Optional: Resend code button
            TextButton(onClick = { /* viewModel.resendEmailVerificationCode() */ }) {
                Text("Resend Code")
            }
        }
    }
}