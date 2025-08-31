package com.example.campfire.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.campfire.core.common.logging.Firelog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesSource {
    
    private object PreferencesKeys {
        val IS_PROFILE_SETUP_COMPLETE = booleanPreferencesKey("is_profile_setup_complete")
        val IS_APP_SETUP_COMPLETE = booleanPreferencesKey("is_app_setup_complete")
    }
    
    override val isProfileSetupComplete: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Firelog.e("Error reading IS_CORE_PROFILE_SETUP_COMPLETE", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_PROFILE_SETUP_COMPLETE] ?: false
        }
    
    override val isAppSetupComplete: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Firelog.e("Error reading IS_APP_SETUP_COMPLETE", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_APP_SETUP_COMPLETE] ?: false
        }
    
    override suspend fun setProfileSetupComplete(completed: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_PROFILE_SETUP_COMPLETE] = completed
            }
            Firelog.d("Set IS_PROFILE_SETUP_COMPLETE to $completed")
        } catch (exception: IOException) {
            Firelog.e("Error writing IS_PROFILE_SETUP_COMPLETE", exception)
        }
    }
    
    override suspend fun setAppSetupComplete(completed: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_APP_SETUP_COMPLETE] = completed
            }
            Firelog.d("Set IS_APP_SETUP_COMPLETE to $completed")
        } catch (exception: IOException) {
            Firelog.e("Error writing IS_APP_SETUP_COMPLETE", exception)
        }
    }
    
    override suspend fun clearOnboardingFlags() {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_PROFILE_SETUP_COMPLETE] = false
                preferences[PreferencesKeys.IS_APP_SETUP_COMPLETE] = false
            }
            Firelog.i("Cleared onboarding flags in preferences.")
        } catch (exception: IOException) {
            Firelog.e("Error clearing onboarding flags", exception)
        }
    }
}