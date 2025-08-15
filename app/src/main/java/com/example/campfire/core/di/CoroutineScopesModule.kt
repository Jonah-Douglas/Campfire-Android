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


/**
 * Hilt Module that provides application-level [CoroutineScope] instances.
 *
 * Scopes provided by this module are designed to live as long as the application itself
 * and should be used for operations that need to continue regardless of individual
 * screen or component lifecycles, or for managing background tasks that are tied
 * to the application's lifetime.
 *
 * This module is installed in the [SingletonComponent], meaning any scope provided
 * here will be a singleton and available throughout the application.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopesModule {
    
    /**
     * Provides a singleton [CoroutineScope] intended for application-wide operations.
     *
     * This scope is configured with:
     * - **[SupervisorJob]:** Ensures that the failure of one child coroutine
     *   does not cancel the entire scope or its other children. This is crucial for
     *   an application-level scope where independent tasks might be running.
     * - **[Dispatchers.Default]:** Uses the default dispatcher, which is optimized for
     *   CPU-intensive work off the main thread. It's suitable for operations
     *   like complex calculations, data processing, or background tasks that
     *   don't involve direct UI manipulation or blocking I/O (for which
     *   [Dispatchers.IO] would be more appropriate).
     */
    @Singleton
    @Provides
    @Named("ApplicationScope")
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}