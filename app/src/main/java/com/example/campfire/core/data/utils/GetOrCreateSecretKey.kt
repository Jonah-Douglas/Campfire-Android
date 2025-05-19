package com.example.campfire.core.data.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey


private const val KEY_ALIAS = "com.example.campfire.encryption_key"

fun getOrCreateSecretKey(): SecretKey {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    
    // Check if the key already exists
    keyStore.getKey(KEY_ALIAS, null)?.let {
        return it as SecretKey
    }
    
    // If not, create a new key
    val keyGenerator = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES,
        "AndroidKeyStore"
    )
    val keyGenParameterSpec = KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    ).apply {
        setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        setKeySize(256)
    }.build()
    keyGenerator.init(keyGenParameterSpec)
    
    return keyGenerator.generateKey()
}