package com.example.campfire.auth.presentation.navigation

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
import com.example.campfire.core.common.logging.Firelog


fun NavGraphBuilder.authGraph(
    navController: NavHostController,
) {
    composable(AuthScreen.Entry.route) {
        Firelog.i("Navigating to EntryScreen")
        EntryScreen(
            onNavigateToEnterPhoneNumber = { authAction ->
                Firelog.i("EntryScreen: Navigating to EnterPhoneNumberScreen with action: $authAction")
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
        
        Firelog.i("Navigating to EnterPhoneNumberScreen. AuthAction: $authAction (from arg: '$authActionString')")
        
        val authViewModel: AuthViewModel = hiltViewModel(backStackEntry)
        Firelog.d("EnterPhoneNumberScreen: AuthViewModel instance hash: ${authViewModel.hashCode()}")
        
        val selectedRegionCodeResult =
            backStackEntry.savedStateHandle.get<String>(AuthScreen.Args.SELECTED_REGION_CODE)
        
        LaunchedEffect(selectedRegionCodeResult) {
            if (selectedRegionCodeResult != null) {
                Firelog.d("EnterPhoneNumberScreen: Received selectedRegionCode: $selectedRegionCodeResult. Updating ViewModel.")
                authViewModel.onRegionSelected(selectedRegionCodeResult)
                backStackEntry.savedStateHandle.remove<String>(AuthScreen.Args.SELECTED_REGION_CODE)
                Firelog.d("EnterPhoneNumberScreen: Removed selectedRegionCode from SavedStateHandle.")
            }
        }
        
        EnterPhoneNumberScreen(
            authAction = authAction,
            viewModel = authViewModel,
            onNavigateToVerifyOTP = { phoneNumber, action ->
                Firelog.i("EnterPhoneNumberScreen: Navigating to VerifyOTPScreen. Phone (hash): ${phoneNumber.hashCode()}, Action: $action")
                navController.navigate(AuthScreen.VerifyOTP.createRoute(phoneNumber, action))
            },
            onNavigateBack = {
                Firelog.i("EnterPhoneNumberScreen: Navigating back.")
                navController.popBackStack()
            },
            onNavigateToPickCountry = {
                Firelog.i("EnterPhoneNumberScreen: Navigating to PickCountryScreen.")
                navController.navigate(AuthScreen.PickCountry.route)
            }
        )
    }
    
    composable(route = AuthScreen.PickCountry.route) {
        Firelog.i("Navigating to PickCountryScreen.")
        val parentEntry =
            remember(it) { navController.getBackStackEntry(AuthScreen.EnterPhoneNumber.route) }
        val authViewModel: AuthViewModel = hiltViewModel(parentEntry)
        
        Firelog.d("PickCountryScreen: Using shared AuthViewModel instance hash: ${authViewModel.hashCode()} from parent ${parentEntry.destination.route}")
        
        PickCountryScreen(
            viewModel = authViewModel,
            onCountrySelectedAndNavigateBack = { selectedRegionCode ->
                Firelog.i("PickCountryScreen: Country selected: $selectedRegionCode. Navigating back.")
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(AuthScreen.Args.SELECTED_REGION_CODE, selectedRegionCode)
                navController.popBackStack()
            },
            onNavigateBack = {
                Firelog.i("PickCountryScreen: Navigating back.")
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
        val authActionString = backStackEntry.arguments?.getString(AuthScreen.Args.AUTH_ACTION)
        val authAction =
            authActionString?.let { AuthAction.valueOf(it) } ?: AuthAction.LOGIN
        
        Firelog.i("Navigating to VerifyOTPScreen. Phone (from arg hash): ${phoneNumber?.hashCode()}, AuthAction: $authAction (from arg: '$authActionString')")
        
        if (phoneNumber == null || phoneNumber.isBlank()) {
            navController.popBackStack()
            return@composable
        }
        
        VerifyOTPScreen(
            phoneNumberFromNav = phoneNumber,
            authActionFromNav = authAction,
            onNavigateBack = {
                Firelog.i("VerifyOTPScreen: Navigating back.")
                navController.popBackStack()
            }
        )
    }
    
    composable(AuthScreen.ProfileSetupName.route) {
        // JD TODO: Implement
        Firelog.i("Navigating to ProfileSetupNameScreen (Placeholder)")
        Text("Profile Setup Name Screen Placeholder (Implement Me)")
    }
    
    composable(AuthScreen.ProfileSetupEmail.route) {
        // JD TODO: Implement
        Firelog.i("Navigating to ProfileSetupEmailScreen (Placeholder)")
        Text("Profile Setup Email Screen Placeholder (Implement Me)")
    }
    
    composable(AuthScreen.ProfileSetupDob.route) {
        // JD TODO: Implement
        Firelog.i("Navigating to ProfileSetupDobScreen (Placeholder)")
        Text("Profile Setup Dob Screen Placeholder (Implement Me)")
    }
    
    composable(AuthScreen.ProfileSetupNotifs.route) {
        // JD TODO: Implement
        Firelog.i("Navigating to ProfileSetupNotifsScreen (Placeholder)")
        Text("Profile Setup Notifs Screen Placeholder (Implement Me)")
    }
}