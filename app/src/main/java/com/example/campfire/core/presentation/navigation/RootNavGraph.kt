package com.example.campfire.core.presentation.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import com.example.campfire.auth.presentation.navigation.AuthScreen
import com.example.campfire.auth.presentation.navigation.authGraph
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.presentation.model.AppState
import com.example.campfire.feed.presentation.navigation.FeedScreen
import com.example.campfire.feed.presentation.navigation.feedNavGraph
import com.example.campfire.onboarding.app_setup.presentation.navigation.AppSetupScreen
import com.example.campfire.onboarding.app_setup.presentation.navigation.appSetupNavGraph
import com.example.campfire.onboarding.profile_setup.presentation.navigation.ProfileSetupScreen
import com.example.campfire.onboarding.profile_setup.presentation.navigation.profileSetupNavGraph


/**
 * The main navigation Composable for the application.
 *
 * Sets up the top-level navigation structure using a [NavHost] and determines the initial
 * navigation graph based on the user's authentication and onboarding completion status
 * provided via [appState].
 *
 * It orchestrates navigation between major features: Authentication, Profile Setup (Onboarding),
 * App Setup (Onboarding), and the Main Application Feed. It receives callbacks for user-initiated
 * logout and for when specific onboarding steps are completed, which are then propagated
 * to the appropriate components or handled as needed.
 *
 * @param navController The [NavHostController] that manages app navigation. This is typically
 *                      created and remembered in the calling Composable (e.g., `MainActivity`).
 * @param appState Represents the overall state of the application, including authentication
 *                 and onboarding completion status. This is derived from `GlobalStateViewModel`
 *                 and passed in by `MainActivity`.
 * @param onUserInitiatedLogout A callback lambda invoked when the user explicitly initiates a logout
 *                              from within an authenticated part of the app (e.g., settings screen).
 *                              This callback is responsible for updating global state and navigating
 *                              to the authentication flow.
 * @param onProfileSetupCompleted A callback lambda to be invoked when the core profile setup
 *                               onboarding step is successfully completed by the user.
 * @param onAppSetupCompleted A callback lambda to be invoked when the app-specific setup
 *                            onboarding step is successfully completed by the user.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootNavGraph(
    navController: NavHostController,
    appState: AppState,
    onUserInitiatedLogout: () -> Unit,
    onProfileSetupCompleted: () -> Unit,
    onAppSetupCompleted: () -> Unit
) {
    val startDestination = determineStartDestination(appState)
    Firelog.d("Composing. Current AppState: $appState, Determined StartDestination: $startDestination")
    
    var currentNavHostKey by remember { mutableStateOf(startDestination) }
    LaunchedEffect(startDestination) {
        if (currentNavHostKey != startDestination) {
            Firelog.i("RootNavGraph: Start destination changed from '$currentNavHostKey' to '$startDestination'. NavHost will recompose with new start.")
            currentNavHostKey = startDestination
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // --- Authentication Feature ---
        navigation(
            startDestination = AuthScreen.Entry.route,
            route = AppGraphRoutes.AUTH_FEATURE_ROUTE
        ) {
            authGraph(
                navController = navController
            )
        }
        
        // --- Onboarding: Profile Setup Feature ---
        navigation(
            startDestination = ProfileSetupScreen.ProfileSetupIntro.route,
            route = AppGraphRoutes.PROFILE_SETUP_FEATURE_ROUTE
        ) {
            
            profileSetupNavGraph(
                navController = navController,
                onNavigateToAppSetup = {
                    Firelog.d("RootNavGraph: Navigating from Profile Setup to App Setup.")
                    onProfileSetupCompleted()
                    navController.navigate(AppGraphRoutes.APP_SETUP_FEATURE_ROUTE) {
                        popUpTo(AppGraphRoutes.PROFILE_SETUP_FEATURE_ROUTE) { inclusive = true }
                    }
                }
            )
        }
        
        // --- Onboarding: App Setup Feature ---
        navigation(
            startDestination = AppSetupScreen.AppSetupIntro.route,
            route = AppGraphRoutes.APP_SETUP_FEATURE_ROUTE
        ) {
            appSetupNavGraph(
                navController = navController,
                onNavigateToFeed = {
                    Firelog.d("Navigating from App Setup to Feed.")
                    onAppSetupCompleted()
                    navController.navigate(AppGraphRoutes.FEED_FEATURE_ROUTE) {
                        popUpTo(AppGraphRoutes.APP_SETUP_FEATURE_ROUTE) { inclusive = true }
                    }
                }
            )
        }
        
        // --- Feed Feature (main app landing screen) ---
        navigation(
            startDestination = FeedScreen.FeedOverview.route,
            route = AppGraphRoutes.FEED_FEATURE_ROUTE
        ) {
            feedNavGraph(
                navController = navController,
                onLogout = {
                    Firelog.d("RootNavGraph: Logout initiated from mainAppGraph.")
                    onUserInitiatedLogout()
                }
            )
        }
    }
}

/**
 * Determines the appropriate starting feature route based on the application state.
 * This function is critical for directing the user to the correct part of the app
 * upon launch or after significant state changes (like login/logout).
 *
 * @param appState The current state of the application.
 * @return The route string of the feature graph that should be the starting point.
 */
fun determineStartDestination(appState: AppState): String {
    return if (!appState.isAuthenticated) {
        AppGraphRoutes.AUTH_FEATURE_ROUTE
    } else if (!appState.isProfileSetupComplete) {
        AppGraphRoutes.PROFILE_SETUP_FEATURE_ROUTE
    } else if (!appState.isAppSetupComplete) {
        AppGraphRoutes.APP_SETUP_FEATURE_ROUTE
    } else {
        AppGraphRoutes.FEED_FEATURE_ROUTE
    }
}