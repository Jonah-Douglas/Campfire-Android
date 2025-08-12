package com.example.campfire.auth.presentation.navigation

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.campfire.auth.presentation.AuthViewModel
import com.example.campfire.auth.presentation.screens.EnterPhoneNumberScreen
import com.example.campfire.auth.presentation.screens.EntryScreen
import com.example.campfire.auth.presentation.screens.PickCountryScreen
import com.example.campfire.auth.presentation.screens.VerifyOTPScreen


@SuppressLint("StateFlowValueCalledInComposition")
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
        LaunchedEffect(Unit) {
            Log.d(
                "ViewModelCheck",
                "EnterPhoneNumberScreen - VM Hash: ${authViewModel.hashCode()}, Countries: ${authViewModel.sendOTPUIState.value.availableCountries.size}"
            )
        }
        
        val selectedRegionCodeResult = backStackEntry.savedStateHandle
            .get<String>(AuthScreen.Args.SELECTED_REGION_CODE)
        
        LaunchedEffect(selectedRegionCodeResult) {
            if (selectedRegionCodeResult != null) {
                authViewModel.onRegionSelected(selectedRegionCodeResult)
                backStackEntry.savedStateHandle.remove<String>(AuthScreen.Args.SELECTED_REGION_CODE)
            }
        }
        
        EnterPhoneNumberScreen(
            authAction = authAction,
            viewModel = authViewModel,
            onNavigateToVerifyOTP = { phoneNumber, action ->
                navController.navigate(AuthScreen.VerifyOTP.createRoute(phoneNumber, action))
            },
            onNavigateBack = { navController.popBackStack() },
            onNavigateToPickCountry = { navController.navigate(AuthScreen.PickCountry.route) }
        )
    }
    
    composable(route = AuthScreen.PickCountry.route) {
        val parentEntry =
            remember(it) { navController.getBackStackEntry(AuthScreen.EnterPhoneNumber.route) }
        val authViewModel: AuthViewModel = hiltViewModel(parentEntry)
        Log.d(
            "ViewModelCheck",
            "NavGraph for PickCountry - Shared VM from parentEntry. Hash: ${authViewModel.hashCode()}, Countries: ${authViewModel.sendOTPUIState.value.availableCountries.size}"
        )
        
        PickCountryScreen(
            viewModel = authViewModel,
            onCountrySelectedAndNavigateBack = { selectedRegionCode ->
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(AuthScreen.Args.SELECTED_REGION_CODE, selectedRegionCode)
                navController.popBackStack()
            },
            onNavigateBack = {
                navController.popBackStack()
            }
        )
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
        // JD TODO: Implement
        Text("Profile Setup Name Screen Placeholder (Implement Me)")
    }
    
    composable(AuthScreen.ProfileSetupEmail.route) {
        // JD TODO: Implement
        Text("Profile Setup Email Screen Placeholder (Implement Me)")
    }
    
    composable(AuthScreen.ProfileSetupDob.route) {
        // JD TODO: Implement
        Text("Profile Setup Dob Screen Placeholder (Implement Me)")
    }
    
    composable(AuthScreen.ProfileSetupNotifs.route) {
        // JD TODO: Implement
        Text("Profile Setup Notifs Screen Placeholder (Implement Me)")
    }
}