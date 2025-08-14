package com.example.campfire.core.di

import com.example.campfire.BuildConfig
import com.example.campfire.auth.data.remote.TokenRefreshAPIService
import com.example.campfire.core.data.network.AuthInterceptor
import com.example.campfire.core.data.network.TokenAuthenticator
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


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level =
                if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
        }
    }
    
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
    @Named("TokenRefreshRetrofit")
    fun provideTokenRefreshRetrofit(@Named("TokenRefreshClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideTokenRefreshApiService(
        @Named("TokenRefreshRetrofit") retrofit: Retrofit
    ): TokenRefreshAPIService {
        return retrofit.create(TokenRefreshAPIService::class.java)
    }
    
    // This client will be used for most authenticated API calls
    @Provides
    @Singleton
    @Named("AuthenticatedClient")
    fun provideAuthenticatedOkHttpClient(
        authInterceptor: AuthInterceptor,           // Injects the AuthInterceptor
        tokenAuthenticator: TokenAuthenticator,     // Existing TokenAuthenticator
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)        // Adds the token to outgoing requests
            .addInterceptor(loggingInterceptor)     // Logs the request/response
            .authenticator(tokenAuthenticator)      // Handles 401s to refresh the token
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    // Retrofit for general authenticated API calls, using the "AuthenticatedClient"
    @Provides
    @Singleton
    @Named("AuthenticatedRetrofit")
    fun provideRetrofit(@Named("AuthenticatedClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}