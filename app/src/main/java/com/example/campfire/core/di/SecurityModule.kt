package com.example.campfire.core.di

import com.example.campfire.core.security.AndroidKeystoreEncryptionManager
import com.example.campfire.core.security.IEncryptionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


/**
 * Hilt Module responsible for providing bindings for security-related interfaces
 * to their concrete implementations.
 *
 * This module ensures that when a dependency on a security interface (e.g., [IEncryptionManager])
 * is requested, Hilt knows which concrete implementation (e.g., [AndroidKeystoreEncryptionManager])
 * to provide.
 */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {
    
    /**
     * Binds the [IEncryptionManager] interface to its concrete implementation [AndroidKeystoreEncryptionManager].
     *
     * This tells Hilt that whenever an instance of [IEncryptionManager] is required,
     * an instance of [AndroidKeystoreEncryptionManager] should be provided.
     *
     * @param androidKeystoreEncryptionManager The concrete implementation of [IEncryptionManager].
     *                              Hilt will know how to provide this if [AndroidKeystoreEncryptionManager]
     *                              has an `@Inject` constructor or is provided by another module.
     * @return An instance of [IEncryptionManager], specifically an [AndroidKeystoreEncryptionManager].
     */
    @Binds
    @Singleton
    abstract fun bindEncryptionManager(
        androidKeystoreEncryptionManager: AndroidKeystoreEncryptionManager
    ): IEncryptionManager
}