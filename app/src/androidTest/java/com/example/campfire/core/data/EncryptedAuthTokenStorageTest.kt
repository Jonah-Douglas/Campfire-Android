package com.example.campfire.core.data

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
    
    // Helper to directly interact with SharedPreferences for verification or specific setups
    // This will interact with the *encrypted* preferences if tokenStorage uses EncryptedSharedPreferences
    private lateinit var rawPrefsInspector: SharedPreferences
    
    // A helper to interact with underlying SharedPreferences in a "raw" way for some test setups
    // if we need to simulate corruption or states not achievable via tokenStorage API.
    // For most cases, use tokenStorage.saveTokens() for setup.
    class RawPreferencesHelper(private val context: Context, private val prefsName: String) {
        private val plainPrefs: SharedPreferences = context.getSharedPreferences(
            prefsName + "_plain_for_test_inspection",
            Context.MODE_PRIVATE
        )
        
        // Saves UNENCRYPTED data to a SEPARATE SharedPreferences file for tests
        // that need to simulate scenarios where EncryptedAuthTokenStorage might
        // encounter pre-existing unencrypted data (e.g., migration) OR to
        // directly write malformed encrypted-like strings if needed.
        fun writePotentiallyCorruptibleString(key: String, value: String?) {
            // This writes to the *actual* SharedPreferences file EncryptedAuthTokenStorage uses.
            // Be careful with this. It's for simulating edge cases.
            val actualPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            actualPrefs.edit().putString(key, value).commit()
        }
        
        fun readRawString(key: String): String? {
            // Reads from the *actual* SharedPreferences file.
            val actualPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            return actualPrefs.getString(key, null)
        }
        
        fun clearAllRaw() {
            val actualPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            actualPrefs.edit().clear().commit()
            plainPrefs.edit().clear().commit()
        }
    }
    
    private lateinit var rawHelper: RawPreferencesHelper
    
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        tokenStorage = EncryptedAuthTokenStorage(context)
        
        rawPrefsInspector = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        rawHelper = RawPreferencesHelper(context, PREFS_NAME)
        
        // Clear preferences before each test using the storage's own mechanism and also ensure raw state is clear.
        runBlocking { tokenStorage.clearTokens() }
        rawHelper.clearAllRaw() // Ensures the underlying file is truly empty
    }
    
    @After
    fun tearDown() {
        // Clear tokens after each test using the storage's own mechanism
        runBlocking { tokenStorage.clearTokens() }
        rawHelper.clearAllRaw() // Clean up raw state
    }
    
    //region SaveTokens Tests
    
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
    fun saveTokens_withNullAccessToken_savesAndRetrievesRefreshTokenOnly() = runBlocking {
        val tokensToSave = AuthTokens(null, "test_refresh_token_only")
        tokenStorage.saveTokens(tokensToSave)
        
        val retrievedTokens = tokenStorage.getTokens()
        Assert.assertNotNull(retrievedTokens)
        Assert.assertNull(retrievedTokens?.accessToken)
        Assert.assertEquals(tokensToSave.refreshToken, retrievedTokens?.refreshToken)
        
        // Verify raw prefs to see if access token key is absent or stores a specific null marker
        // EncryptedSharedPreferences might remove the key if the value is null.
        rawPrefsInspector.getString(PREF_KEY_ACCESS_TOKEN, "key_not_found")
        // Depending on EncryptedSharedPreferences behavior for nulls,
        // it might remove the key or store an encrypted form of null/empty.
        // This assertion might need adjustment based on observed behavior.
        // A common behavior is to remove the key.
        val isAccessTokenKeyAbsentOrNullEncrypted =
            rawPrefsInspector.getString(PREF_KEY_ACCESS_TOKEN, null) == null ||
                    !rawPrefsInspector.contains(PREF_KEY_ACCESS_TOKEN)
        Assert.assertTrue(
            "Access token key should be absent or its value undecryptable to non-null",
            isAccessTokenKeyAbsentOrNullEncrypted
        )
        Assert.assertNotNull(
            "Refresh token should exist in raw prefs",
            rawPrefsInspector.getString(PREF_KEY_REFRESH_TOKEN, null)
        )
    }
    
    @Test
    fun saveTokens_withNullRefreshToken_savesAndRetrievesAccessTokenOnly() = runBlocking {
        val tokensToSave = AuthTokens("test_access_token_only", null)
        tokenStorage.saveTokens(tokensToSave)
        
        val retrievedTokens = tokenStorage.getTokens()
        Assert.assertNotNull(retrievedTokens)
        Assert.assertEquals(tokensToSave.accessToken, retrievedTokens?.accessToken)
        Assert.assertNull(retrievedTokens?.refreshToken)
    }
    
    @Test
    fun saveTokens_withBothTokensNull_clearsExistingTokens() = runBlocking {
        // Arrange: Save some initial tokens
        val initialTokens = AuthTokens("initial_access", "initial_refresh")
        tokenStorage.saveTokens(initialTokens)
        Assert.assertNotNull(tokenStorage.getTokens())
        
        // Act: Save AuthTokens with both members null
        val tokensToSave = AuthTokens(null, null)
        tokenStorage.saveTokens(tokensToSave)
        
        // Assert
        val retrievedTokens = tokenStorage.getTokens()
        Assert.assertNull(
            "Tokens should be null after saving AuthTokens(null, null)",
            retrievedTokens
        )
    }
    
    @Test
    fun saveTokens_overwritesExistingTokens() = runBlocking {
        val initialTokens = AuthTokens("initial_access", "initial_refresh")
        tokenStorage.saveTokens(initialTokens)
        
        val newTokens = AuthTokens("new_access_token", "new_refresh_token")
        tokenStorage.saveTokens(newTokens)
        
        val retrievedTokens = tokenStorage.getTokens()
        Assert.assertNotNull(retrievedTokens)
        Assert.assertEquals(newTokens.accessToken, retrievedTokens?.accessToken)
        Assert.assertEquals(newTokens.refreshToken, retrievedTokens?.refreshToken)
    }
    
    //endregion
    
    //region GetTokens Tests
    
    @Test
    fun getTokens_whenNoTokensEverSaved_returnsNull() = runBlocking {
        // Setup ensures prefs are clear
        val retrievedTokens = tokenStorage.getTokens()
        Assert.assertNull("Tokens should be null when nothing has ever been saved", retrievedTokens)
    }
    
    @Test
    fun getTokens_afterSavingValidTokensThenClearing_returnsNull() = runBlocking {
        val tokensToSave = AuthTokens("access", "refresh")
        tokenStorage.saveTokens(tokensToSave)
        tokenStorage.clearTokens() // Clear them
        
        val retrievedTokens = tokenStorage.getTokens()
        Assert.assertNull("Tokens should be null after clearing", retrievedTokens)
    }
    
    @Test
    fun getTokens_whenOnlyAccessTokenWasSaved_returnsTokenWithAccessTokenOnly() = runBlocking {
        // Arrange
        tokenStorage.saveTokens(AuthTokens("valid_access_only", null))
        
        // Act
        val tokens = tokenStorage.getTokens()
        
        // Assert
        Assert.assertNotNull(tokens)
        Assert.assertEquals("valid_access_only", tokens?.accessToken)
        Assert.assertNull(tokens?.refreshToken)
    }
    
    @Test
    fun getTokens_whenOnlyRefreshTokenWasSaved_returnsTokenWithRefreshTokenOnly() =
        runBlocking {
            // Arrange
            tokenStorage.saveTokens(AuthTokens(null, "valid_refresh_only"))
            
            // Act
            val tokens = tokenStorage.getTokens()
            
            // Assert
            Assert.assertNotNull(tokens)
            Assert.assertNull(tokens?.accessToken)
            Assert.assertEquals("valid_refresh_only", tokens?.refreshToken)
        }
    
    // Testing "corrupted" data is tricky if EncryptedSharedPreferences handles all I/O.
    // EncryptedSharedPreferences is designed to throw exceptions if data is tampered with
    // or the master key is lost. It generally won't just return a "half-decrypted" state.
    // If a value is corrupted, getString might throw GeneralSecurityException or IOException.
    // Your getTokens() method's try-catch for these (if it has one) or its behavior
    // when getString returns default null needs to be understood.
    
    // Let's assume your `EncryptedAuthTokenStorage`'s `getString` calls
    // (which internally use EncryptedSharedPreferences) will return `null`
    // if a key is present but the value cannot be decrypted (e.g., due to corruption
    // or if the MasterKey changed/invalidated).
    // This is a simplification; often EncryptedSharedPreferences will throw.
    // If it throws, then getTokens() would need to catch it.
    
    @Test
    fun getTokens_whenUnderlyingAccessTokenIsUnreadable_andRefreshTokenIsValid_returnsRefreshTokenOnly() =
        runBlocking {
            // Arrange: Save a valid refresh token. Then, try to simulate an unreadable access token.
            tokenStorage.saveTokens(
                AuthTokens(
                    null,
                    "readable_refresh"
                )
            ) // Save refresh token normally
            
            // Simulate unreadable access token by writing a garbage string directly to the underlying prefs.
            // This bypasses EncryptedSharedPreferences' normal encryption for this specific key.
            // WARNING: This is a fragile way to test and depends on internal behavior.
            rawHelper.writePotentiallyCorruptibleString(
                PREF_KEY_ACCESS_TOKEN,
                "garbage_data_that_cannot_be_decrypted_by_EncryptedPrefs"
            )
            
            // Act
            val tokens = tokenStorage.getTokens()
            
            // Assert
            // Given the EncryptedAuthTokenStorage structure:
            // getString(PREF_KEY_ACCESS_TOKEN, null) for the garbage data should ideally return null
            // or throw an exception that EncryptedAuthTokenStorage handles by treating the token as null.
            Assert.assertNotNull(
                "AuthTokens should still be returned if one token is valid/readable",
                tokens
            )
            Assert.assertNull(
                "Access token should be null due to unreadability",
                tokens?.accessToken
            )
            Assert.assertEquals("readable_refresh", tokens?.refreshToken)
        }
    
    
    @Test
    fun getTokens_whenUnderlyingRefreshTokenIsUnreadable_andAccessTokenIsValid_returnsAccessTokenOnly() =
        runBlocking {
            // Arrange
            tokenStorage.saveTokens(AuthTokens("readable_access", null))
            rawHelper.writePotentiallyCorruptibleString(
                PREF_KEY_REFRESH_TOKEN,
                "garbage_data_refresh"
            )
            
            // Act
            val tokens = tokenStorage.getTokens()
            
            // Assert
            Assert.assertNotNull(tokens)
            Assert.assertEquals("readable_access", tokens?.accessToken)
            Assert.assertNull(tokens?.refreshToken)
        }
    
    @Test
    fun getTokens_whenBothUnderlyingTokensAreUnreadable_returnsNull() = runBlocking {
        // Arrange
        rawHelper.writePotentiallyCorruptibleString(PREF_KEY_ACCESS_TOKEN, "garbage_access")
        rawHelper.writePotentiallyCorruptibleString(PREF_KEY_REFRESH_TOKEN, "garbage_refresh")
        
        // Act
        val tokens = tokenStorage.getTokens()
        
        // Assert
        Assert.assertNull("Tokens should be null if both underlying values are unreadable", tokens)
    }
    
    //endregion
    
    //region ClearTokens Tests
    
    @Test
    fun clearTokens_removesSavedTokens() = runBlocking {
        val tokensToSave = AuthTokens("access", "refresh")
        tokenStorage.saveTokens(tokensToSave)
        Assert.assertNotNull(tokenStorage.getTokens()) // Ensure they were saved
        
        tokenStorage.clearTokens()
        Assert.assertNull("Tokens should be null after clearTokens", tokenStorage.getTokens())
        
        // Verify with raw inspector that keys are gone
        Assert.assertFalse(
            "Access token key should not exist in raw prefs after clear",
            rawPrefsInspector.contains(PREF_KEY_ACCESS_TOKEN)
        )
        Assert.assertFalse(
            "Refresh token key should not exist in raw prefs after clear",
            rawPrefsInspector.contains(PREF_KEY_REFRESH_TOKEN)
        )
    }
    
    @Test
    fun clearTokens_whenOnlyAccessTokenWasSavedViaStorage_removesIt() = runBlocking {
        // Arrange: Save only an access token using the storage
        tokenStorage.saveTokens(AuthTokens("access_only_to_clear", null))
        val initialRetrieved = tokenStorage.getTokens()
        Assert.assertNotNull(initialRetrieved)
        Assert.assertEquals("access_only_to_clear", initialRetrieved?.accessToken)
        Assert.assertNull(initialRetrieved?.refreshToken)
        
        // Act
        tokenStorage.clearTokens()
        
        // Assert
        Assert.assertNull("Tokens should be null after clear", tokenStorage.getTokens())
        Assert.assertFalse(
            "Access token key should not exist in raw prefs after clear",
            rawPrefsInspector.contains(PREF_KEY_ACCESS_TOKEN)
        )
    }
    
    
    @Test
    fun clearTokens_whenOnlyRefreshTokenWasSavedViaStorage_removesIt() = runBlocking {
        // Arrange: Save only a refresh token using the storage
        tokenStorage.saveTokens(AuthTokens(null, "refresh_only_to_clear"))
        val initialRetrieved = tokenStorage.getTokens()
        Assert.assertNotNull(initialRetrieved)
        Assert.assertNull(initialRetrieved?.accessToken)
        Assert.assertEquals("refresh_only_to_clear", initialRetrieved?.refreshToken)
        
        
        // Act
        tokenStorage.clearTokens()
        
        // Assert
        Assert.assertNull("Tokens should be null after clear", tokenStorage.getTokens())
        Assert.assertFalse(
            "Refresh token key should not exist in raw prefs after clear",
            rawPrefsInspector.contains(PREF_KEY_REFRESH_TOKEN)
        )
    }
    
    
    @Test
    fun clearTokens_whenNoTokensExist_doesNotThrowErrorAndTokensRemainNull() = runBlocking {
        // Arrange: Ensured by setup
        
        // Act & Assert that no error is thrown
        try {
            tokenStorage.clearTokens()
        } catch (e: Exception) {
            Assert.fail("clearTokens should not throw an error when no tokens exist: ${e.message}")
        }
        
        // Assert: Tokens should still be null
        Assert.assertNull(
            "Tokens should remain null after clearing when none existed",
            tokenStorage.getTokens()
        )
        Assert.assertFalse(
            "Access token key should still not exist",
            rawPrefsInspector.contains(PREF_KEY_ACCESS_TOKEN)
        )
        Assert.assertFalse(
            "Refresh token key should still not exist",
            rawPrefsInspector.contains(PREF_KEY_REFRESH_TOKEN)
        )
    }
    
    @Test
    fun clearTokens_doesNotAffectOtherSharedPreferences() = runBlocking {
        // Arrange: Save some dummy tokens and another unrelated preference
        // Note: The unrelated preference is written to the *same* EncryptedSharedPreferences file.
        // EncryptedSharedPreferences encrypts both keys and values if configured.
        // If clearTokens only removes PREF_KEY_ACCESS_TOKEN and PREF_KEY_REFRESH_TOKEN, others should remain.
        
        tokenStorage.saveTokens(AuthTokens("dummy_access", "dummy_refresh"))
        
        val otherPrefKey = "unrelated_preference_key"
        val otherPrefValue = "unrelated_value"
        // EncryptedSharedPreferences will encrypt this key and value too.
        rawPrefsInspector.edit().putString(otherPrefKey, otherPrefValue).commit()
        
        
        // Sanity check
        Assert.assertNotNull(tokenStorage.getTokens())
        Assert.assertEquals(otherPrefValue, rawPrefsInspector.getString(otherPrefKey, null))
        
        // Act
        tokenStorage.clearTokens()
        
        // Assert: Auth tokens are cleared
        Assert.assertNull(tokenStorage.getTokens())
        
        // Assert: The other preference remains untouched
        Assert.assertEquals(
            "Other preference should not be affected by clearTokens",
            otherPrefValue,
            rawPrefsInspector.getString(otherPrefKey, null)
        )
    }
    
    //endregion
}