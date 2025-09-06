package com.example.campfire.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.campfire.core.domain.model.SessionEvent
import com.example.campfire.core.domain.session.UserSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class UserSessionManagerImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserSessionManager {
    
    private object PrefKeys {
        val IS_AUTHENTICATED = booleanPreferencesKey("session_is_authenticated")
        val IS_PROFILE_COMPLETE = booleanPreferencesKey("session_is_profile_complete")
        val IS_APP_SETUP_COMPLETE = booleanPreferencesKey("session_is_app_setup_complete")
    }
    
    override val sessionEvents = MutableSharedFlow<SessionEvent>()
    
    override fun isAuthenticatedFlow(): StateFlow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PrefKeys.IS_AUTHENTICATED] ?: false
        }.stateIn(
            CoroutineScope(Dispatchers.IO + SupervisorJob()),
            SharingStarted.Eagerly, false
        )
    
    override fun isProfileCompleteFlow(): StateFlow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PrefKeys.IS_PROFILE_COMPLETE] ?: false
        }.stateIn(
            CoroutineScope(Dispatchers.IO + SupervisorJob()),
            SharingStarted.Eagerly, false
        )
    
    override fun isAppSetupCompleteFlow(): StateFlow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PrefKeys.IS_APP_SETUP_COMPLETE] ?: false
        }.stateIn(
            CoroutineScope(Dispatchers.IO + SupervisorJob()),
            SharingStarted.Eagerly, false
        )
    
    
    override suspend fun updateUserLoginAndOnboardingState(
        isAuthenticated: Boolean,
        isProfileComplete: Boolean,
        isAppSetupComplete: Boolean
    ) {
        dataStore.edit { settings ->
            settings[PrefKeys.IS_AUTHENTICATED] = isAuthenticated
            settings[PrefKeys.IS_PROFILE_COMPLETE] = isProfileComplete
            settings[PrefKeys.IS_APP_SETUP_COMPLETE] = isAppSetupComplete
        }
        if (!isAuthenticated) {
            sessionEvents.emit(SessionEvent.SessionInvalidated)
        }
    }
    
    override suspend fun updateProfileSetupComplete(isComplete: Boolean) {
        dataStore.edit { settings ->
            settings[PrefKeys.IS_PROFILE_COMPLETE] = isComplete
        }
    }
    
    override suspend fun updateAppSetupComplete(isComplete: Boolean) {
        dataStore.edit { settings ->
            settings[PrefKeys.IS_APP_SETUP_COMPLETE] = isComplete
        }
    }
    
    override suspend fun clearUserSession() {
        dataStore.edit { settings ->
            settings[PrefKeys.IS_AUTHENTICATED] = false
        }
        sessionEvents.emit(SessionEvent.SessionInvalidated)
    }
    
    override suspend fun notifySessionInvalidated() {
        dataStore.edit { settings ->
            settings[PrefKeys.IS_AUTHENTICATED] = false
        }
        sessionEvents.emit(SessionEvent.SessionInvalidated)
    }
}