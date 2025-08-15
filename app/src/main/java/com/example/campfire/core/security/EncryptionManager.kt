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


/**
 * Defines the contract for an encryption manager capable of encrypting and decrypting data,
 * and managing underlying cryptographic keys.
 */
interface IEncryptionManager {
    
    /**
     * Encrypts the given data string using a key associated with the provided alias.
     * If a key for the alias does not exist, it will be generated.
     *
     * @param data The plaintext string to encrypt.
     * @param alias The unique alias for the Keystore key to use or create.
     * @return [EncryptedData] containing the ciphertext and initialization vector (IV) if encryption
     *         is successful, or `null` if any error occurs during the process (e.g., key generation failure,
     *         encryption failure).
     * @throws UserNotAuthenticatedException if the key requires user authentication for encryption
     *         and the user has not authenticated. This exception is re-thrown and should be handled
     *         by the caller, potentially by initiating an authentication flow.
     */
    @Throws(UserNotAuthenticatedException::class)
    fun encrypt(data: String, alias: String): EncryptedData?
    
    /**
     * Decrypts the given [EncryptedData] using the key associated with the provided alias.
     *
     * @param encryptedData The [EncryptedData] object containing the ciphertext and IV to decrypt.
     * @param alias The alias for the Keystore key that was used for encryption.
     * @return The decrypted plaintext string if decryption is successful. Returns `null` if any error
     *         occurs (e.g., key not found, invalid key, tampered data leading to AEADBadTagException,
     *         or other cryptographic errors).
     * @throws UserNotAuthenticatedException if the key requires user authentication for decryption
     *         and the user has not authenticated. The caller is responsible for catching this
     *         and initiating the appropriate authentication flow.
     */
    @Throws(UserNotAuthenticatedException::class)
    fun decrypt(encryptedData: EncryptedData, alias: String): String?
    
    /**
     * Deletes the encryption key associated with the given alias from the Android Keystore.
     * If no key exists for the alias, the operation is considered successful.
     *
     * @param alias The alias of the key to delete.
     * @return `true` if the deletion was successful or if the key did not exist.
     *         Returns `false` if a [KeyStoreException] or other unexpected error occurs
     *         during the deletion attempt.
     */
    fun deleteKey(alias: String): Boolean
}

/**
 * A data class holding the result of an encryption operation.
 * It encapsulates the encrypted ciphertext and the initialization vector (IV)
 * used during the AES/GCM encryption process. The IV is crucial for decryption.
 *
 * @property ciphertext The encrypted data as a byte array.
 * @property initializationVector The initialization vector (IV) used for encryption, as a byte array.
 *                                This must be stored alongside the ciphertext and provided for decryption.
 */
data class EncryptedData(
    val ciphertext: ByteArray,
    val initializationVector: ByteArray
) {
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

/**
 * An implementation of [IEncryptionManager] that utilizes the Android Keystore system
 * to securely store and manage cryptographic keys for AES/GCM encryption and decryption.
 *
 * This manager handles:
 *  - Initialization of the Android Keystore.
 *  - Secure generation of AES-256 keys if they don't already exist for a given alias.
 *  - Encryption of string data using AES/GCM/NoPadding.
 *  - Decryption of corresponding [EncryptedData].
 *  - Deletion of keys from the Keystore.
 *
 * It uses "AES/GCM/NoPadding" as the transformation, which provides authenticated encryption,
 * ensuring both confidentiality and integrity of the encrypted data.
 *
 * Error handling is done by logging issues using [Firelog] and returning `null` or `false`
 * for most operations, except for [UserNotAuthenticatedException] which is re-thrown
 * to allow the caller to initiate user authentication. Keys that become permanently invalidated
 * or are found to be problematic are proactively deleted.
 *
 * This class is designed as a Singleton to ensure a single interface to the Android Keystore.
 */
@Singleton
class AndroidKeystoreEncryptionManager @Inject constructor() : IEncryptionManager {
    
    private val androidKeyStoreName = "AndroidKeyStore"
    private val keyStore: KeyStore
    
    init {
        try {
            keyStore = KeyStore.getInstance(androidKeyStoreName).apply { load(null) }
            Firelog.d(LOG_KEYSTORE_INIT_SUCCESS)
        } catch (e: KeyStoreException) {
            Firelog.e(LOG_KEYSTORE_EXCEPTION, e)
            throw RuntimeException(ERROR_INITIALIZE_KEYSTORE_FAILURE, e)
        } catch (e: CertificateException) {
            Firelog.e(LOG_CERTIFICATE_EXCEPTION, e)
            throw RuntimeException(ERROR_INITIALIZE_KEYSTORE_FAILURE, e)
        } catch (e: NoSuchAlgorithmException) {
            Firelog.e(LOG_NO_SUCH_ALGORITHM_EXCEPTION, e)
            throw RuntimeException(ERROR_INITIALIZE_KEYSTORE_FAILURE, e)
        } catch (e: IOException) {
            Firelog.e(LOG_IO_EXCEPTION, e)
            throw RuntimeException(ERROR_INITIALIZE_KEYSTORE_FAILURE, e)
        } catch (e: Exception) {
            Firelog.e(LOG_GENERIC_EXCEPTION, e)
            throw RuntimeException(ERROR_INITIALIZE_KEYSTORE_FAILURE, e)
        }
    }
    
    /**
     * Retrieves an existing [SecretKey] from the Android Keystore for the given [alias].
     * If the key does not exist, it attempts to generate a new one.
     * If an entry exists for the alias but is not a [SecretKey], it deletes the invalid entry
     * and attempts to generate a new valid key.
     * Handles various Keystore exceptions and logs them.
     *
     * @param alias The alias of the secret key to retrieve or generate.
     * @return The [SecretKey] if found or successfully generated, or `null` if any error occurs
     *         (e.g., Keystore issues, key generation failure, unrecoverable key).
     *         If an [UnrecoverableKeyException] occurs, the problematic key is deleted.
     */
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
            } else {
                // Key not found for the alias, proceed to generate a new one.
                Firelog.d(String.format(LOG_GENERATING_NEW_KEY, alias))
                return generateSecretKey(alias)
            }
        } catch (e: UnrecoverableKeyException) {
            Firelog.e(String.format(LOG_UNRECOVERABLE_KEY, alias), e)
            deleteKey(alias) // Attempt to clean up the problematic key
            return null
        } catch (e: KeyStoreException) {
            Firelog.e(String.format(LOG_KEYSTORE_ERROR_GET_KEY, alias), e)
        } catch (e: NoSuchAlgorithmException) {
            Firelog.e(String.format(LOG_MISSING_ALGORITHM_GET_KEY, alias), e)
        } catch (e: Exception) {
            Firelog.e(String.format(LOG_GENERIC_ALIAS_EXCEPTION, alias), e)
        }
        
        return null
    }
    
    /**
     * Generates a new AES-256 [SecretKey] using [KeyGenParameterSpec] and stores it
     * in the Android Keystore under the given [alias].
     * The key is configured for AES/GCM/NoPadding encryption and decryption.
     *
     * Key Properties:
     * - Algorithm: AES
     * - Block Mode: GCM (Galois/Counter Mode)
     * - Padding: NoPadding (GCM handles padding implicitly)
     * - Key Size: 256 bits
     * - Purpose: Encrypt and Decrypt
     *
     * @param alias The alias under which the new secret key will be stored.
     * @return The newly generated [SecretKey], or `null` if key generation fails due to
     *         provider issues, algorithm issues, or invalid parameters.
     */
    private fun generateSecretKey(alias: String): SecretKey? {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                androidKeyStoreName
            )
            
            val builder = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
            
            keyGenerator.init(builder.build())
            val newKey = keyGenerator.generateKey()
            Firelog.d(String.format(LOG_KEY_GENERATION_SUCCESS, alias))
            return newKey
        } catch (e: NoSuchAlgorithmException) {
            Firelog.e(String.format(LOG_MISSING_ALGORITHM_GEN_KEY, alias), e)
        } catch (e: NoSuchProviderException) {
            Firelog.e(String.format(LOG_ANDROIDKEYSTORE_MISSING, alias), e)
        } catch (e: InvalidAlgorithmParameterException) {
            Firelog.e(String.format(LOG_INVALID_ALGORITHM_PARAMETERS, alias), e)
        } catch (e: Exception) {
            Firelog.e(String.format(LOG_SECRET_KEY_GEN_FAILED, alias), e)
        }
        
        return null
    }
    
    /**
     * Encrypts the given plaintext [data] string using AES/GCM/NoPadding.
     * It retrieves or generates a [SecretKey] for the given [alias].
     * If the key requires user authentication and it's not provided,
     * a [UserNotAuthenticatedException] is thrown.
     * If the key is permanently invalidated, it's deleted.
     *
     * @param data The plaintext string to encrypt.
     * @param alias The alias for the Keystore key to use/create for encryption.
     * @return An [EncryptedData] object containing the ciphertext and IV on success,
     *         or `null` if encryption fails for other reasons (e.g., key issues,
     *         cipher initialization, cryptographic errors).
     * @throws UserNotAuthenticatedException If the key requires user authentication and the user
     *                                     has not authenticated.
     */
    @Throws(UserNotAuthenticatedException::class)
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
            Firelog.w(String.format(LOG_USER_NOT_AUTHENTICATED_ENCRYPT, alias), e)
            throw e
        } catch (e: KeyPermanentlyInvalidatedException) {
            Firelog.e(String.format(LOG_KEY_INVALIDATED_ENCRYPT, alias), e)
            deleteKey(alias)
            return null
        } catch (e: InvalidKeyException) {
            Firelog.e(String.format(LOG_INVALID_KEY_ENCRYPT, alias), e)
            deleteKey(alias) // Clean up potentially problematic key
            return null
        } catch (e: NoSuchAlgorithmException) {
            Firelog.e(String.format(LOG_NO_SUCH_ALGORITHM_FOR_AES_ENCRYPT, alias), e)
        } catch (e: NoSuchPaddingException) {
            Firelog.e(String.format(LOG_NO_SUCH_PADDING_FOR_AES_ENCRYPT, alias), e)
        } catch (e: IllegalBlockSizeException) {
            Firelog.e(String.format(LOG_ILLEGAL_BLOCK_SIZE_ENCRYPT, alias), e)
        } catch (e: BadPaddingException) {
            Firelog.e(String.format(LOG_BAD_PADDING_ENCRYPT, alias), e)
        } catch (e: Exception) {
            Firelog.e(String.format(LOG_GENERIC_ENCRYPTION_ERROR, alias), e)
        }
        
        return null
    }
    
    /**
     * Decrypts the provided [EncryptedData] (ciphertext and IV) using AES/GCM/NoPadding.
     * It retrieves the [SecretKey] associated with the given [alias].
     * If the key requires user authentication and it's not provided,
     * a [UserNotAuthenticatedException] is thrown.
     * If the key is permanently invalidated, it's deleted.
     * Handles [AEADBadTagException] which can indicate data tampering or incorrect key/IV.
     *
     * @param encryptedData The [EncryptedData] object containing the ciphertext and IV.
     * @param alias The alias for the Keystore key used for the original encryption.
     * @return The decrypted plaintext string on success, or `null` if decryption fails
     *         (e.g., key issues, cipher initialization, cryptographic errors, auth tag mismatch).
     * @throws UserNotAuthenticatedException If the key requires user authentication and the user
     *                                     has not authenticated.
     */
    @Throws(UserNotAuthenticatedException::class)
    override fun decrypt(encryptedData: EncryptedData, alias: String): String? {
        try {
            val secretKey =
                getSecretKey(alias)
                    ?: run {
                        Firelog.e(String.format(LOG_SECRET_KEY_MISSING_DECRYPT, alias))
                        return null
                    }
            
            val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, encryptedData.initializationVector)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decryptedBytes = cipher.doFinal(encryptedData.ciphertext)
            val decryptedString = String(decryptedBytes, Charsets.UTF_8)
            
            Firelog.d(String.format(LOG_DECRYPTION_SUCCESS, alias))
            return decryptedString
        } catch (e: UserNotAuthenticatedException) {
            Firelog.w(String.format(LOG_USER_NOT_AUTHENTICATED_DECRYPT, alias), e)
            throw e
        } catch (e: KeyPermanentlyInvalidatedException) {
            Firelog.e(String.format(LOG_KEY_INVALIDATED_DECRYPT, alias), e)
            deleteKey(alias)
            return null
        } catch (e: AEADBadTagException) {
            Firelog.e(String.format(LOG_AUTH_TAG_MISMATCH, alias), e)
            return null
        } catch (e: InvalidKeyException) {
            Firelog.e(String.format(LOG_INVALID_KEY_DECRYPT, alias), e)
            deleteKey(alias)
            return null
        } catch (e: InvalidAlgorithmParameterException) {
            Firelog.e(String.format(LOG_DECRYPT_INVALID_ALGORITHM, alias), e)
        } catch (e: NoSuchAlgorithmException) {
            Firelog.e(String.format(LOG_NO_SUCH_ALGORITHM_FOR_AES_DECRYPT, alias), e)
        } catch (e: NoSuchPaddingException) {
            Firelog.e(String.format(LOG_NO_SUCH_PADDING_FOR_AES_DECRYPT, alias), e)
        } catch (e: IllegalBlockSizeException) {
            Firelog.e(String.format(LOG_ILLEGAL_BLOCK_SIZE_DECRYPT, alias), e)
        } catch (e: BadPaddingException) {
            Firelog.e(String.format(LOG_BAD_PADDING_DECRYPT, alias), e)
        } catch (e: Exception) {
            Firelog.e(String.format(LOG_GENERIC_DECRYPTION_ERROR, alias), e)
        }
        
        return null
    }
    
    /**
     * Deletes the cryptographic key associated with the given [alias] from the Android Keystore.
     * If the key does not exist, the operation is logged but considered successful (idempotent).
     *
     * @param alias The alias of the key to be deleted.
     * @return `true` if the key was successfully deleted or did not exist.
     *         Returns `false` if a [KeyStoreException] or other unexpected error occurs.
     */
    override fun deleteKey(alias: String): Boolean {
        try {
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
                Firelog.d(String.format(LOG_DELETED_KEY, alias))
                return true
            } else {
                Firelog.d(String.format(LOG_MISSING_KEY_FOR_DELETE, alias))
                return true
            }
        } catch (e: KeyStoreException) {
            Firelog.e(String.format(LOG_DELETE_KEY_FAIL, alias), e)
        } catch (e: Exception) {
            Firelog.e(String.format(LOG_UNEXPECTED_DELETE_ERROR, alias), e)
        }
        
        return false
    }
    
    companion object {
        private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        
        // --- Init ---
        private const val LOG_KEYSTORE_INIT_SUCCESS =
            "AndroidKeystoreEncryptionManager initialized successfully."
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
        private const val LOG_GENERATING_NEW_KEY =
            "Key not found for alias '%s'. Generating new key."
        private const val LOG_UNRECOVERABLE_KEY =
            "Key for alias '%s' is unrecoverable. Deleting key."
        private const val LOG_KEYSTORE_ERROR_GET_KEY =
            "KeyStore error while getting key for alias '%s'"
        private const val LOG_MISSING_ALGORITHM_GET_KEY =
            "KeyStore algorithm not found while getting key for alias '%s'"
        private const val LOG_EXISTING_INVALID_ENTRY =
            "Existing entry for alias '%s' is not a SecretKey. Deleting invalid entry."
        private const val LOG_GENERIC_ALIAS_EXCEPTION =
            "Unexpected error while getting key for alias '%s'"
        
        // --- Generate Secret Key ---
        private const val LOG_KEY_GENERATION_SUCCESS =
            "AES-256/GCM key generated successfully for alias '%s'."
        private const val LOG_MISSING_ALGORITHM_GEN_KEY =
            "AES algorithm not found during key generation for alias '%s'."
        private const val LOG_ANDROIDKEYSTORE_MISSING =
            "Failed to get AndroidKeyStore provider for alias '%s'"
        private const val LOG_INVALID_ALGORITHM_PARAMETERS =
            "Invalid Algorithm parameters for KeyGenParameterSpec for alias '%s'"
        private const val LOG_SECRET_KEY_GEN_FAILED =
            "Unexpected error during secret key generation for alias '%s'"
        
        // --- Encrypt ---
        private const val LOG_USER_NOT_AUTHENTICATED_ENCRYPT =
            "UserNotAuthenticatedException during encryption for alias '%s'. Re-throwing."
        private const val LOG_KEY_INVALIDATED_ENCRYPT =
            "Key permanently invalidated during encryption for alias '%s'. Deleting key."
        private const val LOG_INVALID_KEY_ENCRYPT =
            "Invalid key during encryption for alias '%s'. Deleting key."
        private const val LOG_NO_SUCH_ALGORITHM_FOR_AES_ENCRYPT =
            "NoSuchAlgorithmException (AES/GCM/NoPadding) during encryption for alias '%s'"
        private const val LOG_NO_SUCH_PADDING_FOR_AES_ENCRYPT =
            "NoSuchPaddingException (AES/GCM/NoPadding) during encryption for alias '%s'"
        private const val LOG_ILLEGAL_BLOCK_SIZE_ENCRYPT =
            "IllegalBlockSizeException during encryption for alias '%s'"
        private const val LOG_BAD_PADDING_ENCRYPT =
            "BadPaddingException during encryption for alias '%s'"
        private const val LOG_MISSING_SECRET_KEY =
            "Failed to get or generate secret key for alias '%s' during encryption."
        private const val LOG_MISSING_IV =
            "IV was null or empty after encryption for alias '%s'. This is a critical error."
        private const val LOG_ENCRYPTION_SUCCESS =
            "Encryption successful for alias: '%s'"
        private const val LOG_GENERIC_ENCRYPTION_ERROR =
            "Unexpected error during encryption for alias '%s'"
        
        // --- Decrypt ---
        private const val LOG_SECRET_KEY_MISSING_DECRYPT =
            "Secret key not found for alias '%s' during decryption. Data cannot be decrypted."
        private const val LOG_USER_NOT_AUTHENTICATED_DECRYPT =
            "UserNotAuthenticatedException during decryption for alias '%s'. Re-throwing."
        private const val LOG_KEY_INVALIDATED_DECRYPT =
            "Key permanently invalidated during decryption for alias '%s'. Deleting key."
        private const val LOG_INVALID_KEY_DECRYPT =
            "Invalid key during decryption for alias '%s'. Deleting key."
        private const val LOG_NO_SUCH_ALGORITHM_FOR_AES_DECRYPT =
            "NoSuchAlgorithmException (AES/GCM/NoPadding) during decryption for alias '%s'"
        private const val LOG_NO_SUCH_PADDING_FOR_AES_DECRYPT =
            "NoSuchPaddingException (AES/GCM/NoPadding) during decryption for alias '%s'"
        private const val LOG_ILLEGAL_BLOCK_SIZE_DECRYPT =
            "IllegalBlockSizeException during decryption for alias '%s'"
        private const val LOG_BAD_PADDING_DECRYPT =
            "BadPaddingException during decryption for alias '%s'"
        private const val LOG_GENERIC_DECRYPTION_ERROR =
            "Unexpected error during decryption for alias '%s'"
        private const val LOG_DECRYPTION_SUCCESS =
            "Decryption successful for alias: %s"
        private const val LOG_AUTH_TAG_MISMATCH =
            "AEADBadLOG_TAGException: Decryption failed for alias '%s' due to authentication tag mismatch. Data may be tampered or incorrect key/IV."
        private const val LOG_DECRYPT_INVALID_ALGORITHM =
            "InvalidAlgorithmParameterException (likely GCMParameterSpec) during decryption for alias '%s'"
        
        // --- Delete Key ---
        private const val LOG_MISSING_KEY_FOR_DELETE =
            "Key with alias '%s' not found for deletion. Operation considered successful."
        private const val LOG_DELETED_KEY =
            "Successfully deleted key with alias: '%s'"
        private const val LOG_DELETE_KEY_FAIL =
            "Failed to delete key with alias '%s' from KeyStore"
        private const val LOG_UNEXPECTED_DELETE_ERROR =
            "Unexpected error while deleting key with alias '%s'"
    }
}