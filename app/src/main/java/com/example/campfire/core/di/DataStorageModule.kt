package com.example.campfire.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


/**
 * The name for the DataStore preferences file used for session-related data.
 */
private const val SESSION_PREFERENCES_NAME = "campfire_session_prefs"

/**
 * Hilt module responsible for providing [DataStore<Preferences>] instances.
 * This module ensures that DataStore is set up correctly and available
 * as a singleton throughout the application.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    
    /**
     * Provides a singleton instance of [DataStore<Preferences>] specifically for storing
     * session-related preferences.
     *
     * This DataStore uses [SESSION_PREFERENCES_NAME] as its file name.
     *
     * @param appContext The application context, required to create the DataStore file.
     * @return A singleton [DataStore<Preferences>] instance for session data.
     */
    @Singleton
    @Provides
    fun provideSessionPreferencesDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { appContext.preferencesDataStoreFile(SESSION_PREFERENCES_NAME) }
        )
    }
}