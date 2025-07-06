package com.example.campfire.core.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campfire.core.data.auth.AuthTokenStorage
import com.example.campfire.core.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val authTokenStorage: AuthTokenStorage,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val _isDataReady = MutableStateFlow(false)
    val isDataReady = _isDataReady.asStateFlow()
    
    private val _authState = MutableStateFlow(false)
    val authState = _authState.asStateFlow()
    
    val isEntryComplete: StateFlow<Boolean> = userPreferencesRepository.isEntryComplete
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false // Default value until DataStore emits its first value
        )
    
    init {
        Log.d(
            "MainViewModel",
            "ViewModel Initialized: Checking auth status and loading initial data..."
        )
        viewModelScope.launch {
            checkAuthStatus()
            
            // Perform other async loading
            loadOtherInitialResources()
            
            // Once all critical async setup that _isDataReady depends on is complete, set it to true.
            // The isEntryComplete StateFlow will update independently as DataStore emits values.
            _isDataReady.value = true
            
            Log.d(
                "MainViewModel",
                "Initial setup tasks launched. DataReady: true, AuthState: ${_authState.value}, EntryComplete (initial from stateIn): ${isEntryComplete.value}"
            )
        }
    }
    
    private fun checkAuthStatus() {
        val token = authTokenStorage.getTokens()
        _authState.value = token != null
        Log.d("MainViewModel", "Auth status checked. Token exists: ${token != null}")
    }
    
    private suspend fun loadOtherInitialResources() {
        delay(500) // Simulate other loading; adjust as needed
        Log.d("MainViewModel", "Other initial resources loaded.")
    }
    
    fun userLoggedIn() {
        _authState.value = true
        Log.d("MainViewModel", "User logged in. AuthState: true")
    }
    
    fun userLoggedOut() {
        viewModelScope.launch {
            authTokenStorage.clearTokens()
            _authState.value = false
            Log.d("MainViewModel", "User logged out. AuthState: false")
        }
    }
    
    fun entryScreenCompleted() {
        viewModelScope.launch {
            userPreferencesRepository.updateEntryComplete(true)
            Log.d("MainViewModel", "Entry screen marked as complete in preferences.")
        }
    }
}