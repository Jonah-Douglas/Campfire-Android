package com.example.campfire.onboarding.di

import com.example.campfire.onboarding.profile_setup.data.remote.ProfileSetupAPIService
import com.example.campfire.onboarding.profile_setup.data.repository.ProfileSetupRepositoryImpl
import com.example.campfire.onboarding.profile_setup.domain.repository.ProfileSetupRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton


@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingRepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(
        onboardingRepositoryImpl: ProfileSetupRepositoryImpl
    ): ProfileSetupRepository
}

@Module
@InstallIn(SingletonComponent::class)
object OnboardingNetworkModule {
    
    @Provides
    @Singleton
    fun provideOnboardingAPIService(
        @Named("AuthenticatedRetrofit") retrofit: Retrofit
    ): ProfileSetupAPIService {
        return retrofit.create(ProfileSetupAPIService::class.java)
    }
}