package com.example.campfire.core.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.campfire.auth.presentation.AuthViewModel
import com.example.campfire.auth.presentation.navigation.AuthScreen
import com.example.campfire.auth.presentation.navigation.authGraph
import com.example.campfire.feed.presentation.navigation.FeedScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    isAuthenticated: Boolean,
    isEntryComplete: Boolean,
    onAuthSuccess: () -> Unit,
    onLogout: () -> Unit,
    onEntryScreenComplete: () -> Unit
) {
    val topLevelStartDestination = if (isAuthenticated) {
        AppGraphRoutes.MAIN_APP_GRAPH_ROUTE
    } else {
        AppGraphRoutes.AUTH_GRAPH_ROUTE
    }
    
    // --- Global Auth UI Logic (Scoped to Auth Graph) ---
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    
    val authGraphBackStackEntry = remember(currentBackStackEntry) { // Keyed remember
        try {
            navController.getBackStackEntry(AppGraphRoutes.AUTH_GRAPH_ROUTE)
        } catch (e: IllegalArgumentException) {
            // The graph is not on the back stack (e.g., when main_app_graph is active)
            null
        }
    }
    
    val authViewModel: AuthViewModel? = if (authGraphBackStackEntry != null) {
        hiltViewModel(authGraphBackStackEntry) // Pass the remembered entry
    } else {
        null
    }
    
    var showGlobalDialog by rememberSaveable(authViewModel) { mutableStateOf(false) }
    var globalDialogMessage by rememberSaveable(authViewModel) { mutableStateOf<String?>(null) }
    
    authViewModel?.let { vm ->
        val messageState by vm.message.collectAsState()
        val isLoading by vm.isLoading.collectAsState()
        
        LaunchedEffect(messageState) {
            messageState?.let { currentMessage ->
                if (currentMessage.isNotBlank()) {
                    globalDialogMessage = currentMessage
                    showGlobalDialog = true
                    vm.clearMessage()
                }
            }
        }
        
        if (showGlobalDialog && globalDialogMessage != null) {
            AlertDialog(
                onDismissRequest = { showGlobalDialog = false; globalDialogMessage = null },
                title = { Text("Notification") },
                text = { Text(globalDialogMessage!!) },
                confirmButton = {
                    Button(onClick = { showGlobalDialog = false; globalDialogMessage = null }) {
                        Text("OK")
                    }
                }
            )
        }
        
        if (isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            ) {
                CircularProgressIndicator()
            }
        }
    }
    // --- End Global Auth UI Logic ---
    
    NavHost(
        navController = navController,
        startDestination = topLevelStartDestination
    ) {
        navigation(
            startDestination = if (isEntryComplete) AuthScreen.Login.route else AuthScreen.Entry.route,
            route = AppGraphRoutes.AUTH_GRAPH_ROUTE
        ) {
            authGraph(
                navController = navController,
                onAuthSuccessNavigation = {
                    onAuthSuccess()
                    navController.navigate(AppGraphRoutes.MAIN_APP_GRAPH_ROUTE) {
                        popUpTo(AppGraphRoutes.AUTH_GRAPH_ROUTE) { inclusive = true }
                    }
                },
                onEntryScreenComplete = onEntryScreenComplete
            )
        }
        
        navigation(
            startDestination = FeedScreen.FeedOverview.route,
            route = AppGraphRoutes.MAIN_APP_GRAPH_ROUTE
        ) {
            mainAppGraph(
                navController = navController,
                onLogout = {
                    onLogout()
                    navController.navigate(AppGraphRoutes.AUTH_GRAPH_ROUTE) {
                        popUpTo(AppGraphRoutes.MAIN_APP_GRAPH_ROUTE) { inclusive = true }
                    }
                }
            )
        }
    }
}