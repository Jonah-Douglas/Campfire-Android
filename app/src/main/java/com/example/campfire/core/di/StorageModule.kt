package com.example.campfire.core.di

import com.example.campfire.core.data.EncryptedAuthTokenStorage
import com.example.campfire.core.data.auth.AuthTokenStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


// JD TODO: Confirm once I have auth fully established that this still has no refs and needs the suppressed warning
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {
    
    @Binds
    @Singleton
    abstract fun bindAuthTokenStorage(
        secretAuthTokenStorage: EncryptedAuthTokenStorage
    ): AuthTokenStorage
}