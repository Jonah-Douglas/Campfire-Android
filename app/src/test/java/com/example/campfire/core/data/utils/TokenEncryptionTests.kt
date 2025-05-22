package com.example.campfire.core.data.utils

import android.util.Base64
import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey


class CryptoUtilsTest {
    
    private lateinit var mockSecretKey: SecretKey
    
    @Before
    fun setUp() {
        // Mock Android specific classes
        mockkStatic(Base64::class)
        mockkStatic(Log::class)
        
        // Capture log calls instead of actually logging to Android Logcat
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        
        
        // Generate a real AES key for testing encryption/decryption logic
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256) // AES-256
        mockSecretKey = keyGenerator.generateKey()
        
        // Define behavior for Base64 encoding/decoding
        // We need to provide actual implementations for these as they are part of the core logic
        every { Base64.encodeToString(any<ByteArray>(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg())
        }
        every { Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<String>())
        }
        // Handle Base64.decode that might throw IllegalArgumentException for invalid input
        every {
            Base64.decode(
                match<String> { it.contains("!") },
                any()
            )
        } throws IllegalArgumentException("Test invalid Base64")
    }
    
    @After
    fun tearDown() {
        unmockkStatic(Base64::class)
        unmockkStatic(Log::class)
    }
    
    //region EncryptData Tests
    
    @Test
    fun `encryptData with null data returns null`() {
        val result = encryptData(null, mockSecretKey)
        assertNull(result)
    }
    
    @Test
    fun `encryptData with empty data returns null`() {
        val result = encryptData("", mockSecretKey)
        assertNull(result)
    }
    
    @Test
    fun `encryptData successfully encrypts valid non-empty string`() {
        val originalData = "This is a secret message!"
        val encrypted = encryptData(originalData, mockSecretKey)
        
        assertNotNull("Encrypted data should not be null", encrypted)
        assertNotEquals(
            "Encrypted data should not be the same as original",
            originalData,
            encrypted
        )
        assertTrue("Encrypted string should not be empty", encrypted!!.isNotEmpty())
        
        // Verify Base64.encodeToString was called
        verify { Base64.encodeToString(any(), Base64.DEFAULT) }
    }
    
    //endregion
    
    //region DecryptData Tests
    
    @Test
    fun `decryptData with null encryptedData returns null`() {
        val result = decryptData(null, mockSecretKey)
        assertNull(result)
    }
    
    @Test
    fun `decryptData with empty encryptedData returns null`() {
        val result = decryptData("", mockSecretKey)
        assertNull(result)
    }
    
    @Test
    fun `decryptData successfully decrypts valid encrypted string`() {
        val originalData = "Top secret information!"
        // Encrypt first (using the real crypto logic but with mocked Base64)
        val plainCipher = javax.crypto.Cipher.getInstance(AES_MODE)
        plainCipher.init(javax.crypto.Cipher.ENCRYPT_MODE, mockSecretKey)
        val iv = plainCipher.iv
        assertNotNull(iv)
        assertEquals(IV_SIZE, iv.size)
        val encryptedBytes = plainCipher.doFinal(originalData.toByteArray(Charsets.UTF_8))
        val combinedForTest = iv + encryptedBytes
        val encryptedDataForTest = java.util.Base64.getEncoder().encodeToString(combinedForTest)
        
        
        // Now, test decryption
        val decrypted = decryptData(encryptedDataForTest, mockSecretKey)
        
        assertNotNull("Decrypted data should not be null", decrypted)
        assertEquals("Decrypted data should match original", originalData, decrypted)
        verify { Base64.decode(encryptedDataForTest, Base64.DEFAULT) }
    }
    
    @Test
    fun `decryptData returns null for data too short to contain IV`() {
        // Create a string that, when Base64 decoded, is shorter than IV_SIZE
        val shortData = java.util.Base64.getEncoder().encodeToString(ByteArray(IV_SIZE - 1))
        val decrypted = decryptData(shortData, mockSecretKey)
        assertNull(decrypted)
        verify { Log.e("DecryptData", "Encrypted data is too short to contain a valid IV.") }
    }
    
    @Test
    fun `decryptData returns null for invalid Base64 input`() {
        val invalidBase64String =
            "This is not valid Base64!" // Contains '!' which our mock setup will throw on
        val decrypted = decryptData(invalidBase64String, mockSecretKey)
        assertNull(decrypted)
        verify {
            Log.e(
                "DecryptData",
                "Invalid Base64 string provided for decryption: Test invalid Base64",
                any()
            )
        }
    }
    
    @Test
    fun `decryptData returns null on AEADBadTagException (tampered data or wrong key)`() {
        val originalData = "Original good data"
        var encryptedForTest =
            encryptData(originalData, mockSecretKey) // Use our function to encrypt
        assertNotNull(encryptedForTest)
        
        // Tamper with the encrypted data (modify a byte of the Base64 decoded data)
        val combinedBytes = java.util.Base64.getDecoder().decode(encryptedForTest)
        if (combinedBytes.size > IV_SIZE) { // Ensure there's a payload to tamper
            combinedBytes[combinedBytes.size - 1] =
                (combinedBytes[combinedBytes.size - 1] + 1).toByte() // Flip a bit
        }
        val tamperedEncryptedData = java.util.Base64.getEncoder().encodeToString(combinedBytes)
        
        val decrypted = decryptData(tamperedEncryptedData, mockSecretKey)
        assertNull(decrypted)
        verify {
            Log.e(
                "DecryptData",
                match { it.startsWith("Decryption failed due to AEADBadTagException") },
                any()
            )
        }
    }
    
    @Test
    fun `decryptData with a different key returns null`() {
        val originalData = "Sensitive Data"
        val encrypted = encryptData(originalData, mockSecretKey)
        assertNotNull(encrypted)
        
        // Generate a different key
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        val wrongKey = keyGenerator.generateKey()
        
        val decrypted = decryptData(encrypted, wrongKey)
        assertNull("Decryption with wrong key should fail and return null", decrypted)
        verify {
            Log.e(
                "DecryptData",
                match { it.startsWith("Decryption failed due to AEADBadTagException") },
                any()
            )
        }
    }
    
    //endregion
    
    //region EncryptData and DecryptData Integration
    
    @Test
    fun `encryptData then decryptData returns original string`() {
        val originalData = "Hello, World! This is a test. 12345!@#$%^"
        val encrypted = encryptData(originalData, mockSecretKey)
        assertNotNull("Encrypted string should not be null", encrypted)
        
        val decrypted = decryptData(encrypted, mockSecretKey)
        assertNotNull("Decrypted string should not be null", decrypted)
        assertEquals("Decrypted string should match original", originalData, decrypted)
    }
    
    @Test
    fun `encryptData with various string contents then decryptData`() {
        val testStrings = listOf(
            "simple",
            "A bit longer string with spaces and punctuation.",
            "{\"jsonKey\": \"jsonValue\", \"number\": 123}",
            " লাইন স্প্যান পরীক্ষা ", // Bengali text
            "emoji 😂👍🎉",
            "!@#$%^&*()_+=-`~[]{}|;':\",./<>?"
        )
        
        for (originalData in testStrings) {
            val encrypted = encryptData(originalData, mockSecretKey)
            assertNotNull("Encrypted string for '$originalData' should not be null", encrypted)
            
            val decrypted = decryptData(encrypted, mockSecretKey)
            assertNotNull("Decrypted string for '$originalData' should not be null", decrypted)
            assertEquals(
                "Decrypted string for '$originalData' should match original",
                originalData,
                decrypted
            )
        }
    }
    
    //endregion
}