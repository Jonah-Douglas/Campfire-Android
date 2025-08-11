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
        // FeedOverviewScreen(navController = navController, onLogout = onLogout /* if needed directly */)
        Text("Feed Overview Screen (from mainAppGraph)")
        Button(onClick = { navController.navigate(ProfileScreen.ProfileOverview.route) }) {
            Text("Go to Profile")
        }
        Button(onClick = onLogout) { Text("Logout") }
    }
    composable(FeedScreen.PostDetail.route) {
        // val postId = it.arguments?.getString("postId")
        // Replace with your actual PostDetailScreen(postId = postId)
        Text("Post Detail Screen Placeholder")
    }
    
    // --- Profile Feature Screens ---
    composable(ProfileScreen.ProfileOverview.route) {
        // Replace with your actual ProfileOverviewScreen
        Text("Profile Overview Screen (from mainAppGraph)")
        Button(onClick = { navController.navigate(SettingsScreen.SettingsOverview.route) }) {
            Text("Go to Settings")
        }
    }
    composable(ProfileScreen.EditProfile.route) {
        // Replace with your actual EditProfileScreen
        Text("Edit Profile Screen Placeholder")
    }
    
    // --- Settings Feature Screens ---
    composable(SettingsScreen.SettingsOverview.route) {
        // Replace with your actual SettingsOverviewScreen
        Text("Settings Overview Screen (from mainAppGraph)")
    }
    
    // If you had more complex features, you might call their specific graph builders here:
    // e.g., searchGraph(navController)
}