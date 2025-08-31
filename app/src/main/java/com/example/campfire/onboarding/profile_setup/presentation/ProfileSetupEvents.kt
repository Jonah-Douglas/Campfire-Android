package com.example.campfire.onboarding.profile_setup.presentation


sealed class ProfileSetupNavigationEvent {
    object ToAppSetup : ProfileSetupNavigationEvent()
    object ToFeedsScreen : ProfileSetupNavigationEvent()
}