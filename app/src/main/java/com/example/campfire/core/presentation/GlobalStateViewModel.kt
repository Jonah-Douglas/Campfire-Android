package com.example.campfire.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campfire.auth.data.local.IAuthTokenManager
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.data.preferences.UserPreferencesSource
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
 * A [ViewModel] responsible for managing and exposing critical global UI states for the
 * entire application. It also coordinates essential initial data loading required to
 * establish these states.
 *
 * The name `GlobalStateViewModel` emphasizes its focused role: to be the definitive source
 * for application-wide states that are not tied to any single feature, thereby discouraging
 * it from becoming a catch-all for unrelated logic.
 *
 * This ViewModel handles:
 *  - **Application Readiness:** Determining if essential application data is loaded and the
 *    UI is ready to be displayed (see [isDataReady]). This is primarily consumed by the
 *    main application shell (e.g., `MainActivity`) to manage elements like splash screens.
 *  - **Authentication Status:** Exposing the current authentication state of the user
 *    (see [authState]), allowing various parts of the UI to react to login/logout events.
 *  - **Initial Entry Completion:** Indicating whether the user has completed foundational
 *    setup steps, such as onboarding (see [isEntryComplete]).
 *
 * Upon initialization, it performs asynchronous tasks like checking for existing authentication
 * tokens and loading critical user preferences to correctly establish its initial states.
 * It provides methods to update the authentication state ([userLoggedIn], [userLoggedOut]),
 * delegating specific operations like token clearing to appropriate data sources.
 *
 * It utilizes [com.example.campfire.auth.data.local.IAuthTokenManager] for managing authentication tokens and
 * [com.example.campfire.core.data.preferences.UserPreferencesSource] for accessing user-specific settings.
 * Logging of key lifecycle events and operations is performed using [Firelog].
 *
 * @property authTokenManager Source for accessing and managing authentication tokens.
 * @property userPreferencesSource Source for accessing user-specific preferences,
 *                                   including the completion status of initial entry.
 */
@HiltViewModel
class GlobalStateViewModel @Inject constructor(
    private val authTokenManager: IAuthTokenManager,
    private val userPreferencesSource: UserPreferencesSource
) : ViewModel() {
    private val _isDataReady = MutableStateFlow(false)
    
    /**
     * A [StateFlow] indicating whether all critical initial data has been loaded and the UI
     * is ready to be displayed. This is primarily used by the main application shell
     * (e.g., `MainActivity`) to decide when to hide the splash screen.
     * `false` initially, set to `true` after all essential async setup in `init` is complete.
     */
    val isDataReady = _isDataReady.asStateFlow()
    
    private val _authState = MutableStateFlow(false)
    
    /**
     * A [StateFlow] representing the current authentication status of the user.
     * `true` if the user is considered authenticated (e.g., a valid token exists), `false` otherwise.
     * This state is observed by UI components across the application to adapt to user login/logout.
     */
    val authState = _authState.asStateFlow()
    
    /**
     * A [StateFlow] indicating whether the user has completed the initial entry flow
     * (e.g., onboarding, profile setup if applicable after registration).
     * This value is sourced directly from [UserPreferencesSource].
     *
     * The flow is converted to a [StateFlow] that is shared while subscribed, with a
     * timeout of 5000ms, and starts with an initial value of `false`.
     */
    val isEntryComplete: StateFlow<Boolean> = userPreferencesSource.isEntryComplete
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
     * Checks for the existence of authentication tokens using [IAuthTokenManager]
     * and updates the [_authState] accordingly.
     */
    private suspend fun checkAuthStatus() {
        val token = authTokenManager.getTokens()
        _authState.value = token != null
        Firelog.d(String.format(LOG_TOKEN_EXISTS, token != null))
    }
    
    /**
     * Placeholder for loading other initial resources asynchronously that may be necessary
     * for establishing the initial global state of the application.
     * This could involve network requests for essential configuration or other setup tasks.
     */
    // JD TODO: Replace with actual async loading
    private suspend fun loadOtherInitialResources() {
        delay(500) // Simulate other loading; adjust as needed
        Firelog.d(LOG_LOADED_INITIAL_RESOURCES)
    }
    
    /**
     * Placeholder for loading any other user preferences that might directly influence
     * initial global states managed by this ViewModel.
     */
    private fun loadUserPreferences() {
        Firelog.d(LOG_LOADED_USER_PREFS)
    }
    
    /**
     * Updates the authentication state to indicate that a user has successfully logged in.
     * This is typically called after a successful login operation from the authentication feature.
     */
    fun userLoggedIn() {
        _authState.value = true
        Firelog.d(LOG_USER_LOGGED_IN)
    }
    
    /**
     * Updates the authentication state to indicate that a user has logged out.
     * This involves clearing any stored authentication tokens via [IAuthTokenManager]
     * and then setting the [_authState] to `false`.
     * This operation is performed asynchronously within the [viewModelScope].
     */
    fun userLoggedOut() {
        viewModelScope.launch {
            authTokenManager.clearTokens()
            _authState.value = false
            Firelog.d(LOG_USER_LOGGED_OUT)
        }
    }
    
    companion object {
        private const val LOG_VIEWMODEL_INITIALIZED =
            "GlobalStateViewModel Initialized: Establishing initial global states..."
        private const val LOG_INIT_ERROR =
            "Error during GlobalStateViewModel initialization and state setup."
        private const val LOG_ALL_DATA_READY =
            "All initial global states established. isDataReady set to true."
        private const val LOG_TOKEN_EXISTS =
            "Authentication status checked. Valid token exists: '%s'"
        private const val LOG_LOADED_INITIAL_RESOURCES =
            "Other initial global resources loaded."
        private const val LOG_LOADED_USER_PREFS =
            "Initial global user preferences loaded."
        private const val LOG_USER_LOGGED_IN =
            "Global authState updated: User logged in."
        private const val LOG_USER_LOGGED_OUT =
            "Global authState updated: User logged out and tokens cleared."
    }
}