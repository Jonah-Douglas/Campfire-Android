package com.example.campfire.onboarding.profile_setup.presentation.navigation

import com.example.campfire.core.presentation.navigation.NavigationDestination


sealed class ProfileSetupScreen(
    override val route: String,
) : NavigationDestination {
    
    data object ProfileSetupIntro : ProfileSetupScreen(PROFILE_SETUP_INTRO_ROUTE)
    
    data object ProfileSetupName : ProfileSetupScreen(PROFILE_SETUP_NAME_ROUTE)
    
    data object ProfileSetupEmail : ProfileSetupScreen(PROFILE_SETUP_EMAIL_ROUTE)
    
    data object ProfileSetupDob : ProfileSetupScreen(PROFILE_SETUP_DOB_ROUTE)
    
    data object ProfileSetupNotifs : ProfileSetupScreen(PROFILE_SETUP_NOTIFS_ROUTE)
    
    companion object Args {
        
        // Routes
        private const val PROFILE_SETUP_INTRO_ROUTE = "profile_setup_intro"
        private const val PROFILE_SETUP_NAME_ROUTE = "profile_setup_name"
        private const val PROFILE_SETUP_EMAIL_ROUTE = "profile_setup_email"
        private const val PROFILE_SETUP_DOB_ROUTE = "profile_setup_dob"
        private const val PROFILE_SETUP_NOTIFS_ROUTE = "profile_setup_notifs"
    }
}