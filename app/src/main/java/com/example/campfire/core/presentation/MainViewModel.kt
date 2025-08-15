package com.example.campfire.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campfire.core.common.logging.Firelog
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


/**
 * A [ViewModel] responsible for managing the overall UI state for the [MainActivity]
 * and coordinating initial data loading operations for the application.
 *
 * This ViewModel handles:
 * - Determining if essential application data is ready to be displayed (see [isDataReady]),
 *   which is used by [MainActivity] to control the splash screen.
 * - Checking and exposing the current authentication status of the user (see [authState]).
 * - Exposing whether the user has completed initial entry/setup steps (see [isEntryComplete]).
 * - Performing initial asynchronous tasks such as checking authentication tokens and loading
 *   user preferences or other critical resources upon initialization.
 * - Providing methods to update the authentication state ([userLoggedIn], [userLoggedOut]).
 *
 * It utilizes [AuthTokenStorage] to manage authentication tokens and [UserPreferencesRepository]
 * to access user-specific settings.
 *
 * Logging of key lifecycle events and operations is performed using [Firelog].
 *
 * @property authTokenStorage Repository for accessing and managing authentication tokens.
 * @property userPreferencesRepository Repository for accessing user-specific preferences,
 *                                   including the completion status of initial entry.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authTokenStorage: AuthTokenStorage,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val _isDataReady = MutableStateFlow(false)
    
    /**
     * A [StateFlow] indicating whether all critical initial data has been loaded and the UI
     * is ready to be displayed. This is primarily used by [MainActivity] to decide when
     * to hide the splash screen.
     * `false` initially, set to `true` after all essential async setup in `init` is complete.
     */
    val isDataReady = _isDataReady.asStateFlow()
    
    private val _authState = MutableStateFlow(false)
    
    /**
     * A [StateFlow] representing the current authentication status of the user.
     * `true` if the user is considered authenticated (e.g., a valid token exists), `false` otherwise.
     * This state is observed by UI components to adapt to user login/logout.
     */
    val authState = _authState.asStateFlow()
    
    /**
     * A [StateFlow] indicating whether the user has completed the initial entry flow
     * (e.g., onboarding, profile setup if applicable after registration).
     * This value is sourced directly from [UserPreferencesRepository].
     *
     * The flow is converted to a [StateFlow] that is shared while subscribed, with a
     * timeout of 5000ms, and starts with an initial value of `false`.
     */
    val isEntryComplete: StateFlow<Boolean> = userPreferencesRepository.isEntryComplete
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    init {
        Firelog.d(LOG_VIEWMODEL_INITIALIZED)
        try {
            viewModelScope.launch {
                // Perform initial setup tasks asynchronously
                checkAuthStatus()
                
                // Perform other async loading
                loadOtherInitialResources()
                loadUserPreferences()
                
                // Once all critical async setup that _isDataReady depends on is complete, set it to true
                _isDataReady.value = true
                Firelog.d(LOG_ALL_DATA_READY)
            }
        } catch (e: Exception) {
            Firelog.e(LOG_INIT_ERROR, e)
            _isDataReady.value = true
        }
    }
    
    /**
     * Checks for the existence of authentication tokens using [AuthTokenStorage]
     * and updates the [_authState] accordingly.
     */
    private suspend fun checkAuthStatus() {
        val token = authTokenStorage.getTokens()
        _authState.value = token != null
        Firelog.d(String.format(LOG_TOKEN_EXISTS, token != null))
    }
    
    /**
     * Placeholder for loading other initial resources asynchronously.
     * This could involve network requests, database queries, or other setup tasks.
     */
    // JD TODO: Replace with actual async loading
    private suspend fun loadOtherInitialResources() {
        delay(500) // Simulate other loading; adjust as needed
        Firelog.d(LOG_LOADED_INITIAL_RESOURCES)
    }
    
    /**
     * Placeholder for loading user preferences.
     * This might involve reading from SharedPreferences, DataStore, or another storage mechanism.
     * Note: `isEntryComplete` is already handled directly from the repository flow.
     * This function could be for other preferences not exposed as a direct flow.
     */
    private fun loadUserPreferences() {
        Firelog.d(LOG_LOADED_USER_PREFS)
    }
    
    /**
     * Updates the authentication state to indicate that a user has successfully logged in.
     * This is typically called after a successful login operation.
     */
    fun userLoggedIn() {
        _authState.value = true
        Firelog.d(LOG_USER_LOGGED_IN)
    }
    
    /**
     * Updates the authentication state to indicate that a user has logged out.
     * This involves clearing any stored authentication tokens via [AuthTokenStorage]
     * and then setting the [_authState] to `false`.
     * This operation is performed asynchronously within the [viewModelScope].
     */
    fun userLoggedOut() {
        viewModelScope.launch {
            authTokenStorage.clearTokens()
            _authState.value = false
            Firelog.d(LOG_USER_LOGGED_OUT)
        }
    }
    
    companion object {
        private const val LOG_VIEWMODEL_INITIALIZED =
            "ViewModel Initialized: Checking auth status and loading initial data..."
        private const val LOG_INIT_ERROR =
            "Error during MainViewModel initialization and data loading."
        private const val LOG_ALL_DATA_READY =
            "All initial data loaded. isDataReady set to true."
        private const val LOG_TOKEN_EXISTS =
            "Auth status checked. Token exists: '%s'"
        private const val LOG_LOADED_INITIAL_RESOURCES =
            "Other initial resources loaded."
        private const val LOG_LOADED_USER_PREFS =
            "User preferences loaded."
        private const val LOG_USER_LOGGED_IN =
            "User logged in successfully."
        private const val LOG_USER_LOGGED_OUT =
            "User logged out successfully and tokens cleared."
    }
}