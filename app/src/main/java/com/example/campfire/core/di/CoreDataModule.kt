package com.example.campfire.core.di

import com.example.campfire.core.data.preferences.UserPreferencesRepository
import com.example.campfire.core.data.preferences.UserPreferencesSource
import com.example.campfire.core.data.session.UserSessionManagerImpl
import com.example.campfire.core.domain.session.UserSessionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


/**
 * Hilt module that provides singleton-scoped dependencies related to core data handling.
 * This includes services for managing user preferences and user sessions.
 */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreDataModule {
    
    /**
     * Binds [UserPreferencesRepository] to the [UserPreferencesSource] interface.
     * This allows other components to inject [UserPreferencesSource] and receive an instance
     * of [UserPreferencesRepository] as the concrete implementation for managing user preferences.
     * Provided as a singleton.
     */
    @Binds
    @Singleton
    abstract fun bindUserPreferencesSource(
        userPreferencesRepository: UserPreferencesRepository
    ): UserPreferencesSource
    
    /**
     * Binds [UserSessionManagerImpl] to the [UserSessionManager] interface.
     * This allows other components to inject [UserSessionManager] and receive an instance
     * of [UserSessionManagerImpl] as the concrete implementation for managing the user's session.
     * Provided as a singleton.
     */
    @Binds
    @Singleton
    abstract fun bindUserSessionManager(
        userSessionManagerImpl: UserSessionManagerImpl
    ): UserSessionManager
}