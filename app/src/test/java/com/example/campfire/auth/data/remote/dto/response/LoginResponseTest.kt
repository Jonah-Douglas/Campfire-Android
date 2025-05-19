package com.example.campfire.auth.data.remote.dto.response

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test


class LoginResponseTest {
    
    @Test
    fun `loginResponse instantiation and property access`() {
        val expectedToken = "sample_jwt_token_string"
        val expectedMessage = "Login successful"
        
        val response = LoginResponse(
            token = expectedToken,
            message = expectedMessage
        )
        
        assertEquals(expectedToken, response.token)
        assertEquals(expectedMessage, response.message)
    }
    
    @Test
    fun `loginResponse equality for same token and message`() {
        val token = "token123"
        val message = "User logged in"
        
        val response1 = LoginResponse(token = token, message = message)
        val response2 = LoginResponse(token = token, message = message)
        
        assertEquals(response1, response2) // Tests equals()
        assertEquals(response1.hashCode(), response2.hashCode()) // Tests hashCode() consistency
    }
    
    @Test
    fun `loginResponse inequality for different tokens`() {
        val message = "User logged in"
        val response1 = LoginResponse(token = "tokenABC", message = message)
        val response2 = LoginResponse(token = "tokenXYZ", message = message)
        
        assertNotEquals(response1, response2)
    }
    
    @Test
    fun `loginResponse inequality for different messages`() {
        val token = "common_token"
        val response1 = LoginResponse(token = token, message = "Message Alpha")
        val response2 = LoginResponse(token = token, message = "Message Beta")
        
        assertNotEquals(response1, response2)
    }
    
    @Test
    fun `loginResponse inequality for different token and message`() {
        val response1 = LoginResponse(token = "token1", message = "Message1")
        val response2 = LoginResponse(token = "token2", message = "Message2")
        
        assertNotEquals(response1, response2)
    }
    
    @Test
    fun `loginResponse copy creates identical instance`() {
        val originalToken = "original_token_value"
        val originalMessage = "Original success message"
        val originalResponse = LoginResponse(token = originalToken, message = originalMessage)
        
        val copiedResponse = originalResponse.copy()
        
        assertEquals(originalResponse, copiedResponse)
    }
    
    @Test
    fun `loginResponse copy can change token`() {
        val originalMessage = "Keep this message"
        val originalResponse = LoginResponse(token = "initial_token", message = originalMessage)
        val updatedToken = "updated_token_value"
        
        val copiedResponse = originalResponse.copy(token = updatedToken)
        
        assertEquals(updatedToken, copiedResponse.token)
        assertEquals(
            originalMessage,
            copiedResponse.message
        ) // Ensure other properties are preserved
        assertNotEquals(originalResponse, copiedResponse)
    }
    
    @Test
    fun `loginResponse copy can change message`() {
        val originalToken = "Keep this token"
        val originalResponse = LoginResponse(token = originalToken, message = "initial_message")
        val updatedMessage = "Updated status message"
        
        val copiedResponse = originalResponse.copy(message = updatedMessage)
        
        assertEquals(originalToken, copiedResponse.token) // Ensure other properties are preserved
        assertEquals(updatedMessage, copiedResponse.message)
        assertNotEquals(originalResponse, copiedResponse)
    }
    
    @Test
    fun `loginResponse copy can change all properties`() {
        val originalResponse = LoginResponse(token = "token_v1", message = "Message_v1")
        val updatedToken = "token_v2"
        val updatedMessage = "Message_v2"
        
        val copiedResponse = originalResponse.copy(token = updatedToken, message = updatedMessage)
        
        assertEquals(updatedToken, copiedResponse.token)
        assertEquals(updatedMessage, copiedResponse.message)
        assertNotEquals(originalResponse, copiedResponse)
    }
    
    @Test
    fun `loginResponse toString contains token and message`() {
        // Basic check, might be too brittle if format changes often
        val token = "test_token"
        val message = "This is a test message"
        val response = LoginResponse(token = token, message = message)
        val responseString = response.toString()
        
        assert(responseString.contains(token))
        assert(responseString.contains(message))
    }
}