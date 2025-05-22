package com.example.campfire.core.data.network

import com.example.campfire.auth.data.remote.TokenRefreshApiService
import com.example.campfire.core.data.auth.AuthTokenStorage
import io.mockk.every
import io.mockk.mockk
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test


class TokenAuthenticatorTest {
    
    private lateinit var mockTokenStorage: AuthTokenStorage
    private lateinit var mockTokenRefreshApiServiceLazy: dagger.Lazy<TokenRefreshApiService>
    private lateinit var mockTokenRefreshApiService: TokenRefreshApiService // The actual service instance
    
    private lateinit var tokenAuthenticator: TokenAuthenticator
    
    @Before
    fun setUp() {
        mockTokenStorage = mockk(relaxed = true) // relaxed = true to avoid mocking all methods
        mockTokenRefreshApiService = mockk(relaxed = true)
        mockTokenRefreshApiServiceLazy = mockk()
        every { mockTokenRefreshApiServiceLazy.get() } returns mockTokenRefreshApiService
        
        tokenAuthenticator = TokenAuthenticator(mockTokenStorage, mockTokenRefreshApiServiceLazy)
    }
    
    // Helper function to create a Response for testing.
    private fun createMockResponse(priorResponse: Response? = null): Response {
        val mockRequest = mockk<Request>(relaxed = true)
        return Response.Builder()
            .request(mockRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(200) // Default success code, doesn't matter much for this test
            .message("OK")
            .priorResponse(priorResponse) // Link to prior response
            .build()
    }
    
    //region ResponseCount Tests
    
    @Test
    fun `responseCount with no priorResponse returns 1`() {
        val response = createMockResponse(priorResponse = null)
        // Access responseCount via reflection if it's private,
        // or make it internal/public for testing, or test through authenticate.
        val method =
            TokenAuthenticator::class.java.getDeclaredMethod("responseCount", Response::class.java)
        method.isAccessible = true
        val count = method.invoke(tokenAuthenticator, response) as Int
        
        assertEquals(1, count)
    }
    
    @Test
    fun `responseCount with one priorResponse returns 2`() {
        val priorResponse1 = createMockResponse(priorResponse = null)
        val currentResponse = createMockResponse(priorResponse = priorResponse1)
        
        val method =
            TokenAuthenticator::class.java.getDeclaredMethod("responseCount", Response::class.java)
        method.isAccessible = true
        val count = method.invoke(tokenAuthenticator, currentResponse) as Int
        
        assertEquals(2, count)
    }
    
    @Test
    fun `responseCount with multiple priorResponses returns correct count`() {
        val priorResponse1 = createMockResponse(priorResponse = null)
        val priorResponse2 = createMockResponse(priorResponse = priorResponse1)
        val priorResponse3 = createMockResponse(priorResponse = priorResponse2)
        val currentResponse = createMockResponse(priorResponse = priorResponse3)
        
        val method =
            TokenAuthenticator::class.java.getDeclaredMethod("responseCount", Response::class.java)
        method.isAccessible = true
        val count = method.invoke(tokenAuthenticator, currentResponse) as Int
        
        assertEquals(
            4,
            count
        ) // priorResponse3, priorResponse2, priorResponse1, currentResponse itself
    }
    
    @Test
    fun `responseCount with five priorResponses returns 6`() {
        var latestResponse: Response? = null
        for (i in 0 until 5) { // Create a chain of 5 prior responses
            latestResponse = createMockResponse(priorResponse = latestResponse)
        }
        val currentResponse =
            createMockResponse(priorResponse = latestResponse) // The 6th response in the chain
        
        val method =
            TokenAuthenticator::class.java.getDeclaredMethod("responseCount", Response::class.java)
        method.isAccessible = true
        val count = method.invoke(tokenAuthenticator, currentResponse) as Int
        
        assertEquals(6, count)
    }
    
    //endregion
}