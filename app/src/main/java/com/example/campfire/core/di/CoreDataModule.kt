package com.example.campfire.core.di

import com.example.campfire.core.data.preferences.UserPreferencesRepository
import com.example.campfire.core.data.preferences.UserPreferencesSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class CoreDataModule {
    
    @Binds
    @Singleton
    abstract fun bindUserPreferencesSource(
        userPreferencesRepository: UserPreferencesRepository
    ): UserPreferencesSource
}