package com.example.campfire.core.di

import android.content.Context
import com.example.campfire.auth.data.remote.AuthApiService
import com.example.campfire.core.data.EncryptedAuthTokenStorage
import com.example.campfire.core.data.auth.AuthTokenStorage
import com.example.campfire.core.data.network.TokenAuthenticator
import com.example.campfire.profile.data.remote.ProfileApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton


// JD TODO: Confirm one or the other urls is correct for base
// const val BASE_URL = "http://10.0.2.2:8000/"
const val BASE_URL = "http://127.0.0.1:8000" // Or from BuildConfig

// JD TODO: Confirm Suppressions are necessary
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideAuthTokenStorage(context: Context): AuthTokenStorage {
        return EncryptedAuthTokenStorage(context)
    }
    
    // OkHttpClient for general API calls (uses TokenAuthenticator)
    @Provides
    @Singleton
    @Named("AuthenticatorClient")
    fun provideOkHttpClient(
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    // OkHttpClient for the TokenRefreshApiService (DOES NOT use TokenAuthenticator)
    @Provides
    @Singleton
    @Named("TokenRefreshClient")
    fun provideTokenRefreshOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return loggingInterceptor
    }
    
    @Provides
    @Singleton
    @Named("AuthenticatedClient")
    fun provideAuthenticatedOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    // Retrofit for general authenticated API calls
    @Provides
    @Singleton
    fun provideRetrofit(@Named("AuthenticatedClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // Retrofit for the TokenRefreshApiService
    @Provides
    @Singleton
    @Named("TokenRefreshRetrofit")
    fun provideTokenRefreshRetrofit(@Named("TokenRefreshClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL) // Ensure this is correct for your refresh endpoint
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideProfileApiService(@Named("TokenRefreshRetrofit") retrofit: Retrofit): ProfileApiService {
        return retrofit.create(ProfileApiService::class.java)
    }
}