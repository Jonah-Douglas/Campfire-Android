package com.example.campfire.core.di

import com.example.campfire.BuildConfig
import com.example.campfire.auth.data.remote.TokenRefreshApiService
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
    
    // OkHttpClient for the TokenRefreshApiService (DOES NOT use AuthInterceptor or TokenAuthenticator)
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
    
    // This client will be used for most authenticated API calls
    @Provides
    @Singleton
    @Named("AuthenticatedClient")
    fun provideAuthenticatedOkHttpClient(
        authInterceptor: AuthInterceptor,           // Injects the AuthInterceptor
        tokenAuthenticator: TokenAuthenticator,     // Your existing TokenAuthenticator
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
    
    // JD TODO: Might be able to remove this- confirm authenticated client handles refresh
    // OkHttpClient for general API calls (uses TokenAuthenticator)
//    @Provides
//    @Singleton
//    @Named("AuthenticatorClient")
//    fun provideOkHttpClient(
//        tokenAuthenticator: TokenAuthenticator,
//        loggingInterceptor: HttpLoggingInterceptor
//    ): OkHttpClient {
//        return OkHttpClient.Builder()
//            .addInterceptor(loggingInterceptor)
//            .authenticator(tokenAuthenticator)
//            .connectTimeout(10, TimeUnit.SECONDS)
//            .writeTimeout(10, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS)
//            .build()
//    }
    
    // Retrofit for general authenticated API calls, using the "AuthenticatedClient"
    @Provides
    @Singleton
    fun provideRetrofit(@Named("AuthenticatedClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
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
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideTokenRefreshApiService(
        @Named("TokenRefreshRetrofit") retrofit: Retrofit // Inject the Retrofit meant for token refresh
    ): TokenRefreshApiService {
        return retrofit.create(TokenRefreshApiService::class.java)
    }
    
    // JD TODO: Move this into its own module within the profile feature when I get to that
//    @Provides
//    @Singleton
//    fun provideProfileApiService(@Named("TokenRefreshRetrofit") retrofit: Retrofit): ProfileApiService {
//        return retrofit.create(ProfileApiService::class.java)
//    }
}