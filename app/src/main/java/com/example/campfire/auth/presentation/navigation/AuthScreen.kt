package com.example.campfire.auth.presentation.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.campfire.core.presentation.navigation.NavigationDestination


enum class AuthAction {
    LOGIN,
    REGISTER
}

/**
 * Defines the screen destinations within the authentication feature graph.
 */
@Suppress("unused")
sealed class AuthScreen(
    override val route: String,
    val arguments: List<NamedNavArgument> = emptyList()
) : NavigationDestination {
    
    data object Entry : AuthScreen("auth_entry")
    
    data object EnterPhoneNumber : AuthScreen(
        route = "${ENTER_PHONE_NUMBER_ROUTE}/{${AUTH_ACTION}}",
        arguments = listOf(navArgument(AUTH_ACTION) { type = NavType.StringType })
    ) {
        fun createRoute(authAction: AuthAction): String =
            "${ENTER_PHONE_NUMBER_ROUTE}/${authAction.name}"
    }
    
    data object PickCountry : AuthScreen(
        route = PICK_COUNTRY_ROUTE
    )
    
    data object VerifyOTP : AuthScreen(
        route = "${VERIFY_OTP}/{${PHONE_NUMBER}}/{${AUTH_ACTION}}",
        arguments = listOf(
            navArgument(PHONE_NUMBER) { type = NavType.StringType },
            navArgument(AUTH_ACTION) { type = NavType.StringType }
        )
    ) {
        fun createRoute(phoneNumber: String, authAction: AuthAction): String =
            "${VERIFY_OTP}/$phoneNumber/${authAction.name}"
    }
    
    data object ProfileSetupName : AuthScreen(PROFILE_SETUP_NAME_ROUTE)
    
    data object ProfileSetupEmail : AuthScreen(PROFILE_SETUP_EMAIL_ROUTE)
    
    data object ProfileSetupDob : AuthScreen(PROFILE_SETUP_DOB_ROUTE)
    
    data object ProfileSetupNotifs : AuthScreen(PROFILE_SETUP_NOTIFS_ROUTE)
    
    companion object Args {
        const val AUTH_ACTION = "authAction"
        const val PHONE_NUMBER = "phoneNumber"
        const val SELECTED_REGION_CODE = "selected_region_code"
        
        // Routes
        private const val ENTER_PHONE_NUMBER_ROUTE = "enter_phone_number"
        private const val PICK_COUNTRY_ROUTE = "pick_country"
        private const val VERIFY_OTP = "verify_otp"
        private const val PROFILE_SETUP_NAME_ROUTE = "profile_setup_name"
        private const val PROFILE_SETUP_EMAIL_ROUTE = "profile_setup_email"
        private const val PROFILE_SETUP_DOB_ROUTE = "profile_setup_dob"
        private const val PROFILE_SETUP_NOTIFS_ROUTE = "profile_setup_notifs"
    }
}