package com.example.campfire.auth.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campfire.auth.presentation.composables.LoginScreen
import com.example.campfire.auth.presentation.composables.RegisterScreen
import dagger.hilt.android.AndroidEntryPoint


@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AuthApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthApp() {
    LocalContext.current
    val viewModel: AuthViewModel = hiltViewModel()
    rememberCoroutineScope()
    //Show different screens
    var showRegister by rememberSaveable { mutableStateOf(true) } // Start with register
    var showEmailVerification by rememberSaveable { mutableStateOf(false) }
    var showPhoneVerification by rememberSaveable { mutableStateOf(false) }
    var showLogin by rememberSaveable { mutableStateOf(false) }
    
    // Alert Dialog
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var dialogMessage by rememberSaveable { mutableStateOf("") }
    
    // Observe the message from the ViewModel and show in dialog
    val message by viewModel.message
    LaunchedEffect(message) {
        if (message.isNotBlank()) {
            dialogMessage = message
            showDialog = true
            viewModel.updateMessage("") // Clear the message after displaying it
        }
    }
    
    // Show dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            title = { Text("Message") },
            text = { Text(dialogMessage) },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                }) {
                    Text("OK")
                }
            }
        )
    }
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Authentication") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (showRegister) {
                RegisterScreen(
                    viewModel = viewModel,
                    onRegister = { viewModel.registerUser() },
                    onNavigateToLogin = {
                        showRegister = false
                        showLogin = true
                        viewModel.resetInputFields()
                    },
                    onNavigateToEmailVerification = {
                        showRegister = false
                        showEmailVerification = true
                    }
                )
            } else if (showEmailVerification) {
                EmailVerificationScreen(
                    viewModel = viewModel,
                    onVerifyEmail = { viewModel.verifyEmail() },
                    onNavigateToPhoneVerification = {
                        showEmailVerification = false
                        showPhoneVerification = true
                        viewModel.resetInputFields()
                    }
                )
            } else if (showPhoneVerification) {
                PhoneVerificationScreen(
                    viewModel = viewModel,
                    onVerifyPhone = { viewModel.verifyPhone() },
                    onNavigateToLogin = {
                        showPhoneVerification = false
                        showLogin = true
                        viewModel.resetInputFields()
                    }
                )
            } else if (showLogin) {
                LoginScreen(
                    viewModel = viewModel,
                    onLogin = { viewModel.loginUser() },
                    onNavigateToRegister = {
                        showLogin = false
                        showRegister = true
                        viewModel.resetInputFields()
                    }
                )
            }
        }
    }
}

//Composable for Email Verification Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    viewModel: AuthViewModel,
    onVerifyEmail: () -> Unit,
    onNavigateToPhoneVerification: () -> Unit
) {
    val email by viewModel.email
    val emailCode by viewModel.emailCode
    val isLoading by viewModel.isLoading
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Verify Email", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 24.sp))
        Text("Please enter the code sent to ${email.text}") //show email
        OutlinedTextField(
            value = emailCode,
            onValueChange = viewModel::updateEmailCode,
            label = { Text("Verification Code") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                onVerifyEmail()
                onNavigateToPhoneVerification()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Text("Verify Email")
            }
        }
    }
}

//Composable for Phone Verification Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneVerificationScreen(
    viewModel: AuthViewModel,
    onVerifyPhone: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val phone by viewModel.phone
    val phoneCode by viewModel.phoneCode
    val isLoading by viewModel.isLoading
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Verify Phone Number",
            style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 24.sp)
        )
        Text("Please enter the code sent to ${phone.text}") //show phone number.
        OutlinedTextField(
            value = phoneCode,
            onValueChange = viewModel::updatePhoneCode,
            label = { Text("Verification Code") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                onVerifyPhone()
                onNavigateToLogin()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Text("Verify Phone")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Surface {
        AuthApp()
    }
}