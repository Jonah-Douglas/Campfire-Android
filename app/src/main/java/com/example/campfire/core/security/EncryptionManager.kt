package com.example.campfire.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.Charset
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton


interface IEncryptionManager {
    fun encrypt(data: String, alias: String): EncryptedData?
    fun decrypt(encryptedData: EncryptedData, alias: String): String?
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
    
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    
    private fun getSecretKey(alias: String): SecretKey {
        return keyStore.getKey(alias, null) as? SecretKey ?: generateSecretKey(alias)
    }
    
    private fun generateSecretKey(alias: String): SecretKey {
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val parameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).run {
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            // JD TODO: Do we want a biometric prompt here?
            build()
        }
        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }
    
    override fun encrypt(data: String, alias: String): EncryptedData? {
        return try {
            val secretKey = getSecretKey(alias)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val ciphertext = cipher.doFinal(data.toByteArray(Charset.forName("UTF-8")))
            EncryptedData(ciphertext, cipher.iv)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    override fun decrypt(encryptedData: EncryptedData, alias: String): String? {
        return try {
            val secretKey = getSecretKey(alias)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, encryptedData.initializationVector)
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decryptedBytes = cipher.doFinal(encryptedData.ciphertext)
            String(decryptedBytes, Charset.forName("UTF-8"))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}