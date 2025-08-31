package com.example.campfire.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campfire.auth.data.local.IAuthTokenManager
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.domain.session.SessionEvent
import com.example.campfire.core.domain.session.UserSessionManager
import com.example.campfire.core.domain.usecase.GetAppSetupCompletionStatusUseCase
import com.example.campfire.core.domain.usecase.GetProfileSetupCompletionStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
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
 *  - **Application Readiness:** Determining if essential application data (like initial auth
 *    status and persisted onboarding flags) is loaded and the UI is ready to be displayed
 *    (see [isDataReady]). This is primarily consumed by `MainActivity` to manage the splash screen.
 *  - **Authentication Status:** Exposing the current authentication state of the user
 *    (see [authState]), allowing various parts of the UI to react to login/logout events.
 *    This state is derived from initial token checks and session invalidation events.
 *  - **Onboarding Completion Status:** Continuously observing and exposing whether the user
 *    has completed foundational setup steps, specifically core profile setup (see [isCoreProfileComplete])
 *    and app-specific setup (see [isAppSetupComplete]). These states are determined by
 *    querying dedicated use cases that typically read from persistent storage (e.g., DataStore).
 *    These onboarding flags **persist across user sessions** and are not reset on logout.
 *  - **Session Invalidation Handling:** Observes session invalidation events (e.g., due to token
 *    expiry detected elsewhere) via [UserSessionManager] and updates its authentication state accordingly,
 *    triggering UI changes such as navigation to the authentication flow.
 *
 * Upon initialization ([loadInitialAppState]), it asynchronously fetches the initial authentication
 * status (by checking for existing tokens) and obtains the initial values of persisted onboarding
 * completion flags. It then continuously collects updates to these onboarding flags.
 *
 * It provides methods like [userLoggedIn] and [userLoggedOut] to allow other parts of the
 * application (typically other ViewModels or navigation event handlers) to signal changes in
 * authentication state. Token storage operations (saving/clearing tokens) and the direct updating
 * of persisted onboarding flags are explicitly **not** handled by this ViewModel; they are the
 * responsibility of other, more specialized layers (e.g., auth data/domain layer, onboarding
 * feature ViewModels). This ViewModel reacts to the *outcomes* of those operations by reflecting
 * the current state.
 *
 * Logging of key lifecycle events and state changes is performed using [Firelog].
 *
 * @property getAppSetupCompletionStatusUseCase Use case to observe the app setup completion status.
 * @property getProfileSetupCompletionStatusUseCase Use case to observe the core profile setup completion status.
 * @property userSessionManager Manages and emits session-related events, like invalidation.
 * @property tokenRepository Source for checking the initial presence of authentication tokens.
 */
@HiltViewModel
class GlobalStateViewModel @Inject constructor(
    private val getAppSetupCompletionStatusUseCase: GetAppSetupCompletionStatusUseCase,
    private val getProfileSetupCompletionStatusUseCase: GetProfileSetupCompletionStatusUseCase,
    private val userSessionManager: UserSessionManager,
    private val tokenRepository: IAuthTokenManager
) : ViewModel() {
    
    private val _isDataReady = MutableStateFlow(false)
    
    /**
     * A [StateFlow] indicating whether all critical initial data has been loaded and the UI
     * is ready to be displayed. This is primarily used by `MainActivity` to decide
     * when to hide the splash screen.
     * `false` initially, set to `true` after all essential async setup in [loadInitialAppState] is complete.
     */
    val isDataReady: StateFlow<Boolean> = _isDataReady.asStateFlow()
    
    private val _authState = MutableStateFlow<Boolean?>(null)
    
    /**
     * A [StateFlow] representing the current authentication status of the user.
     * `null` if the initial status is not yet determined.
     * `true` if the user is considered authenticated.
     * `false` if the user is not authenticated or session is invalid.
     * This state is observed by UI components across the application to adapt to user login/logout.
     */
    val authState: StateFlow<Boolean?> = _authState.asStateFlow()
    
    private val _isProfileSetupComplete = MutableStateFlow<Boolean?>(null)
    
    /**
     * A [StateFlow] indicating whether the user has completed the core profile setup.
     * `null` if the status is not yet determined.
     * `true` if completed, `false` otherwise.
     */
    val isProfileSetupComplete: StateFlow<Boolean?> = _isProfileSetupComplete.asStateFlow()
    
    private val _isAppSetupComplete = MutableStateFlow<Boolean?>(null)
    
    /**
     * A [StateFlow] indicating whether the user has completed the app-specific setup.
     * `null` if the status is not yet determined.
     * `true` if completed, `false` otherwise.
     */
    val isAppSetupComplete: StateFlow<Boolean?> = _isAppSetupComplete.asStateFlow()
    
    private val _triggerNavigateToAuth = MutableSharedFlow<Unit>(replay = 0)
    
    /**
     * A [kotlinx.coroutines.flow.SharedFlow] that emits an event to signal that navigation to the
     * authentication feature should occur. This is typically triggered by session invalidation.
     */
    val triggerNavigateToAuth = _triggerNavigateToAuth.asSharedFlow()
    
    init {
        Firelog.d(LOG_VIEWMODEL_INITIALIZED)
        loadInitialAppState()
        
        // Observe session invalidation events
        viewModelScope.launch {
            userSessionManager.sessionEvents.collectLatest { event ->
                when (event) {
                    is SessionEvent.SessionInvalidated -> {
                        Firelog.i("Observed SessionInvalidated event.")
                        _authState.value = false
                        _triggerNavigateToAuth.emit(Unit)
                    }
                }
            }
        }
        viewModelScope.launch {
            getProfileSetupCompletionStatusUseCase().collectLatest { completed ->
                if (_isProfileSetupComplete.value != completed) {
                    Firelog.d("CoreProfileCompletion status updated via Flow: $completed")
                    _isProfileSetupComplete.value = completed
                }
            }
        }
        viewModelScope.launch {
            getAppSetupCompletionStatusUseCase().collectLatest { completed ->
                if (_isAppSetupComplete.value != completed) {
                    Firelog.d("AppSetupCompletion status updated via Flow: $completed")
                    _isAppSetupComplete.value = completed
                }
            }
        }
    }
    
    /**
     * Loads the initial application state regarding authentication status and persisted
     * onboarding completion flags. This method is called once during ViewModel initialization.
     */
    private fun loadInitialAppState() {
        viewModelScope.launch {
            Firelog.d("loadInitialAppState started.")
            // 1. Determine initial authentication status
            val storedTokens = tokenRepository.getTokens()
            val initialAuth = storedTokens != null
            _authState.value = initialAuth
            Firelog.i("Initial auth state set to $initialAuth.")
            
            // 2. Load initial persisted onboarding flags.
            if (_isProfileSetupComplete.value == null) {
                _isProfileSetupComplete.value =
                    getProfileSetupCompletionStatusUseCase().firstOrNull() ?: false
            }
            if (_isAppSetupComplete.value == null) {
                _isAppSetupComplete.value =
                    getAppSetupCompletionStatusUseCase().firstOrNull() ?: false
            }
            Firelog.i("Initial onboarding states loaded. ProfileComplete: ${_isProfileSetupComplete.value}, AppSetupComplete: ${_isAppSetupComplete.value}")
            
            // 3. Mark data as ready
            _isDataReady.value = true
            Firelog.i(LOG_ALL_DATA_READY)
        }
    }
    
    /**
     * Called when the user has successfully logged in through an authentication flow.
     * This updates the authentication state. Persisted onboarding states are already
     * being observed and will reflect the correct status for the logged-in account.
     */
    fun userLoggedIn() {
        Firelog.i("userLoggedIn called.")
        _authState.value = true
        Firelog.d("Auth state set to true. Onboarding states will be updated by their respective collectors.")
    }
    
    /**
     * Called when the user has logged out (e.g., user action from settings) or the session
     * is otherwise terminated locally. This updates the authentication state.
     * Onboarding completion flags are **not** reset by this action as they are tied to the
     * user's account and persist across sessions.
     */
    fun userLoggedOut() {
        Firelog.i("userLoggedOut called (state update only).")
        _authState.value = false
    }
    
    companion object {
        private const val LOG_VIEWMODEL_INITIALIZED =
            "Initialized: Establishing initial global states..."
        private const val LOG_ALL_DATA_READY =
            "All initial global states established. isDataReady set to true."
    }
}