package com.example.campfire.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campfire.auth.data.local.IAuthTokenManager
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.domain.model.SessionEvent
import com.example.campfire.core.domain.session.UserSessionManager
import com.example.campfire.core.domain.usecase.GetAppSetupCompletionStatusUseCase
import com.example.campfire.core.domain.usecase.GetProfileSetupCompletionStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
 *  - **Application Readiness:** Determining if essential application data (like initial
 *    authentication status and persisted onboarding flags) is loaded and the UI is ready to
 *    be displayed (see [isDataReady]). This is primarily consumed by `MainActivity` to
 *    manage the splash screen.
 *  - **Authentication Status:** Exposing the current authentication state of the user
 *    (see [authState]), allowing various parts of the UI to react to login/logout events.
 *    This state is primarily derived from [UserSessionManager] and initial token checks.
 *  - **Onboarding Completion Status:** Continuously observing and exposing whether the user
 *    has completed foundational setup steps, specifically core profile setup (see
 *    [isProfileSetupComplete]) and app-specific setup (see [isAppSetupComplete]).
 *    These states are sourced from [UserSessionManager], which typically reflects persisted
 *    values (e.g., from DataStore). These onboarding flags **persist across user sessions**
 *    and are not reset on logout.
 *  - **Session Invalidation Handling:** Observes session invalidation events (e.g., due to token
 *    expiry detected elsewhere) via [UserSessionManager] and updates its authentication state
 *    accordingly, potentially triggering UI changes such as navigation to the authentication flow
 *    via [triggerNavigateToAuth].
 *
 * Upon initialization (within its `init` block), it asynchronously:
 *  1. Checks for existing authentication tokens via [tokenRepository] to determine an
 *     initial authentication status.
 *  2. Ensures consistency between token presence and the session state reported by [UserSessionManager].
 *  3. Sets [isDataReady] to `true` once these initial checks are complete, relying on
 *     [UserSessionManager] to provide the initial states for [authState],
 *     [isProfileSetupComplete], and [isAppSetupComplete].
 *
 * It provides methods like [userLoggedOut] to allow other parts of the application
 * (typically other ViewModels or navigation event handlers) to signal changes in
 * authentication state. The direct management of token storage (saving/clearing tokens)
 * and the direct updating of persisted onboarding flags are explicitly **not** handled by
 * this ViewModel; they are the responsibility of other, more specialized layers (e.g.,
 * auth data/domain layer, onboarding feature ViewModels, or [UserSessionManager] itself for
 * session-related flags). This ViewModel reacts to the *outcomes* of those operations by
 * reflecting the current state as exposed by [UserSessionManager] or initial token checks.
 *
 * Logging of key lifecycle events and state changes is performed using [Firelog].
 *
 * @property getAppSetupCompletionStatusUseCase Use case to observe the app setup completion status.
 *           (Note: Current implementation sources this via UserSessionManager.
 *           This property might be for future use or an alternative observation mechanism).
 * @property getProfileSetupCompletionStatusUseCase Use case to observe the core profile setup completion status.
 *           (Note: Current implementation sources this via UserSessionManager.
 *           This property might be for future use or an alternative observation mechanism).
 * @property userSessionManager Manages user session state (authentication, profile completion,
 *           app setup completion) and emits session-related events like invalidation. It is the
 *           primary source for [authState], [isProfileSetupComplete], and [isAppSetupComplete].
 * @property tokenRepository Source for checking the initial presence of authentication tokens,
 *           used to establish the initial [isDataReady] state.
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
    
    /**
     * A [StateFlow] representing the current authentication state of the user.
     * `true` if the user is considered authenticated, `false` otherwise.
     * This state is derived from [UserSessionManager].
     */
    val authState: StateFlow<Boolean> = userSessionManager.isAuthenticatedFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    /**
     * A [StateFlow] indicating whether the user has completed the core profile setup.
     * `true` if complete, `false` otherwise. Persists across sessions.
     * This state is derived from [UserSessionManager].
     */
    val isProfileSetupComplete: StateFlow<Boolean> = userSessionManager.isProfileCompleteFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    /**
     * A [StateFlow] indicating whether the user has completed app-specific setup steps.
     * `true` if complete, `false` otherwise. Persists across sessions.
     * This state is derived from [UserSessionManager].
     */
    val isAppSetupComplete: StateFlow<Boolean> = userSessionManager.isAppSetupCompleteFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    private val _triggerNavigateToAuth = MutableSharedFlow<Unit>(replay = 0)
    
    /**
     * A [SharedFlow] that emits an event to signal that navigation to the
     * authentication flow is required, typically due to session invalidation.
     * Observers can collect this flow to react to such events.
     */
    val triggerNavigateToAuth = _triggerNavigateToAuth.asSharedFlow()
    
    init {
        Firelog.d(LOG_VIEWMODEL_INITIALIZED)
        
        // Observe session invalidation events from UserSessionManager
        viewModelScope.launch {
            userSessionManager.sessionEvents.collectLatest { event ->
                when (event) {
                    is SessionEvent.SessionInvalidated -> {
                        Firelog.i("Observed SessionInvalidated event.")
                        _triggerNavigateToAuth.emit(Unit)
                    }
                }
            }
        }
        
        viewModelScope.launch {
            val storedTokens = tokenRepository.getTokens()
            val initialAuth = storedTokens != null
            
            if (!initialAuth && userSessionManager.isAuthenticatedFlow().value) {
                // Should not happen if DataStore is cleared on logout, but as a safeguard.
                userSessionManager.clearUserSession()
            } else if (initialAuth && !userSessionManager.isAuthenticatedFlow().value) {
                Firelog.w("Initial token found, but UserSessionManager reports not authenticated. State might be stale until next login.")
            }
            
            _isDataReady.value = true
            Firelog.i("$LOG_ALL_DATA_READY (relies on UserSessionManager providing initial states)")
        }
    }
    
    /**
     * Called when the user has logged out (e.g., user action from settings) or the session
     * is otherwise terminated locally. This updates the authentication state.
     * Onboarding completion flags are **not** reset by this action as they are tied to the
     * user's account and persist across sessions.
     */
    fun userLoggedOut() {
        Firelog.i("userLoggedOut called. Instructing UserSessionManager to clear session.")
        viewModelScope.launch {
            userSessionManager.clearUserSession()
        }
    }
    
    /**
     * Updates the persisted status of the core profile setup completion.
     * This method delegates the actual persistence to the [UserSessionManager].
     * The updated status will be reflected in the [isProfileSetupComplete] StateFlow.
     *
     * @param isComplete `true` if profile setup is now complete, `false` otherwise.
     */
    fun setProfileSetupComplete(isComplete: Boolean) {
        viewModelScope.launch {
            userSessionManager.updateProfileSetupComplete(isComplete)
        }
    }
    
    /**
     * Updates the persisted status of the app-specific setup completion.
     * This method delegates the actual persistence to the [UserSessionManager].
     * The updated status will be reflected in the [isAppSetupComplete] StateFlow.
     *
     * @param isComplete `true` if app-specific setup is now complete, `false` otherwise.
     */
    fun setAppSetupComplete(isComplete: Boolean) {
        viewModelScope.launch {
            userSessionManager.updateAppSetupComplete(isComplete)
        }
    }
    
    companion object {
        private const val LOG_VIEWMODEL_INITIALIZED =
            "Initialized: Establishing initial global states..."
        private const val LOG_ALL_DATA_READY =
            "All initial global states established. isDataReady set to true."
    }
}