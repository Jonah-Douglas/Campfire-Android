package com.example.campfire.auth.presentation.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.campfire.auth.domain.model.AuthAction
import com.example.campfire.auth.presentation.AuthNavigationEvent
import com.example.campfire.auth.presentation.AuthViewModel
import com.example.campfire.auth.presentation.screens.EnterPhoneNumberScreen
import com.example.campfire.auth.presentation.screens.EntryScreen
import com.example.campfire.auth.presentation.screens.PickCountryScreen
import com.example.campfire.auth.presentation.screens.VerifyOTPScreen
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.presentation.navigation.AppGraphRoutes


@SuppressLint("UnrememberedGetBackStackEntry")
fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    onNavigateToProfileSetup: () -> Unit,
    onNavigateToFeed: () -> Unit
) {
    val graphScopedAuthViewModel: @Composable () -> AuthViewModel = {
        hiltViewModel(remember { navController.getBackStackEntry(AppGraphRoutes.AUTH_FEATURE_ROUTE) })
    }
    
    composable(AuthScreen.Entry.route) {
        Firelog.i("Navigating to EntryScreen")
        val authViewModel = graphScopedAuthViewModel()
        
        LaunchedEffect(key1 = authViewModel) {
            authViewModel.authNavigationEvents.collect { event ->
                if (event is AuthNavigationEvent.ToEnterPhoneNumberScreen) {
                    Firelog.i("AuthNavGraph (Entry): Navigating to EnterPhoneNumberScreen with action: ${event.action}")
                    navController.navigate(AuthScreen.EnterPhoneNumber.createRoute(event.action))
                }
            }
        }
        
        EntryScreen(
            viewModel = authViewModel
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
        val authViewModel = graphScopedAuthViewModel()
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
            onNavigateBack = {
                Firelog.i("EnterPhoneNumberScreen: Navigating back.")
                navController.popBackStack()
            },
            onNavigateToPickCountry = {
                Firelog.i("EnterPhoneNumberScreen: Navigating to PickCountryScreen.")
                navController.navigate(AuthScreen.PickCountry.route)
            },
            onNavigateToVerifyOTP = { phoneNumber, action ->
                Firelog.i("EnterPhoneNumberScreen: Navigating to VerifyOTPScreen. Phone (hash): ${phoneNumber.hashCode()}, Action: $action")
                navController.navigate(AuthScreen.VerifyOTP.createRoute(phoneNumber, action))
            }
        )
    }
    
    composable(route = AuthScreen.PickCountry.route) {
        Firelog.i("Navigating to PickCountryScreen.")
        val authViewModel = graphScopedAuthViewModel()
        Firelog.d("PickCountryScreen: AuthViewModel instance hash: ${authViewModel.hashCode()}")
        
        PickCountryScreen(
            viewModel = authViewModel,
            onNavigateBack = {
                Firelog.i("PickCountryScreen: Navigating back.")
                navController.popBackStack()
            },
            onCountrySelectedAndNavigateBack = { selectedRegionCode ->
                Firelog.i("PickCountryScreen: Country selected: $selectedRegionCode. Navigating back.")
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(AuthScreen.Args.SELECTED_REGION_CODE, selectedRegionCode)
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
            Firelog.w("VerifyOTPScreen: Phone number is null/blank, popping back.")
            navController.popBackStack()
            return@composable
        }
        
        val authViewModel = graphScopedAuthViewModel()
        Firelog.d("VerifyOTPScreen: AuthViewModel instance hash: ${authViewModel.hashCode()}")
        
        LaunchedEffect(key1 = authViewModel) {
            authViewModel.authNavigationEvents.collect { event ->
                Firelog.d("VerifyOTPScreen: AuthViewModel event received: $event")
                
                when (event) {
                    is AuthNavigationEvent.ToOnboarding -> {
                        Firelog.i("VerifyOTPScreen: Calling onNavigateToProfileSetup (to RootNavGraph)")
                        onNavigateToProfileSetup()
                    }
                    
                    is AuthNavigationEvent.ToFeed -> {
                        Firelog.i("VerifyOTPScreen: Calling onNavigateToMainApp (to RootNavGraph)")
                        onNavigateToFeed()
                    }
                    
                    else -> Firelog.d("VerifyOTPScreen: Unhandled/internal auth navigation event: $event")
                }
            }
        }
        
        VerifyOTPScreen(
            viewModel = authViewModel,
            phoneNumberFromNav = phoneNumber,
            authActionFromNav = authAction,
            onNavigateBack = {
                Firelog.i("VerifyOTPScreen: Navigating back.")
                navController.popBackStack()
            }
        )
    }
}