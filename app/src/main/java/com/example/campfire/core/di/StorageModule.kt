package com.example.campfire.core.di

import com.example.campfire.core.data.auth.AuthTokenStorage
import com.example.campfire.core.data.auth.IAuthTokenManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {
    
    @Binds
    @Singleton
    abstract fun bindAuthTokenStorage(
        authTokenStorage: AuthTokenStorage
    ): IAuthTokenManager
}