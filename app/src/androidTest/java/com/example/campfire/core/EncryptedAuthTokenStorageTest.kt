package com.example.campfire.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.campfire.core.data.EncryptedAuthTokenStorage
import com.example.campfire.core.data.auth.AuthTokens
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedAuthTokenStorageTest {
    
    private lateinit var context: Context
    private lateinit var tokenStorage: EncryptedAuthTokenStorage
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        // Potentially use a test-specific SharedPreferences name if needed
        // or ensure it's cleared. For simplicity, this uses the default.
        tokenStorage = EncryptedAuthTokenStorage(context)
        
        // Clear tokens before each test to ensure a clean state
        runBlocking { tokenStorage.clearTokens() }
    }
    
    @After
    fun tearDown() {
        // Clear tokens after each test
        runBlocking { tokenStorage.clearTokens() }
    }
    
    @Test
    fun saveAndGetTokens_validTokens_retrievesSuccessfully() = runBlocking {
        val tokensToSave = AuthTokens("test_access_token", "test_refresh_token")
        tokenStorage.saveTokens(tokensToSave)
        
        val retrievedTokens = tokenStorage.getTokens()
        Assert.assertNotNull(retrievedTokens)
        Assert.assertEquals(tokensToSave.accessToken, retrievedTokens?.accessToken)
        Assert.assertEquals(tokensToSave.refreshToken, retrievedTokens?.refreshToken)
    }
    
    @Test
    fun getTokens_whenNoTokensSaved_returnsNull() = runBlocking {
        val retrievedTokens = tokenStorage.getTokens()
        Assert.assertNull(retrievedTokens)
    }
    
    @Test
    fun clearTokens_removesSavedTokens() = runBlocking {
        val tokensToSave = AuthTokens("access", "refresh")
        tokenStorage.saveTokens(tokensToSave)
        Assert.assertNotNull(tokenStorage.getTokens()) // Ensure they were saved
        
        tokenStorage.clearTokens()
        Assert.assertNull(tokenStorage.getTokens())
    }
    
    // JD TODO: ... more tests for saving null, overwriting, etc.
}