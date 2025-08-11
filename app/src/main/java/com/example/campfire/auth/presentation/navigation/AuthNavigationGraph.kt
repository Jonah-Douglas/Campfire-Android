package com.example.campfire.auth.presentation.navigation

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.campfire.auth.presentation.AuthViewModel
import com.example.campfire.auth.presentation.screens.EnterPhoneNumberScreen
import com.example.campfire.auth.presentation.screens.EntryScreen
import com.example.campfire.auth.presentation.screens.VerifyOTPScreen


fun NavGraphBuilder.authGraph(
    navController: NavHostController,
) {
    composable(AuthScreen.Entry.route) {
        EntryScreen(
            onNavigateToEnterPhoneNumber = { authAction ->
                navController.navigate(AuthScreen.EnterPhoneNumber.createRoute(authAction)) { }
            }
        )
    }
    
    composable(
        route = AuthScreen.EnterPhoneNumber.route,
        arguments = listOf(
            navArgument(AuthScreen.Args.AUTH_ACTION) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val authActionString = backStackEntry.arguments?.getString(AuthScreen.Args.AUTH_ACTION)
        val authAction = authActionString?.let { AuthAction.valueOf(it) } ?: AuthAction.LOGIN
        val authViewModel: AuthViewModel =
            hiltViewModel(backStackEntry)
        
        val selectedRegionCode by backStackEntry.savedStateHandle
            .getStateFlow<String?>("selected_region_code", null)
            .collectAsState()
        
        LaunchedEffect(selectedRegionCode) {
            selectedRegionCode?.let { region ->
                authViewModel.onRegionSelected(region)
                backStackEntry.savedStateHandle.remove<String>("selected_region_code")
            }
        }
        
        EnterPhoneNumberScreen(
            authAction = authAction,
            viewModel = authViewModel,
            onNavigateToVerifyOTP = { phoneNumber, action ->
                navController.navigate(AuthScreen.VerifyOTP.createRoute(phoneNumber, action))
            },
            onNavigateBack = { navController.popBackStack() },
            onShowCountryPicker = { navController.navigate(AuthScreen.PickCountry.route) }
        )
    }
    
    composable(
        route = AuthScreen.PickCountry.route,
    ) {
        Text("Country Picker Screen (Implement Me)")
    }
    
    composable(
        route = AuthScreen.VerifyOTP.route,
        arguments = listOf(
            navArgument(AuthScreen.Args.PHONE_NUMBER) { type = NavType.StringType },
            navArgument(AuthScreen.Args.AUTH_ACTION) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val phoneNumber = backStackEntry.arguments?.getString(AuthScreen.Args.PHONE_NUMBER)
        if (phoneNumber == null || phoneNumber.isBlank()) {
            Log.e("AuthGraph", "VerifyOTPScreen launched without a valid phone number.")
            navController.popBackStack()
            return@composable
        }
        
        val authActionString = backStackEntry.arguments?.getString(AuthScreen.Args.AUTH_ACTION)
        val authAction =
            authActionString?.let { AuthAction.valueOf(it) } ?: AuthAction.LOGIN // Default
        
        VerifyOTPScreen(
            phoneNumberFromNav = phoneNumber,
            authActionFromNav = authAction,
            onNavigateBack = { navController.popBackStack() }
        )
    }
    
    composable(AuthScreen.ProfileSetupName.route) {
        // Replace with your actual ProfileSetupNameScreen
        Text("Profile Setup Name Screen Placeholder (Implement Me)")
    }
    
    composable(AuthScreen.ProfileSetupEmail.route) {
        // Replace with your actual ProfileSetupEmailScreen
        Text("Profile Setup Email Screen Placeholder (Implement Me)")
    }
    
    composable(AuthScreen.ProfileSetupDob.route) {
        // Replace with your actual ProfileSetupDobScreen
        Text("Profile Setup Dob Screen Placeholder (Implement Me)")
    }
    
    composable(AuthScreen.ProfileSetupNotifs.route) {
        // Replace with your actual ProfileSetupNotifsScreen
        Text("Profile Setup Notifs Screen Placeholder (Implement Me)")
    }
}