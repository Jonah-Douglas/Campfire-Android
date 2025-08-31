package com.example.campfire.onboarding.profile_setup.presentation.navigation

import androidx.compose.material3.Text
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.campfire.core.common.logging.Firelog


fun NavGraphBuilder.profileSetupNavGraph(
    navController: NavHostController,
    onNavigateToAppSetup: () -> Unit
) {
    
    composable(ProfileSetupScreen.ProfileSetupIntro.route) {
        // JD TODO: Implement
        Firelog.i("Navigating to ProfileSetupIntro (Placeholder)")
        Text("Profile Setup Name Screen Placeholder (Implement Me)")
    }
    
    composable(ProfileSetupScreen.ProfileSetupName.route) {
        // JD TODO: Implement
        Firelog.i("Navigating to ProfileSetupNameScreen (Placeholder)")
        Text("Profile Setup Name Screen Placeholder (Implement Me)")
    }
    
    composable(ProfileSetupScreen.ProfileSetupEmail.route) {
        // JD TODO: Implement
        Firelog.i("Navigating to ProfileSetupEmailScreen (Placeholder)")
        Text("Profile Setup Email Screen Placeholder (Implement Me)")
    }
    
    composable(ProfileSetupScreen.ProfileSetupDob.route) {
        // JD TODO: Implement
        Firelog.i("Navigating to ProfileSetupDobScreen (Placeholder)")
        Text("Profile Setup Dob Screen Placeholder (Implement Me)")
    }
    
    composable(ProfileSetupScreen.ProfileSetupNotifs.route) {
        // JD TODO: Implement
        Firelog.i("Navigating to ProfileSetupNotifsScreen (Placeholder)")
        Text("Profile Setup Notifs Screen Placeholder (Implement Me)")
    }
}