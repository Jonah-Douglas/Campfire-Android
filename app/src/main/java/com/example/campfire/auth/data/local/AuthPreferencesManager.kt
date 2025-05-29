package com.example.campfire.auth.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject


private const val KEY_REMEMBER_ME = "remember_me_auth"

@Suppress("unused")
class AuthPreferencesManager @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    // JD TODO: Add remember me button that stores user info
    fun setShouldRememberUser(remember: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_REMEMBER_ME, remember) }
    }
    
    fun getShouldRememberUser(): Boolean {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)
    }
}