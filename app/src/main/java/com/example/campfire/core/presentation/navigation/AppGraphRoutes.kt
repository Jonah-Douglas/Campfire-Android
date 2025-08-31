package com.example.campfire.core.presentation.navigation


/**
 * Defines unique string constants representing the routes for the major
 * navigation graphs or top-level features within the application.
 *
 * These constants are used by the Navigation Compose library to identify and navigate
 * between different high-level sections of the app, such as authentication,
 * the multi-phase onboarding process, and the main application content after setup.
 */
object AppGraphRoutes {
    
    /**
     * Route for the Authentication feature graph.
     * This graph handles user login and registration (e.g., phone number input, OTP verification).
     */
    const val AUTH_FEATURE_ROUTE = "auth_feature"
    
    /**
     * Route for the Core Profile Setup phase of the onboarding process.
     * This graph handles collecting essential user details like name, email, DOB.
     */
    const val PROFILE_SETUP_FEATURE_ROUTE = "profile_setup_feature"
    
    /**
     * Route for the App/Feature Setup phase of the onboarding process.
     * This graph handles guiding the user through initial app settings and feature configurations.
     */
    const val APP_SETUP_FEATURE_ROUTE = "app_setup_feature"
    
    /**
     * Route for the Feed feature graph (main landing page), accessible after authentication and
     * onboarding completion.
     */
    const val FEED_FEATURE_ROUTE = "feed_feature"
}