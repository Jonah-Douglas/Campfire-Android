package com.example.campfire.auth.data.remote.dto.response

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test


class MessageResponseTest {
    
    @Test
    fun `messageResponse instantiation and property access`() {
        val expectedMessage = "Test successful"
        val response = MessageResponse(message = expectedMessage)
        assertEquals(expectedMessage, response.message)
    }
    
    @Test
    fun `messageResponse equality for same messages`() {
        val message = "Are Equal"
        val response1 = MessageResponse(message = message)
        val response2 = MessageResponse(message = message)
        assertEquals(response1, response2) // Tests equals()
        assertEquals(response1.hashCode(), response2.hashCode()) // Tests hashCode() consistency
    }
    
    @Test
    fun `messageResponse inequality for different messages`() {
        val response1 = MessageResponse(message = "Message A")
        val response2 = MessageResponse(message = "Message B")
        assertNotEquals(response1, response2) // Tests equals()
    }
    
    @Test
    fun `messageResponse copy creates identical instance`() {
        val originalMessage = "Original message"
        val originalResponse = MessageResponse(message = originalMessage)
        val copiedResponse = originalResponse.copy()
        assertEquals(originalResponse, copiedResponse)
    }
    
    @Test
    fun `messageResponse copy can change property`() {
        // Less impactful with a single property, but shows the mechanism
        val originalResponse = MessageResponse(message = "Initial")
        val updatedMessage = "Updated message"
        val copiedResponse = originalResponse.copy(message = updatedMessage)
        
        assertEquals(updatedMessage, copiedResponse.message)
        assertNotEquals(originalResponse, copiedResponse)
    }
    
    @Test
    fun `messageResponse toString contains message`() {
        // Basic check, might be too brittle if format changes often
        val message = "This is a test"
        val response = MessageResponse(message = message)
        val responseString = response.toString()
        assert(responseString.contains(message))
    }
}