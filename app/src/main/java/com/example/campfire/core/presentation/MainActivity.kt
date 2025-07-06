package com.example.campfire.core.presentation

import android.os.Bundle
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
import com.example.campfire.core.presentation.navigation.AppNavigation
import com.example.campfire.core.presentation.utils.CampfireTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val mainViewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        splashScreen.setKeepOnScreenCondition { !mainViewModel.isDataReady.value }
        
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val fadeOut = android.view.animation.AlphaAnimation(1f, 0f).apply {
                duration = 300L
                fillAfter = true
            }
            splashScreenViewProvider.view.startAnimation(fadeOut)
            splashScreenViewProvider.remove()
        }
        
        setContent {
            CampfireTheme {
                val isDataReady by mainViewModel.isDataReady.collectAsState()
                val isAuthenticated by mainViewModel.authState.collectAsState()
                val isEntryComplete by mainViewModel.isEntryComplete.collectAsState()
                
                if (isDataReady) {
                    AppNavigation(
                        isAuthenticated = isAuthenticated,
                        isEntryComplete = isEntryComplete,
                        onAuthSuccess = { mainViewModel.userLoggedIn() },
                        onLogout = { mainViewModel.userLoggedOut() },
                        onEntryScreenComplete = { mainViewModel.entryScreenCompleted() }
                    )
                } else {
                    // Your placeholder while system splash is waiting for isDataReady
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {}
                }
            }
        }
    }
}