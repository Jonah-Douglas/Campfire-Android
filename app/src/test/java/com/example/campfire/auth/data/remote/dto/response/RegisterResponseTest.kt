package com.example.campfire.auth.data.remote.dto.response

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test


class RegisterResponseTest {
    
    @Test
    fun `registerResponse instantiation and property access`() {
        val expectedUserId = "user_abc_123"
        val expectedMessage = "Registration successful. Please check your email."
        
        val response = RegisterResponse(
            userId = expectedUserId,
            message = expectedMessage
        )
        
        assertEquals(expectedUserId, response.userId)
        assertEquals(expectedMessage, response.message)
    }
    
    @Test
    fun `registerResponse equality for same userId and message`() {
        val userId = "user_test_001"
        val message = "User registered successfully"
        
        val response1 = RegisterResponse(userId = userId, message = message)
        val response2 = RegisterResponse(userId = userId, message = message)
        
        assertEquals(response1, response2) // Tests equals()
        assertEquals(response1.hashCode(), response2.hashCode()) // Tests hashCode() consistency
    }
    
    @Test
    fun `registerResponse inequality for different userIds`() {
        val message = "User registered"
        val response1 = RegisterResponse(userId = "user_A", message = message)
        val response2 = RegisterResponse(userId = "user_B", message = message)
        
        assertNotEquals(response1, response2)
    }
    
    @Test
    fun `registerResponse inequality for different messages`() {
        val userId = "common_user_id"
        val response1 = RegisterResponse(userId = userId, message = "Message One")
        val response2 = RegisterResponse(userId = userId, message = "Message Two")
        
        assertNotEquals(response1, response2)
    }
    
    @Test
    fun `registerResponse inequality for different userId and message`() {
        val response1 = RegisterResponse(userId = "user1", message = "MessageAlpha")
        val response2 = RegisterResponse(userId = "user2", message = "MessageBeta")
        
        assertNotEquals(response1, response2)
    }
    
    @Test
    fun `registerResponse copy creates identical instance`() {
        val originalUserId = "original_user_id_789"
        val originalMessage = "Original registration message"
        val originalResponse = RegisterResponse(userId = originalUserId, message = originalMessage)
        
        val copiedResponse = originalResponse.copy()
        
        assertEquals(originalResponse, copiedResponse)
    }
    
    @Test
    fun `registerResponse copy can change userId`() {
        val originalMessage = "Consistent message"
        val originalResponse = RegisterResponse(userId = "initial_uid", message = originalMessage)
        val updatedUserId = "updated_user_identifier"
        
        val copiedResponse = originalResponse.copy(userId = updatedUserId)
        
        assertEquals(updatedUserId, copiedResponse.userId)
        assertEquals(originalMessage, copiedResponse.message) // Ensure other property is preserved
        assertNotEquals(originalResponse, copiedResponse)
    }
    
    @Test
    fun `registerResponse copy can change message`() {
        val originalUserId = "consistent_uid"
        val originalResponse =
            RegisterResponse(userId = originalUserId, message = "initial_reg_message")
        val updatedMessage = "Updated registration status"
        
        val copiedResponse = originalResponse.copy(message = updatedMessage)
        
        assertEquals(originalUserId, copiedResponse.userId) // Ensure other property is preserved
        assertEquals(updatedMessage, copiedResponse.message)
        assertNotEquals(originalResponse, copiedResponse)
    }
    
    @Test
    fun `registerResponse copy can change all properties`() {
        val originalResponse = RegisterResponse(userId = "uid_v1", message = "Registration_v1")
        val updatedUserId = "uid_v2"
        val updatedMessage = "Registration_v2"
        
        val copiedResponse = originalResponse.copy(userId = updatedUserId, message = updatedMessage)
        
        assertEquals(updatedUserId, copiedResponse.userId)
        assertEquals(updatedMessage, copiedResponse.message)
        assertNotEquals(originalResponse, copiedResponse)
    }
    
    @Test
    fun `registerResponse toString contains userId and message`() {
        val userId = "test_uid_456"
        val message = "This is a registration test message"
        val response = RegisterResponse(userId = userId, message = message)
        val responseString = response.toString()
        
        assert(responseString.contains(userId))
        assert(responseString.contains(message))
    }
}