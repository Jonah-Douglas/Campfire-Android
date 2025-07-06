package com.example.campfire.auth.presentation.navigation

import androidx.compose.material3.Text
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.campfire.auth.presentation.screens.EntryScreen
import com.example.campfire.auth.presentation.screens.LoginScreen
import com.example.campfire.auth.presentation.screens.RegisterScreen
import com.example.campfire.core.presentation.navigation.AppGraphRoutes


fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    onAuthSuccessNavigation: () -> Unit,
    onEntryScreenComplete: () -> Unit
) {
    composable(AuthScreen.Entry.route) {
        EntryScreen(
            onNavigateToLogin = {
                onEntryScreenComplete()
                navController.navigate(AuthScreen.Login.route) {
                    // Prevent swiping back to Entry from Login
                    popUpTo(AuthScreen.Entry.route) { inclusive = true }
                }
            },
            onNavigateToRegister = {
                onEntryScreenComplete()
                navController.navigate(AuthScreen.Register.route) {
                    // Prevent swiping back to Entry from Login
                    popUpTo(AuthScreen.Entry.route) { inclusive = true }
                }
            }
        )
    }
    composable(AuthScreen.Login.route) {
        LoginScreen(
            onLoginSuccess = { requiresEmailVerification ->
                if (requiresEmailVerification) {
                    navController.navigate(AuthScreen.EmailVerification.route) {
                        popUpTo(AuthScreen.Login.route) { inclusive = true }
                    }
                } else {
                    onAuthSuccessNavigation()
                }
            },
            onNavigateToRegister = {
                navController.navigate(AuthScreen.Register.route) {
                    // When going from Login to Register, pop Login so back goes to Entry
                    popUpTo(AuthScreen.Login.route) { inclusive = true }
                }
            },
            onNavigateBackToEntry = {
                navController.navigate(AuthScreen.Entry.route) {
                    // Pop Login from the back stack when going back to Entry
                    popUpTo(AuthScreen.Login.route) { inclusive = true }
                }
            }
        )
    }
    composable(AuthScreen.Register.route) {
        RegisterScreen(
            onRegistrationSuccess = { requiresEmailVerification ->
                if (requiresEmailVerification) {
                    navController.navigate(AuthScreen.EmailVerification.route) {
                        popUpTo(AuthScreen.Register.route) { inclusive = true }
                    }
                } else {
                    // After successful registration, navigate to Login
                    navController.navigate(AuthScreen.Login.route) {
                        popUpTo(AppGraphRoutes.AUTH_GRAPH_ROUTE) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            },
            onNavigateToLogin = {
                navController.navigate(AuthScreen.Login.route) {
                    popUpTo(AuthScreen.Register.route) { inclusive = true }
                }
            },
            onNavigateBackToEntry = {
                navController.navigate(AuthScreen.Entry.route) {
                    popUpTo(AuthScreen.Register.route) { inclusive = true }
                }
            }
        )
    }
    composable(AuthScreen.EmailVerification.route) {
        // Replace with your actual EmailVerificationScreen
        Text("Email Verification Screen Placeholder (Implement Me)")
    }
    composable(AuthScreen.PhoneVerification.route) { // Assuming you have this in AuthScreen
        Text("Phone Verification Screen Placeholder (Implement Me)")
    }
}