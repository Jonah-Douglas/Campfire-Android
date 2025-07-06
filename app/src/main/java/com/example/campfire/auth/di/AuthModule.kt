package com.example.campfire.auth.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.example.campfire.auth.data.remote.AuthApiService
import com.example.campfire.auth.data.repository.AuthRepositoryImpl
import com.example.campfire.auth.domain.repository.AuthRepository
import com.example.campfire.core.domain.SessionInvalidator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton


@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindSessionInvalidator(
        authRepositoryImpl: AuthRepositoryImpl
    ): SessionInvalidator
    
    companion object {
        @Provides
        @Singleton
        fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
            return retrofit.create(AuthApiService::class.java)
        }
        
        @Provides
        @Singleton
        fun provideSharedPreferences(application: Application): SharedPreferences {
            return application.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        }
    }
}