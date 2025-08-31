package com.example.campfire.onboarding.app_setup.presentation.navigation

import androidx.compose.material3.Text
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.campfire.core.common.logging.Firelog


fun NavGraphBuilder.appSetupNavGraph(
    navController: NavHostController,
    onNavigateToFeed: () -> Unit
) {
    
    composable(AppSetupScreen.AppSetupIntro.route) {
        // JD TODO: Implement
        Firelog.i("Navigating to ProfileSetupIntro (Placeholder)")
        Text("Profile Setup Name Screen Placeholder (Implement Me)")
    }
}