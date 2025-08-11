package com.example.campfire.core.presentation.navigation

import android.util.Log
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.campfire.auth.presentation.AuthNavigationEvent
import com.example.campfire.auth.presentation.AuthViewModel
import com.example.campfire.auth.presentation.navigation.AuthScreen
import com.example.campfire.auth.presentation.navigation.authGraph
import com.example.campfire.feed.presentation.navigation.FeedScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    isAuthenticated: Boolean,
    isEntryComplete: Boolean,
    onAuthSuccess: () -> Unit,
    onLogout: () -> Unit,
) {
    val topLevelStartDestination = if (isAuthenticated) {
        AppGraphRoutes.MAIN_APP_GRAPH_ROUTE
    } else {
        AppGraphRoutes.AUTH_GRAPH_ROUTE
    }
    
    // --- Global Auth UI Logic (Scoped to Auth Graph) ---
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    
    val authGraphBackStackEntry = remember(currentBackStackEntry) {
        if (currentBackStackEntry?.destination?.parent?.route == AppGraphRoutes.AUTH_GRAPH_ROUTE || currentBackStackEntry?.destination?.route == AppGraphRoutes.AUTH_GRAPH_ROUTE
        ) {
            try {
                navController.getBackStackEntry(AppGraphRoutes.AUTH_GRAPH_ROUTE)
            } catch (_: IllegalArgumentException) {
                // The graph is not on the back stack (e.g., when main_app_graph is active)
                null
            }
        } else {
            null
        }
    }
    
    val authViewModel: AuthViewModel? = if (authGraphBackStackEntry != null) {
        hiltViewModel(authGraphBackStackEntry) // Pass the remembered entry
    } else {
        null
    }
    
    authViewModel?.let { vm ->
        LaunchedEffect(Unit) {
            authViewModel.authNavigationEvents.collect { event ->
                when (event) {
                    is AuthNavigationEvent.ToOTPVerifiedScreen -> {
                        // Navigate to VerifyOTPScreen, passing the phoneNumber (which AuthViewModel should have)
                        // and the event.originatingAction
                        val currentPhoneNumber =
                            authViewModel.currentPhoneNumberForVerification.value // Assuming AuthViewModel exposes this
                        if (currentPhoneNumber != null) {
                            navController.navigate(
                                AuthScreen.VerifyOTP.createRoute(
                                    phoneNumber = currentPhoneNumber,
                                    authAction = event.originatingAction
                                )
                            ) {
                                popUpTo(AuthScreen.EnterPhoneNumber.route)
                            }
                        } else {
                            Log.e(
                                "AppNavigation",
                                "Cannot navigate to VerifyOTP, phone number missing in ViewModel."
                            )
                        }
                    }
                    
                    is AuthNavigationEvent.ToProfileCompletion -> {
                        navController.navigate(AuthScreen.ProfileSetupName.route) {
                            popUpTo(AuthScreen.VerifyOTP.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    
                    is AuthNavigationEvent.ToMainApp -> {
                        onAuthSuccess() // Callback to update MainActivity's isAuthenticated state
                        navController.navigate(AppGraphRoutes.MAIN_APP_GRAPH_ROUTE) {
                            popUpTo(AppGraphRoutes.AUTH_GRAPH_ROUTE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }
    // --- End Global Auth UI Logic ---
    
    NavHost(
        navController = navController,
        startDestination = topLevelStartDestination
    ) {
        navigation(
            startDestination = AuthScreen.Entry.route,
            route = AppGraphRoutes.AUTH_GRAPH_ROUTE
        ) {
            authGraph(
                navController = navController
            )
        }
        
        navigation(
            startDestination = FeedScreen.FeedOverview.route,
            route = AppGraphRoutes.MAIN_APP_GRAPH_ROUTE
        ) {
            mainAppGraph(
                navController = navController,
                onLogout = {
                    onLogout()
                    navController.navigate(AppGraphRoutes.AUTH_GRAPH_ROUTE) {
                        popUpTo(AppGraphRoutes.MAIN_APP_GRAPH_ROUTE) { inclusive = true }
                    }
                }
            )
        }
    }
}