package com.example.campfire.core.di

import com.example.campfire.auth.data.local.AuthTokenStorage
import com.example.campfire.auth.data.local.IAuthTokenManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


/**
 * Hilt Module responsible for providing bindings for storage-related interfaces
 * to their concrete implementations.
 *
 * This module ensures that when a dependency on a storage interface (e.g., [IAuthTokenManager])
 * is requested, Hilt knows which concrete implementation (e.g., [AuthTokenStorage])
 * to provide.
 */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {
    
    /**
     * Binds the [IAuthTokenManager] interface to its concrete implementation [AuthTokenStorage].
     *
     * This tells Hilt that whenever an instance of [IAuthTokenManager] is required,
     * an instance of [AuthTokenStorage] should be provided. [AuthTokenStorage] is
     * responsible for the secure persistence and retrieval of authentication tokens.
     *
     * @param authTokenStorage The concrete implementation of [IAuthTokenManager].
     *   Hilt will know how to provide this if [AuthTokenStorage]
     *   has an `@Inject` constructor or is provided by another module.
     * @return An instance of [IAuthTokenManager], specifically an [AuthTokenStorage].
     */
    @Binds
    @Singleton
    abstract fun bindAuthTokenStorage(
        authTokenStorage: AuthTokenStorage
    ): IAuthTokenManager
}