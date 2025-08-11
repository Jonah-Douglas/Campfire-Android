package com.example.campfire.core.di

import com.example.campfire.core.data.mapper.DataTypeMapper
import com.example.campfire.core.data.mapper.DefaultDataTypeMapper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class MapperModule {
    
    @Binds
    @Singleton
    abstract fun bindDataTypeMapper(
        defaultDataTypeMapper: DefaultDataTypeMapper
    ): DataTypeMapper
}