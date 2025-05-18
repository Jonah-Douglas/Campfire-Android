package com.example.campfire.core.data.utils

import android.util.Base64
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.random.Random


private const val AES_MODE = "AES/GCM/NoPadding"
private const val IV_SIZE = 12 // 96 bits
private const val TAG_LENGTH = 128 // 128 bits

fun encryptData(data: String?, secretKey: SecretKey): String? {
    if (data.isNullOrEmpty()) {
        return null
    }
    
    val cipher = Cipher.getInstance(AES_MODE)
    val iv = ByteArray(IV_SIZE).apply {
        Random.nextBytes(this)
    }
    
    val spec = GCMParameterSpec(TAG_LENGTH, iv)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
    val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
    val combined = iv + encrypted
    
    return Base64.encodeToString(combined, Base64.DEFAULT)
}

fun decryptData(encryptedData: String?, secretKey: SecretKey): String? {
    if (encryptedData.isNullOrEmpty()) {
        return null
    }
    
    return try {
        val combined = Base64.decode(encryptedData, Base64.DEFAULT)
        
        if (combined.size < IV_SIZE) {
            Log.e("DecryptData", "Encrypted data is too short to contain a valid IV.")
            return null
        }
        
        val iv = combined.copyOfRange(0, IV_SIZE)
        val encryptedPayload = combined.copyOfRange(IV_SIZE, combined.size)
        val cipher = Cipher.getInstance(AES_MODE)
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decrypted = cipher.doFinal(encryptedPayload)
        
        String(decrypted, Charsets.UTF_8)
    } catch (e: IllegalArgumentException) {
        Log.e("DecryptData", "Invalid Base64 string provided for decryption: ${e.message}", e)
        null
    } catch (e: javax.crypto.AEADBadTagException) {
        Log.e(
            "DecryptData",
            "Decryption failed due to AEADBadTagException (data tampered or wrong key): ${e.message}",
            e
        )
        null
    } catch (e: Exception) {
        Log.e("DecryptData", "Decryption failed with exception: ${e.message}", e)
        null
    }
}