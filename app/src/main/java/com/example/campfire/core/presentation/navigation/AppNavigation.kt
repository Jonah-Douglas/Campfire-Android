package com.example.campfire.core.presentation.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.campfire.auth.presentation.AuthNavigationEvent
import com.example.campfire.auth.presentation.AuthViewModel
import com.example.campfire.auth.presentation.navigation.AuthScreen
import com.example.campfire.auth.presentation.navigation.authGraph
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.feed.presentation.navigation.FeedScreen


/**
 * The main navigation Composable for the application.
 *
 * This Composable sets up the top-level navigation structure using a [NavHost].
 * It determines the initial navigation graph ([AppGraphRoutes.AUTH_GRAPH_ROUTE] or
 * [AppGraphRoutes.MAIN_APP_GRAPH_ROUTE]) based on the user's authentication status.
 *
 * It also observes [AuthNavigationEvent]s from the [AuthViewModel] (when the auth graph
 * is active) to handle navigation actions triggered from within the authentication flow,
 * such as navigating to OTP verification, profile completion, or the main application graph
 * upon successful authentication.
 *
 * @param navController The [NavHostController] that manages app navigation.
 *                      Defaults to a remembered NavController instance.
 * @param isAuthenticated A boolean flag indicating whether the user is currently authenticated.
 *                        This determines the initial graph shown (main app or auth).
 * @param isEntryComplete A boolean flag indicating whether the initial user setup/onboarding
 *                         is complete.
 * @param onAuthSuccess A callback lambda that is invoked when the authentication process
 *                      completes successfully.
 * @param onLogout A callback lambda that is invoked when the user explicitly logs out from
 *                 within the [AppGraphRoutes.MAIN_APP_GRAPH_ROUTE]. This allows for cleanup
 *                 or state updates at a higher level before navigating back to the auth graph.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    isAuthenticated: Boolean,
    isEntryComplete: Boolean, // TODO: Utilize isEntryComplete on profile completion (may not need at all if isNewUser)
    onAuthSuccess: () -> Unit,
    onLogout: () -> Unit,
) {
    // Determine the starting top-level navigation graph based on authentication state.
    val topLevelStartDestination = if (isAuthenticated) {
        AppGraphRoutes.MAIN_APP_GRAPH_ROUTE
    } else {
        AppGraphRoutes.AUTH_GRAPH_ROUTE
    }
    
    // Observe the current back stack entry to react to navigation changes.
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    
    val authGraphBackStackEntry: NavBackStackEntry? = remember(currentBackStackEntry) {
        // Check if the current destination or its parent graph is the auth graph.
        val isAuthGraphActive =
            currentBackStackEntry?.destination?.parent?.route == AppGraphRoutes.AUTH_GRAPH_ROUTE ||
                    currentBackStackEntry?.destination?.route == AppGraphRoutes.AUTH_GRAPH_ROUTE
        
        if (isAuthGraphActive) {
            try {
                // Attempt to get the back stack entry for the auth graph.
                // This allows sharing a ViewModel instance across all screens within this graph.
                navController.getBackStackEntry(AppGraphRoutes.AUTH_GRAPH_ROUTE)
            } catch (_: IllegalArgumentException) {
                Firelog.w("AppNavigation: Auth graph back stack entry not found despite checks.")
                null
            }
        } else {
            null // Auth graph is not active or not on the back stack.
        }
    }
    
    // Obtain an instance of AuthViewModel, scoped to the authGraphBackStackEntry.
    // This ViewModel will only be active and retained as long as the auth graph is on the back stack.
    val authViewModel: AuthViewModel? = if (authGraphBackStackEntry != null) {
        hiltViewModel(authGraphBackStackEntry)
    } else {
        null
    }
    
    // If the AuthViewModel is available observe its navigation events.
    authViewModel?.let { vm ->
        LaunchedEffect(Unit) {
            authViewModel.authNavigationEvents.collect { event ->
                when (event) {
                    is AuthNavigationEvent.NavigateToPickCountry -> {
                        navController.navigate(AuthScreen.PickCountry.route)
                    }
                    
                    is AuthNavigationEvent.ToOTPVerifiedScreen -> {
                        val currentPhoneNumber =
                            authViewModel.currentPhoneNumberForVerification.value
                        if (currentPhoneNumber != null) {
                            navController.navigate(
                                AuthScreen.VerifyOTP.createRoute(
                                    phoneNumber = currentPhoneNumber,
                                    authAction = event.originatingAction
                                )
                            ) {
                                // Pop up to EnterPhoneNumber to prevent going back to it after OTP.
                                popUpTo(AuthScreen.EnterPhoneNumber.route)
                            }
                        } else {
                            Firelog.e("Cannot navigate to VerifyOTP, phone number missing in ViewModel.")
                        }
                    }
                    
                    is AuthNavigationEvent.ToProfileCompletion -> {
                        navController.navigate(AuthScreen.ProfileSetupName.route) {
                            popUpTo(AuthScreen.VerifyOTP.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    
                    is AuthNavigationEvent.ToMainApp -> {
                        onAuthSuccess() // Notify that authentication was successful.
                        navController.navigate(AppGraphRoutes.MAIN_APP_GRAPH_ROUTE) {
                            popUpTo(AppGraphRoutes.AUTH_GRAPH_ROUTE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    
                }
            }
        }
    }
    
    // Setup the NavHost with the determined start destination and defined navigation graphs.
    NavHost(
        navController = navController,
        startDestination = topLevelStartDestination
    ) {
        // Defines the nested navigation graph for authentication flow.
        navigation(
            startDestination = AuthScreen.Entry.route, // The first screen within the auth graph
            route = AppGraphRoutes.AUTH_GRAPH_ROUTE
        ) {
            authGraph(
                navController = navController
            )
        }
        
        // Defines the nested navigation graph for the main application content.
        navigation(
            startDestination = FeedScreen.FeedOverview.route, // The first screen within the main app graph
            route = AppGraphRoutes.MAIN_APP_GRAPH_ROUTE
        ) {
            mainAppGraph(
                navController = navController,
                onLogout = {
                    onLogout()
                    // Navigate back to the authentication graph, clearing the main app graph.
                    navController.navigate(AppGraphRoutes.AUTH_GRAPH_ROUTE) {
                        popUpTo(AppGraphRoutes.MAIN_APP_GRAPH_ROUTE) { inclusive = true }
                        launchSingleTop = true // Ensure auth graph is a single top instance
                    }
                }
            )
        }
    }
}