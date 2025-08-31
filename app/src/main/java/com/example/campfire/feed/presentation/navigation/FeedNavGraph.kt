package com.example.campfire.feed.presentation.navigation

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.campfire.profile.presentation.navigation.ProfileScreen
import com.example.campfire.settings.presentation.navigation.SettingsScreen


/**
 * An extension function on [NavGraphBuilder] that defines the routes and destinations
 * for the main application graph. This graph is intended for users who are authenticated
 * and have access to the core features of the application.
 *
 * It encapsulates feature-specific screens such as those related to the Feed, Profile,
 * and Settings. Each feature's screens are defined as individual `composable` destinations
 * within this graph.
 *
 * This function is typically called within a `NavHost`'s `navigation` block that uses
 * [com.example.campfire.core.presentation.navigation.AppGraphRoutes.FEED_FEATURE_ROUTE] as its route.
 *
 * @param navController The [NavHostController] used for navigating between screens
 *                      within this graph and potentially to other graphs.
 * @param onLogout A callback lambda that is invoked when a logout action is triggered
 *                 from within this graph (e.g., from a settings or profile screen).
 *                 This allows the parent navigator (e.g., [AppNavigation]) to handle
 *                 the global logout logic, such as clearing session data and navigating
 *                 back to the authentication graph.
 */
fun NavGraphBuilder.feedNavGraph(
    navController: NavHostController,
    onLogout: () -> Unit
) {
    // --- Feed Feature Screens ---
    // Defines the routes and composable content for screens related to the application's feed.
    
    /**
     * Destination for the Feed Overview screen.
     * This is typically the main landing screen for the feed feature, displaying a list or grid
     * of posts or items.
     *
     * Current placeholder includes navigation to Profile and a Logout button.
     */
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
    // Defines the routes and composable content for screens related to user profiles.
    
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
    // Defines the routes and composable content for application settings screens.
    
    composable(SettingsScreen.SettingsOverview.route) {
        Text("Settings Overview Screen (from mainAppGraph)")
    }
}