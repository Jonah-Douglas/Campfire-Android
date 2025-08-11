package com.example.campfire.core.data.network

import com.example.campfire.core.data.auth.IAuthTokenManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: IAuthTokenManager
) : Interceptor {
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val accessToken = tokenManager.getCurrentTokens()?.accessToken
        
        val requestBuilder = originalRequest.newBuilder()
        if (accessToken != null) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }
        
        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}