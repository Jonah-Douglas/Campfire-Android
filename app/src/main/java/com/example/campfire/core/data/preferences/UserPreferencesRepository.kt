package com.example.campfire.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.campfire.core.common.logging.Firelog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Extension property on [Context] to provide a singleton instance of [DataStore]
 * for user preferences, named "settings".
 *
 * This uses [preferencesDataStore] to create and manage the DataStore instance.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository for managing user-specific preferences using Jetpack DataStore.
 *
 * This class provides a centralized way to access and modify user preferences,
 * abstracting the DataStore implementation details from the rest of the application.
 * Preferences are exposed as [Flow]s, allowing for reactive updates in the UI
 * or other app components.
 *
 * It is a [Singleton] to ensure a single source of truth for user preferences.
 *
 * @property context The application context, used to access the [dataStore].
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesSource {
    /**
     * Internal object holding [Preferences.Key] definitions for type-safe access
     * to DataStore.
     */
    private object PreferencesKeys {
        /**
         * A boolean preference key indicating whether the user has completed
         * an initial entry or setup process (e.g., onboarding, profile creation).
         * Defaults to `false` if not explicitly set.
         */
        val IS_ENTRY_COMPLETE = booleanPreferencesKey("is_entry_complete")
    }
    
    /**
     * A [Flow] emitting the current state of the "is entry complete" preference.
     *
     * This flow will emit `true` if the user has completed the initial entry/setup,
     * and `false` otherwise (including if the preference has never been set, as
     * accessing a non-existent boolean key defaults to `false` in the mapping logic).
     *
     * The flow also includes basic error handling to catch [java.io.IOException]s that might
     * occur during DataStore reads (e.g., disk issues) and emits `false` as a fallback
     * in such cases, logging the error. For more critical errors, it rethrows them.
     */
    override val isEntryComplete: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Firelog.e("Error reading IS_ENTRY_COMPLETE preference", exception)
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_ENTRY_COMPLETE] ?: false
        }
}