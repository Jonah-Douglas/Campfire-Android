package com.example.campfire.core.data.utils

import android.security.keystore.KeyProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import javax.crypto.SecretKey


@RunWith(AndroidJUnit4::class)
class GetOrCreateSecretKeyTest {
    
    private lateinit var keyStore: KeyStore
    
    @Before
    fun setUp() {
        keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        
        deleteTestKeyAlias()
    }
    
    @After
    fun tearDown() {
        deleteTestKeyAlias()
    }
    
    private fun deleteTestKeyAlias() {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }
    
    //region GetOrCreateSecretKey Tests
    
    @Test
    fun getOrCreateSecretKey_whenKeyDoesNotExist_createsAndReturnsNewKey() {
        // Arrange: Ensured by setUp that the key does not exist
        
        // Act
        val secretKey = getOrCreateSecretKey()
        
        // Assert
        assertNotNull("SecretKey should not be null", secretKey)
        assertEquals(
            "Algorithm should be AES",
            KeyProperties.KEY_ALGORITHM_AES,
            secretKey.algorithm
        )
        assertTrue("Key should now exist in KeyStore", keyStore.containsAlias(KEY_ALIAS))
        
        // Optionally, retrieve the key again to verify it's the same instance or equivalent
        val retrievedKey = keyStore.getKey(KEY_ALIAS, null)
        assertNotNull("Key should be retrievable from KeyStore", retrievedKey)
        assertTrue("Retrieved key should be a SecretKey", retrievedKey is SecretKey)
        assertEquals(
            "Retrieved key should be equivalent to the created one",
            secretKey,
            retrievedKey as SecretKey
        )
    }
    
    @Test
    fun getOrCreateSecretKey_whenKeyExists_returnsExistingKey() {
        // Arrange: First, create the key
        val initialKey = getOrCreateSecretKey()
        assertNotNull("Initial key creation failed", initialKey)
        assertTrue(
            "Key should exist after initial creation",
            keyStore.containsAlias(KEY_ALIAS)
        )
        
        // Act: Call the function again
        val subsequentKey = getOrCreateSecretKey()
        
        // Assert
        assertNotNull("Subsequent SecretKey should not be null", subsequentKey)
        assertEquals(
            "Should return the same key instance (or an equivalent key)",
            initialKey,
            subsequentKey
        )
        assertEquals(
            "Algorithm should be AES",
            KeyProperties.KEY_ALGORITHM_AES,
            subsequentKey.algorithm
        )
    }
    
    @Test
    fun getOrCreateSecretKey_createsKeyWithCorrectProperties() {
        // Arrange: Key does not exist (ensured by setUp)
        
        // Act
        val secretKey = getOrCreateSecretKey() // This creates the key
        
        // Assert: Verify the key in the keystore has the intended properties.
        assertNotNull(secretKey)
        val retrievedEntry = keyStore.getEntry(KEY_ALIAS, null)
        assertTrue("Entry should be a SecretKeyEntry", retrievedEntry is KeyStore.SecretKeyEntry)
        
        val keyFromEntry = (retrievedEntry as KeyStore.SecretKeyEntry).secretKey
        assertEquals(
            "Algorithm should be AES",
            KeyProperties.KEY_ALGORITHM_AES,
            keyFromEntry.algorithm
        )
    }
    
    @Test
    fun getOrCreateSecretKey_multipleCallsReturnSameKeyInstanceOrEquivalent() {
        // Arrange (ensured by setUp that key doesn't exist initially)
        
        // Act
        val key1 = getOrCreateSecretKey()
        val key2 = getOrCreateSecretKey()
        val key3 = getOrCreateSecretKey()
        
        // Assert
        assertNotNull(key1)
        assertEquals("First and second key should be the same", key1, key2)
        assertEquals("Second and third key should be the same", key2, key3)
    }
    
    //endregion
}