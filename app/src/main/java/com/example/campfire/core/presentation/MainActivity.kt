package com.example.campfire.core.presentation

import android.os.Bundle
import android.view.animation.AlphaAnimation
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.presentation.model.AppState
import com.example.campfire.core.presentation.navigation.AppGraphRoutes
import com.example.campfire.core.presentation.navigation.RootNavGraph
import com.example.campfire.core.presentation.utils.CampfireTheme
import dagger.hilt.android.AndroidEntryPoint


/**
 * The main and only Activity in this Android application, serving as the entry point
 * and primary container for the UI.
 *
 * This Activity is responsible for:
 * 1.  Setting up core window properties (e.g., drawing behind system bars).
 * 2.  Managing the application's splash screen, keeping it visible until essential
 *     data is loaded (controlled by [GlobalStateViewModel.isDataReady]).
 * 3.  Setting the main content view using Jetpack Compose via [setContent].
 * 4.  Applying the application-wide [CampfireTheme].
 * 5.  Observing application-level state from [GlobalStateViewModel] (e.g., data readiness,
 *     authentication state, onboarding completion statuses).
 * 6.  Constructing the [AppState] object required by [RootNavGraph].
 * 7.  Initializing and providing the [RootNavGraph] Composable with the necessary
 *     [NavHostController], [AppState], and callbacks (e.g., for logout, onboarding step completion).
 * 8.  Handling global navigation events triggered by [GlobalStateViewModel] (e.g., session invalidation
 *     requiring navigation to authentication) via the [HandleNavigationEvents] Composable.
 * 9.  Displaying a placeholder UI while waiting for `isDataReady` to become true.
 *
 * It uses Hilt for dependency injection ([AndroidEntryPoint]).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    // ViewModel responsible for holding and managing UI-related data for MainActivity.
    private val globalStateViewModel: GlobalStateViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the splash screen
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Keep the splash screen visible until initial state is loaded
        splashScreen.setKeepOnScreenCondition {
            val isDataReady = globalStateViewModel.isDataReady.value
            Firelog.v("SplashScreen - isDataReady: $isDataReady")
            !isDataReady
        }
        
        // Custom splash screen exit animation
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val fadeOut = AlphaAnimation(1f, 0f).apply {
                duration = 300L
                fillAfter = true
            }
            splashScreenViewProvider.view.startAnimation(fadeOut)
            splashScreenViewProvider.remove()
        }
        
        setContent {
            CampfireTheme {
                val isDataReady by globalStateViewModel.isDataReady.collectAsState()
                val navController = rememberNavController()
                
                HandleNavigationEvents(
                    navController = navController,
                    viewModel = globalStateViewModel
                )
                
                if (isDataReady) {
                    Firelog.d("Data is ready. Composing RootNavGraph.")
                    val isAuthenticatedNullable by globalStateViewModel.authState.collectAsState()
                    val isProfileSetupCompleteNullable by globalStateViewModel.isProfileSetupComplete.collectAsState()
                    val isAppSetupCompleteNullable by globalStateViewModel.isAppSetupComplete.collectAsState()
                    
                    val appState = AppState(
                        isAuthenticated = isAuthenticatedNullable ?: false,
                        isProfileSetupComplete = isProfileSetupCompleteNullable ?: false,
                        isAppSetupComplete = isAppSetupCompleteNullable ?: false
                    )
                    Firelog.v("Current AppState for RootNavGraph: $appState")
                    
                    RootNavGraph(
                        navController = navController,
                        appState = appState,
                        onUserInitiatedLogout = {
                            globalStateViewModel.userLoggedOut()
                            navController.navigate(AppGraphRoutes.AUTH_FEATURE_ROUTE) {}
                        },
                        onProfileSetupCompleted = {
                            Firelog.i("MainActivity: Core Profile Setup reported as complete.")
                        },
                        onAppSetupCompleted = {
                            Firelog.i("MainActivity: App Setup reported as complete.")
                        }
                    )
                } else {
                    // Placeholder while system splash is waiting for isDataReady
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {}
                }
            }
        }
    }
}

/**
 * A private Composable responsible for observing and handling global navigation events
 * emitted by the [GlobalStateViewModel]. This is primarily used for scenarios like
 * automatic redirection to the authentication flow upon session invalidation.
 *
 * @param navController The [NavHostController] used for performing navigation.
 * @param viewModel The [GlobalStateViewModel] that emits navigation trigger events.
 */
@Composable
private fun HandleNavigationEvents(
    navController: NavHostController,
    viewModel: GlobalStateViewModel
) {
    LaunchedEffect(key1 = Unit) {
        viewModel.triggerNavigateToAuth.collect {
            Firelog.i("HandleNavigationEvents - Received triggerNavigateToAuth. Navigating to AUTH_FEATURE_ROUTE.")
            navController.navigate(AppGraphRoutes.AUTH_FEATURE_ROUTE) {
                // Clear the entire back stack and make AUTH_FEATURE_ROUTE the new root
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
                launchSingleTop = true // Avoid multiple copies of the auth graph
            }
        }
    }
}