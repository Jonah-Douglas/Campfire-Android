package com.example.campfire.core.presentation

import android.util.Log
import android.util.Log.e
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
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val _isDataReady = MutableStateFlow(false)
    val isDataReady = _isDataReady.asStateFlow()
    
    private val _authState = MutableStateFlow(false)
    val authState = _authState.asStateFlow()
    
    val isEntryComplete: StateFlow<Boolean> = userPreferencesRepository.isEntryComplete
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false // Or a sensible default before the flow emits
        )
    
    init {
        Log.d(LOG_TAG, LOG_VIEWMODEL_INITIALIZED)
        try {
            viewModelScope.launch {
                checkAuthStatus()
                
                // Perform other async loading
                loadOtherInitialResources()
                loadUserPreferences()
                
                // Once all critical async setup that _isDataReady depends on is complete, set it to true.
                _isDataReady.value = true
            }
        } catch (e: Exception) {
            e(e.message, LOG_AUTH_STATUS_CHECK_FAIL)
        }
    }
    
    private suspend fun checkAuthStatus() {
        val token = authTokenStorage.getTokens()
        _authState.value = token != null
        Log.d(LOG_TAG, String.format(LOG_TOKEN_EXISTS, token != null))
    }
    
    private suspend fun loadOtherInitialResources() {
        delay(500) // Simulate other loading; adjust as needed
        Log.d(LOG_TAG, LOG_LOADED_INITIAL_RESOURCES)
    }
    
    private fun loadUserPreferences() {
        Log.d(LOG_TAG, LOG_LOADED_USER_PREFS)
    }
    
    fun userLoggedIn() {
        _authState.value = true
        Log.d(LOG_TAG, LOG_USER_LOGGED_IN)
    }
    
    fun userLoggedOut() {
        viewModelScope.launch {
            authTokenStorage.clearTokens()
            _authState.value = false
            Log.d(LOG_TAG, LOG_USER_LOGGED_OUT)
        }
    }
    
    companion object {
        private const val LOG_TAG = "MainViewModel"
        private const val LOG_VIEWMODEL_INITIALIZED =
            "ViewModel Initialized: Checking auth status and loading initial data..."
        private const val LOG_AUTH_STATUS_CHECK_FAIL =
            "Issue with checking auth status and loading data."
        private const val LOG_TOKEN_EXISTS =
            "Auth status checked. Token exists: '%s'"
        private const val LOG_LOADED_INITIAL_RESOURCES =
            "Other initial resources loaded."
        private const val LOG_LOADED_USER_PREFS =
            "User preferences loaded."
        private const val LOG_USER_LOGGED_IN =
            "User logged in."
        private const val LOG_USER_LOGGED_OUT =
            "User logged out."
    }
}