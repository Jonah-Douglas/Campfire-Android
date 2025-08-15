package com.example.campfire.core.presentation

import android.os.Bundle
import android.view.animation.AlphaAnimation
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.campfire.core.presentation.navigation.AppNavigation
import com.example.campfire.core.presentation.utils.CampfireTheme
import dagger.hilt.android.AndroidEntryPoint


/**
 * The main and only Activity in this Android application, serving as the entry point
 * and primary container for the UI.
 *
 * This Activity is responsible for:
 * 1.  Setting up the core window properties, such as drawing behind system bars using [WindowCompat].
 * 2.  Installing and managing the application's splash screen using [androidx.core.splashscreen.SplashScreen.installSplashScreen],
 *     keeping it on screen until essential data is ready (controlled by [MainViewModel.isDataReady]).
 * 3.  Defining a custom exit animation for the splash screen.
 * 4.  Setting the main content view using Jetpack Compose via [setContent].
 * 5.  Applying the application-wide [CampfireTheme].
 * 6.  Observing application-level state from [MainViewModel] (e.g., data readiness,
 *     authentication state, entry completion status).
 * 7.  Initializing and providing the [AppNavigation] Composable with the necessary state
 *     and callbacks to manage the application's navigation flow.
 * 8.  Displaying a placeholder UI (a simple Surface) while the splash screen is active and
 *     waiting for `isDataReady` to become true.
 *
 * It uses Hilt for dependency injection, as indicated by the [AndroidEntryPoint] annotation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    // ViewModel responsible for holding and managing UI-related data for MainActivity.
    private val mainViewModel: MainViewModel by viewModels()
    
    /**
     * Called when the activity is first created. This is where most initialization should go:
     * calling `setContentView(int)` to inflate the activity's UI, using `findViewById(int)`
     * to programmatically interact with widgets in the UI, calling
     * `managedQuery(android.net.Uri, String[], String, String[], String)` to retrieve
     * cursors for data being displayed, etc.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down then this Bundle contains the data it most
     *                           recently supplied in `onSaveInstanceState(Bundle)`.
     *                           <b><i>Note: Otherwise it is null.</i></b>
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the splash screen. Must be called before super.onCreate() or setContentView().
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Keep the splash screen visible until initial state is loaded
        splashScreen.setKeepOnScreenCondition { !mainViewModel.isDataReady.value }
        
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
            CampfireTheme { // Apply the custom application theme
                val isDataReady by mainViewModel.isDataReady.collectAsState()
                val isAuthenticated by mainViewModel.authState.collectAsState()
                val navController = rememberNavController()
                val isEntryComplete by mainViewModel.isEntryComplete.collectAsState()
                
                if (isDataReady) {
                    AppNavigation(
                        isAuthenticated = isAuthenticated,
                        onAuthSuccess = { mainViewModel.userLoggedIn() },
                        onLogout = { mainViewModel.userLoggedOut() },
                        navController = navController,
                        isEntryComplete = isEntryComplete
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