package com.example.campfire.core.di

import com.example.campfire.core.security.AndroidKeystoreEncryptionManager
import com.example.campfire.core.security.IEncryptionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {
    
    @Binds
    @Singleton
    abstract fun bindEncryptionManager(
        androidKeystoreEncryptionManager: AndroidKeystoreEncryptionManager
    ): IEncryptionManager
}