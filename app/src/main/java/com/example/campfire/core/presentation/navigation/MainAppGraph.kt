package com.example.campfire.core.presentation.navigation

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.campfire.feed.presentation.navigation.FeedScreen
import com.example.campfire.profile.presentation.navigation.ProfileScreen
import com.example.campfire.settings.presentation.navigation.SettingsScreen


/**
 * Builder for the main authenticated application graph.
 * This graph will host other features like Feed, Profile, Settings.
 */
fun NavGraphBuilder.mainAppGraph(
    navController: NavHostController,
    onLogout: () -> Unit
) {
    // --- Feed Feature Screens ---
    composable(FeedScreen.FeedOverview.route) {
        Text("Feed Overview Screen (from mainAppGraph)")
        Button(onClick = { navController.navigate(ProfileScreen.ProfileOverview.route) }) {
            Text("Go to Profile")
        }
        Button(onClick = onLogout) { Text("Logout") }
    }
    composable(FeedScreen.PostDetail.route) {
        Text("Post Detail Screen Placeholder")
    }
    
    // --- Profile Feature Screens ---
    composable(ProfileScreen.ProfileOverview.route) {
        Text("Profile Overview Screen (from mainAppGraph)")
        Button(onClick = { navController.navigate(SettingsScreen.SettingsOverview.route) }) {
            Text("Go to Settings")
        }
    }
    composable(ProfileScreen.EditProfile.route) {
        Text("Edit Profile Screen Placeholder")
    }
    
    // --- Settings Feature Screens ---
    composable(SettingsScreen.SettingsOverview.route) {
        Text("Settings Overview Screen (from mainAppGraph)")
    }
}