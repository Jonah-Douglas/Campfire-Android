package com.example.campfire.profile.presentation.navigation

import com.example.campfire.core.presentation.navigation.NavigationDestination


sealed class ProfileScreen(override val route: String) : NavigationDestination {
    object ProfileOverview : ProfileScreen("profile_overview")
    object EditProfile : ProfileScreen("profile_edit")
}