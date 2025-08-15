package com.example.campfire.core.di

import com.example.campfire.core.data.mapper.DataTypeMapper
import com.example.campfire.core.data.mapper.DefaultDataTypeMapper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


/**
 * Hilt Module responsible for providing bindings for mapper interfaces
 * to their concrete implementations.
 *
 * This module ensures that when a dependency on a mapper interface (e.g., [DataTypeMapper])
 * is requested, Hilt knows which concrete implementation (e.g., [DefaultDataTypeMapper])
 * to provide.
 */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class MapperModule {
    
    /**
     * Binds the [DataTypeMapper] interface to its concrete implementation [DefaultDataTypeMapper].
     *
     * This tells Hilt that whenever an instance of [DataTypeMapper] is required,
     * an instance of [DefaultDataTypeMapper] should be provided.
     *
     * @param defaultDataTypeMapper The concrete implementation of [DataTypeMapper].
     *                              Hilt will know how to provide this if [DefaultDataTypeMapper]
     *                              has an `@Inject` constructor or is provided by another module.
     * @return An instance of [DataTypeMapper].
     */
    @Binds
    @Singleton
    abstract fun bindDataTypeMapper(
        defaultDataTypeMapper: DefaultDataTypeMapper
    ): DataTypeMapper
}