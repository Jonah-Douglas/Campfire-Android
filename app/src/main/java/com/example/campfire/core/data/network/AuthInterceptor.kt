package com.example.campfire.core.data.network

import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.data.auth.IAuthTokenManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


/**
 * An OkHttp [Interceptor] responsible for automatically adding an
 * "Authorization" header with a Bearer token to outgoing network requests.
 *
 * This interceptor retrieves the current access token using the [IAuthTokenManager]
 * and attaches it to requests. If no access token is available, the request proceeds
 * without the Authorization header.
 *
 * It is a [Singleton] as it's configured once per OkHttpClient instance.
 *
 * @property tokenManager An instance of [IAuthTokenManager] used to retrieve
 *                        the current authentication tokens.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: IAuthTokenManager
) : Interceptor {
    
    /**
     * Intercepts an outgoing HTTP request to potentially add an Authorization header.
     *
     * It retrieves the current access token from the [tokenManager]. If an access token
     * is found, it's added to the request's "Authorization" header as a Bearer token.
     * The request then proceeds, either with or without the added header.
     *
     * This method relies on the synchronous [IAuthTokenManager.getCurrentTokens]
     * to avoid blocking the network thread while fetching the token. The token manager
     * implementation is expected to provide a cached token for this call.
     *
     * @param chain The [Interceptor.Chain] providing access to the [okhttp3.Request] and allowing
     *              the request to proceed.
     * @return The [Response] from the server, potentially after the request has been modified.
     * @throws IOException if an I/O error occurs during the network call.
     */
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        Firelog.v("Intercepting request for URL: ${originalRequest.url}")
        
        val accessToken = tokenManager.getCurrentTokens()?.accessToken
        Firelog.d("Access token found: ${accessToken != null}")
        
        val requestBuilder = originalRequest.newBuilder()
        
        if (accessToken != null) {
            Firelog.d("Adding Authorization header to request for ${originalRequest.url}")
            requestBuilder.header("Authorization", "Bearer $accessToken")
        } else {
            Firelog.d("No access token available. Proceeding without Authorization header for ${originalRequest.url}")
        }
        
        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}