package com.example.campfire.settings.presentation.navigation

import com.example.campfire.core.presentation.navigation.NavigationDestination


sealed class SettingsScreen(override val route: String) : NavigationDestination {
    object SettingsOverview : SettingsScreen("settings_overview")
}