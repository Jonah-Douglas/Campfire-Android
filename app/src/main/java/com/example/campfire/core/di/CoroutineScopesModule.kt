package com.example.campfire.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Named
import javax.inject.Singleton


@Suppress("Unused")
@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopesModule {
    @Singleton
    @Provides
    @Named("ApplicationScope")
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}