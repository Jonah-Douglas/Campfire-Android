package com.example.campfire.core.di

import com.example.campfire.BuildConfig
import com.example.campfire.auth.data.remote.TokenRefreshAPIService
import com.example.campfire.core.data.network.AuthInterceptor
import com.example.campfire.core.data.network.TokenAuthenticator
import com.example.campfire.core.di.NetworkModule.provideAuthenticatedOkHttpClient
import com.example.campfire.core.di.NetworkModule.provideTokenRefreshOkHttpClient
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


/**
 * Hilt Module dedicated to providing network-related dependencies,
 * primarily [OkHttpClient] and [Retrofit] instances, along with their
 * necessary components like interceptors and API service interfaces.
 *
 * This module configures two main sets of network clients:
 * 1. A client specifically for token refresh operations ([TokenRefreshClient], [TokenRefreshRetrofit]).
 *    This client is minimal and does not include the [AuthInterceptor] or [TokenAuthenticator]
 *    to avoid circular dependencies or issues during the refresh process itself.
 * 2. A primary client for general authenticated API calls ([AuthenticatedClient], [AuthenticatedRetrofit]).
 *    This client is equipped with an [AuthInterceptor] to add tokens to outgoing requests
 *    and a [TokenAuthenticator] to handle automatic token refresh on 401 errors.
 *
 * All provided dependencies are scoped as [Singleton] as they are typically configured
 * once per application lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * Provides a singleton [HttpLoggingInterceptor] for logging HTTP request and response data.
     * The logging level is set to [HttpLoggingInterceptor.Level.BODY] for `DEBUG` builds
     * and [HttpLoggingInterceptor.Level.NONE] for release builds to avoid leaking sensitive
     * information in production.
     *
     * @return A configured [HttpLoggingInterceptor].
     */
    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level =
                if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
        }
    }
    
    // --- Token Refresh Network Stack ---
    
    /**
     * Provides a dedicated [OkHttpClient] instance specifically for token refresh API calls.
     *
     * This client is named "TokenRefreshClient" and includes:
     * - The common [HttpLoggingInterceptor].
     * - Standard connection, write, and read timeouts.
     *
     * It **does not** include the [AuthInterceptor] or [TokenAuthenticator]
     * to prevent issues (like circular calls) during the token refresh process itself.
     *
     * @param loggingInterceptor The shared HTTP logging interceptor.
     * @return An [OkHttpClient] configured for token refresh operations.
     */
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
    
    /**
     * Provides a [Retrofit] instance configured for token refresh API calls,
     * using the [OkHttpClient] provided by [provideTokenRefreshOkHttpClient].
     *
     * This Retrofit instance is named "TokenRefreshRetrofit" and is configured with:
     * - The application's base URL from [BuildConfig.BASE_URL].
     * - The "TokenRefreshClient" [OkHttpClient].
     * - [GsonConverterFactory] for JSON serialization/deserialization.
     *
     * @param okHttpClient The [OkHttpClient] specifically configured for token refresh,
     *                     injected with the "@Named("TokenRefreshClient")" qualifier.
     * @return A [Retrofit] instance for token refresh API services.
     */
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
    
    /**
     * Provides an instance of the [TokenRefreshAPIService] used for making token refresh calls.
     * This service is created using the "TokenRefreshRetrofit" instance.
     *
     * @param retrofit The [Retrofit] instance configured for token refresh,
     *                 injected with the "@Named("TokenRefreshRetrofit")" qualifier.
     * @return An implementation of [TokenRefreshAPIService].
     */
    @Provides
    @Singleton
    fun provideTokenRefreshApiService(
        @Named("TokenRefreshRetrofit") retrofit: Retrofit
    ): TokenRefreshAPIService {
        return retrofit.create(TokenRefreshAPIService::class.java)
    }
    
    // --- Authenticated API Network Stack ---
    
    /**
     * Provides the primary [OkHttpClient] instance for most authenticated API calls throughout the application.
     *
     * This client is named "AuthenticatedClient" and is configured with:
     * - [AuthInterceptor]: Adds the Authorization header (Bearer token) to outgoing requests.
     * - [TokenAuthenticator]: Handles 401 Unauthorized responses by attempting to refresh the token.
     * - The common [HttpLoggingInterceptor].
     * - Standard connection, write, and read timeouts.
     *
     * @param authInterceptor The interceptor responsible for adding auth tokens to requests.
     * @param tokenAuthenticator The authenticator responsible for handling token refresh logic.
     * @param loggingInterceptor The shared HTTP logging interceptor.
     * @return An [OkHttpClient] configured for general authenticated API calls.
     */
    @Provides
    @Singleton
    @Named("AuthenticatedClient")
    fun provideAuthenticatedOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator) // Handles 401s to refresh the token
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Provides the primary [Retrofit] instance for general authenticated API calls,
     * using the [OkHttpClient] provided by [provideAuthenticatedOkHttpClient].
     *
     * It is configured with:
     * - The application's base URL from [BuildConfig.BASE_URL].
     * - The "AuthenticatedClient" [OkHttpClient].
     * - [GsonConverterFactory] for JSON serialization/deserialization.
     *
     * API services that require automatic token handling should be created using this Retrofit instance.
     *
     * @param okHttpClient The [OkHttpClient] configured for authenticated calls,
     *                     injected with the "@Named("AuthenticatedClient")" qualifier.
     * @return A [Retrofit] instance for general authenticated API services.
     */
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