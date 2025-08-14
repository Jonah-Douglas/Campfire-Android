package com.example.campfire.core.security

import android.security.KeyStoreException
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import com.example.campfire.core.common.logging.Firelog
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton


interface IEncryptionManager {
    /**
     * Encrypts the given data string.
     * @param data The plaintext string to encrypt.
     * @param alias The alias for the Keystore key to use/create.
     * @return EncryptedData containing ciphertext and IV, or null on failure.
     */
    fun encrypt(data: String, alias: String): EncryptedData?
    
    /**
     * Decrypts the given EncryptedData.
     * @param encryptedData The data to decrypt.
     * @param alias The alias for the Keystore key used for encryption.
     * @return The decrypted plaintext string, or null on failure (including if user authentication is required but not performed).
     * @throws UserNotAuthenticatedException if the key requires user authentication and the user has not authenticated.
     *         The caller is responsible for catching this and initiating the authentication flow.
     */
    @Throws(UserNotAuthenticatedException::class)
    fun decrypt(encryptedData: EncryptedData, alias: String): String?
    
    /**
     * Deletes the encryption key associated with the given alias from the Android Keystore.
     * @param alias The alias of the key to delete.
     * @return True if deletion was successful or key didn't exist, false otherwise.
     */
    fun deleteKey(alias: String): Boolean
}

data class EncryptedData(val ciphertext: ByteArray, val initializationVector: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedData
        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!initializationVector.contentEquals(other.initializationVector)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + initializationVector.contentHashCode()
        return result
    }
}

@Singleton
class AndroidKeystoreEncryptionManager @Inject constructor() : IEncryptionManager {
    
    private val androidKeyStoreName = "AndroidKeyStore"
    private val keyStore: KeyStore
    
    init {
        try {
            keyStore = KeyStore.getInstance(androidKeyStoreName).apply { load(null) }
        } catch (e: KeyStoreException) {
            Firelog.e(LOG_KEYSTORE_EXCEPTION)
            throw RuntimeException(ERROR_INITIALIZE_KEYSTORE_FAILURE, e)
        } catch (e: CertificateException) {
            Firelog.e(LOG_CERTIFICATE_EXCEPTION)
            throw RuntimeException(ERROR_INITIALIZE_KEYSTORE_FAILURE, e)
        } catch (e: NoSuchAlgorithmException) {
            Firelog.e(LOG_NO_SUCH_ALGORITHM_EXCEPTION)
            throw RuntimeException(ERROR_INITIALIZE_KEYSTORE_FAILURE, e)
        } catch (e: IOException) {
            Firelog.e(LOG_IO_EXCEPTION)
            throw RuntimeException(ERROR_INITIALIZE_KEYSTORE_FAILURE, e)
        } catch (e: Exception) { // Catch-all for any other init errors
            Firelog.e(LOG_GENERIC_EXCEPTION)
            throw RuntimeException(ERROR_INITIALIZE_KEYSTORE_FAILURE, e)
        }
    }
    
    private fun getSecretKey(alias: String): SecretKey? {
        try {
            val existingKey = keyStore.getKey(alias, null) // No password for AndroidKeyStore
            if (existingKey != null) {
                if (existingKey is KeyStore.SecretKeyEntry) {
                    return existingKey.secretKey
                } else {
                    // Found an entry but it's not a SecretKeyEntry (e.g., PrivateKeyEntry)
                    Firelog.w(String.format(LOG_EXISTING_INVALID_ENTRY, alias))
                    deleteKey(alias) // Clean up the unexpected entry type
                    return generateSecretKey(alias) // Attempt to generate the correct type
                }
            }
            // Key not found, generate a new one
            return generateSecretKey(alias)
        } catch (e: UnrecoverableKeyException) {
            deleteKey(alias) // Attempt to clean up the problematic key
            return null
        } catch (e: KeyStoreException) {
            Firelog.e(String.format(LOG_KEYSTORE_ERROR, alias))
        } catch (e: NoSuchAlgorithmException) {
            Firelog.e(String.format(LOG_MISSING_ALGORITHM, alias), e)
        } catch (e: Exception) {
            Firelog.e(String.format(LOG_GENERIC_ALIAS_EXCEPTION, alias), e)
        }
        
        return null
    }
    
    private fun generateSecretKey(alias: String): SecretKey? {
        try {
            val keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeyStoreName)
            
            val builder = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
            
            keyGenerator.init(builder.build())
            val newKey = keyGenerator.generateKey()
            return newKey
        } catch (e: NoSuchAlgorithmException) {
            Firelog.e(String.format(LOG_MISSING_ALGORITHM, alias))
        } catch (e: NoSuchProviderException) {
            Firelog.e(String.format(LOG_ANDROIDKEYSTORE_MISSING, alias))
        } catch (e: InvalidAlgorithmParameterException) {
            Firelog.e(String.format(LOG_INVALID_ALGORITHM_PARAMETERS, alias))
        } catch (e: Exception) {
            Firelog.e(String.format(LOG_SECRET_KEY_GEN_FAILED, alias))
            
        }
        return null
    }
    
    override fun encrypt(data: String, alias: String): EncryptedData? {
        try {
            val secretKey = getSecretKey(alias)
                ?: run {
                    Firelog.e(String.format(LOG_MISSING_SECRET_KEY, alias))
                    return null
                }
            
            val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val ciphertext = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            if (iv == null || iv.isEmpty()) {
                Firelog.e(String.format(LOG_MISSING_IV, alias))
                return null
            }
            Firelog.d(String.format(LOG_ENCRYPTION_SUCCESS, alias))
            return EncryptedData(ciphertext, iv)
        } catch (e: UserNotAuthenticatedException) {
            Firelog.w(String.format(LOG_USER_NOT_AUTHENTICATED, alias))
            throw e
        } catch (e: KeyPermanentlyInvalidatedException) {
            Firelog.e(String.format(LOG_DELETING_KEY, alias))
            deleteKey(alias) // Clean up the invalidated key
            return null
        } catch (e: InvalidKeyException) {
            Firelog.e(String.format(LOG_INVALID_KEY, alias))
            deleteKey(alias) // Clean up potentially problematic key
            return null
        } catch (e: NoSuchAlgorithmException) {
            Firelog.e(String.format(LOG_NO_SUCH_ALGORITHM_FOR_AES, alias))
        } catch (e: NoSuchPaddingException) {
            Firelog.e(String.format(LOG_NO_SUCH_PADDING_FOR_AES, alias))
        } catch (e: IllegalBlockSizeException) {
            Firelog.e(String.format(LOG_ILLEGAL_BLOCK_SIZE, alias))
        } catch (e: BadPaddingException) {
            Firelog.e(String.format(LOG_BAD_PADDING, alias))
        } catch (e: Exception) {
            Firelog.e(String.format(LOG_GENERIC_ENCRYPTION_ERROR, alias))
        }
        return null
    }
    
    @Throws(UserNotAuthenticatedException::class)
    override fun decrypt(encryptedData: EncryptedData, alias: String): String? {
        try {
            val secretKey =
                getSecretKey(alias)
                    ?: run {
                        Firelog.e(String.format(LOG_SECRET_KEY_MISSING, alias))
                        return null
                    }
            
            val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
            val spec = GCMParameterSpec(GCM_LOG_TAG_LENGTH_BITS, encryptedData.initializationVector)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decryptedBytes = cipher.doFinal(encryptedData.ciphertext)
            val decryptedString = String(decryptedBytes, Charsets.UTF_8)
            Firelog.d(String.format(LOG_DECRYPTION_SUCCESS, alias))
            return decryptedString
        } catch (e: UserNotAuthenticatedException) {
            Firelog.w(String.format(LOG_USER_NOT_AUTHENTICATED, alias), e)
            throw e
        } catch (e: KeyPermanentlyInvalidatedException) {
            Firelog.e(String.format(LOG_DELETING_KEY, alias), e)
            deleteKey(alias) // Clean up the invalidated key
            return null
        } catch (e: AEADBadTagException) {
            Firelog.e(String.format(LOG_AUTH_TAG_MISMATCH, alias))
            return null
        } catch (e: InvalidKeyException) {
            Firelog.e(String.format(LOG_INVALID_KEY, alias))
            deleteKey(alias) // Clean up potentially problematic key
            return null
        } catch (e: InvalidAlgorithmParameterException) {
            Firelog.e(String.format(LOG_DECRYPT_INVALID_ALGORITHM, alias))
        } catch (e: NoSuchAlgorithmException) {
            Firelog.e(String.format(LOG_NO_SUCH_ALGORITHM_FOR_AES, alias))
        } catch (e: NoSuchPaddingException) {
            Firelog.e(String.format(LOG_NO_SUCH_PADDING_FOR_AES, alias))
        } catch (e: IllegalBlockSizeException) {
            Firelog.e(String.format(LOG_ILLEGAL_BLOCK_SIZE, alias))
        } catch (e: BadPaddingException) {
            Firelog.e(String.format(LOG_BAD_PADDING, alias))
        } catch (e: Exception) {
            Firelog.e(String.format(LOG_GENERIC_ENCRYPTION_ERROR, alias))
        }
        
        return null
    }
    
    override fun deleteKey(alias: String): Boolean {
        try {
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
                Firelog.d(String.format(LOG_DELETED_KEY, alias))
                return true
            } else {
                Firelog.d(String.format(LOG_MISSING_KEY, alias))
                return true
            }
        } catch (e: KeyStoreException) {
            Firelog.e(String.format(LOG_DELETE_KEY_FAIL, alias))
        } catch (e: Exception) {
            Firelog.e(String.format(LOG_UNEXPECTED_DELETE_ERROR, alias))
        }
        
        return false
    }
    
    companion object {
        private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
        private const val GCM_LOG_TAG_LENGTH_BITS = 128
        
        // --- Init ---
        private const val LOG_KEYSTORE_EXCEPTION =
            "Failed to initialize KeyStore (KeyStoreException)"
        private const val LOG_CERTIFICATE_EXCEPTION =
            "Failed to initialize KeyStore (CertificateException)"
        private const val LOG_NO_SUCH_ALGORITHM_EXCEPTION =
            "Failed to initialize KeyStore (NoSuchAlgorithmException)"
        private const val LOG_IO_EXCEPTION =
            "Failed to initialize KeyStore (IOException)"
        private const val LOG_GENERIC_EXCEPTION =
            "Failed to initialize KeyStore (Unexpected Exception)"
        private const val ERROR_INITIALIZE_KEYSTORE_FAILURE =
            "Failed to initialize KeyStore"
        
        // --- GetSecretKey ---
        private const val LOG_EXISTING_INVALID_ENTRY =
            "Existing entry for alias '%s' is not a SecretKey. Deleting invalid entry."
        private const val LOG_KEYSTORE_ERROR =
            "KeyStore error while getting key for alias '%s'"
        private const val LOG_MISSING_ALGORITHM =
            "Algorithm not found while getting key for alias '%S'"
        private const val LOG_GENERIC_ALIAS_EXCEPTION =
            "Unexpected error while getting key for alias '%s'"
        
        // --- Generate Secret Key ---
        private const val LOG_ANDROIDKEYSTORE_MISSING =
            "Failed to get AndroidKeyStore provider for alias '%s'"
        private const val LOG_INVALID_ALGORITHM_PARAMETERS =
            "Invalid Algorithm parameters for KeyGenParameterSpec for alias '%s'"
        private const val LOG_SECRET_KEY_GEN_FAILED =
            "Unexpected error during secret key generation for alias '%s'"
        
        // --- Encrypt ---
        private const val LOG_MISSING_SECRET_KEY =
            "Failed to get or generate secret key for alias '%s' during encryption."
        private const val LOG_MISSING_IV =
            "IV was null or empty after encryption for alias '%s'. This is a critical error."
        private const val LOG_ENCRYPTION_SUCCESS =
            "Encryption successful for alias: '%s'"
        private const val LOG_USER_NOT_AUTHENTICATED =
            "UserNotAuthenticatedException during encryption for alias '%s'. This is unexpected. Re-throwing."
        private const val LOG_DELETING_KEY =
            "Key permanently invalidated during encryption for alias '%s'. Deleting key. Data cannot be encrypted."
        private const val LOG_INVALID_KEY =
            "Invalid key during encryption for alias '%s'. This might mean key spec and cipher mismatch or a corrupted key."
        private const val LOG_NO_SUCH_ALGORITHM_FOR_AES =
            "NoSuchAlgorithmException for AES/GCM/NoPadding during encryption for alias '%s'"
        private const val LOG_NO_SUCH_PADDING_FOR_AES =
            "NoSuchPaddingException for AES/GCM/NoPadding during encryption for alias '%s'"
        private const val LOG_ILLEGAL_BLOCK_SIZE =
            "IllegalBlockSizeException during encryption for alias '%s'"
        private const val LOG_BAD_PADDING =
            "BadPaddingException during encryption for alias '%s'"
        private const val LOG_GENERIC_ENCRYPTION_ERROR =
            "Unexpected error during encryption for alias '%s'"
        
        // --- Decrypt ---
        private const val LOG_SECRET_KEY_MISSING =
            "Secret key not found for alias '%s' during decryption. Data cannot be decrypted."
        private const val LOG_DECRYPTION_SUCCESS =
            "Decryption successful for alias: %s"
        private const val LOG_AUTH_TAG_MISMATCH =
            "AEADBadLOG_TAGException: Decryption failed for alias '%s' due to authentication tag mismatch. Data may be tampered or incorrect key/IV."
        private const val LOG_DECRYPT_INVALID_ALGORITHM =
            "InvalidAlgorithmParameterException (likely GCMParameterSpec) during decryption for alias '%s'"
        
        // --- Delete Key ---
        private const val LOG_DELETED_KEY =
            "Successfully deleted key with alias: '%s'"
        private const val LOG_MISSING_KEY =
            "Key with alias '%s' not found for deletion."
        private const val LOG_DELETE_KEY_FAIL =
            "Failed to delete key with alias '%s' from KeyStore"
        private const val LOG_UNEXPECTED_DELETE_ERROR =
            "Unexpected error while deleting key with alias '%s'"
    }
}