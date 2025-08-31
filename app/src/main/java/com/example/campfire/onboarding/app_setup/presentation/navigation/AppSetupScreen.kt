package com.example.campfire.onboarding.app_setup.presentation.navigation

import com.example.campfire.core.presentation.navigation.NavigationDestination


sealed class AppSetupScreen(
    override val route: String,
) : NavigationDestination {
    
    data object AppSetupIntro : AppSetupScreen(APP_SETUP_INTRO_ROUTE)
    
    companion object Args {
        
        // Routes
        private const val APP_SETUP_INTRO_ROUTE = "app_setup_intro"
    }
}