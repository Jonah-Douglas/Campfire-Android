package com.example.campfire

import android.app.Application
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.common.logging.TimberLogger
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber


@HiltAndroidApp
class CampfireApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber in debug builds
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // JD TODO: Plant a release tree for production logging
        }
        
        // Initialize Firelog wrapper
        Firelog.initialize(TimberLogger())
        
        Firelog.i("CampfireApplication created and logging initialized.")
    }
}