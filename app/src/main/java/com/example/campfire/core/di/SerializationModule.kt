package com.example.campfire.core.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


/**
 * Hilt Module responsible for providing serialization-related dependencies,
 * primarily a configured instance of [Gson] for JSON processing.
 *
 * This module ensures that a consistent, shared instance of [Gson] is available
 * throughout the application for serializing and deserializing objects to and from JSON.
 *
 * It is installed in the [SingletonComponent], meaning the provided [Gson] instance
 * will be a singleton.
 */
@Module
@InstallIn(SingletonComponent::class)
object SerializationModule {
    
    /**
     * Provides a singleton instance of [Gson].
     *
     * This method returns a default [Gson] instance.
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}