package com.example.campfire.core.presentation.navigation


/**
 * Defines unique string constants representing the routes for the major
 * navigation graphs within the application.
 *
 * These constants are used by the Navigation Compose library to identify and navigate
 * between different nested navigation graphs, such as the authentication flow
 * and the main application content after login.
 */
object AppGraphRoutes {
    const val AUTH_GRAPH_ROUTE = "auth_graph"
    const val MAIN_APP_GRAPH_ROUTE = "main_app_graph"
}