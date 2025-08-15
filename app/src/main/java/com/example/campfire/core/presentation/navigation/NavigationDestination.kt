package com.example.campfire.core.presentation.navigation


/**
 * Represents a distinct destination within the application's navigation structure.
 *
 * This interface provides a contract for all navigable screens or features, ensuring
 * they can expose a unique `route` string. This `route` is fundamental for the
 * Navigation Compose library to identify and navigate to specific destinations.
 *
 */
interface NavigationDestination {
    val route: String
}