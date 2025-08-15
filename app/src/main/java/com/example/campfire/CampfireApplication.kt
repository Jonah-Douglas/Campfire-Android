package com.example.campfire

import android.app.Application
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.common.logging.TimberLogger
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber


/**
 * Custom [Application] class for the Campfire application.
 *
 * This class serves as the main entry point and global application state container.
 * Key responsibilities include:
 * 1.  Initializing Hilt for dependency injection throughout the application. This is achieved
 *     via the [HiltAndroidApp] annotation, which triggers Dagger Hilt's code generation
 *     to create the application-level dependency container.
 * 2.  Setting up application-wide logging. In debug builds, it plants a [Timber.DebugTree]
 *     for detailed log output. For release builds, a production-appropriate logging tree
 *     should be planted.
 * 3.  Initializing the [Firelog] wrapper with a [TimberLogger] instance, allowing for a
 *     consistent logging API across the app that delegates to Timber.
 *
 * This class is instantiated when the application process is created, before any Activity,
 * Service, or other components (unless content providers).
 */
@HiltAndroidApp
class CampfireApplication : Application() {
    
    /**
     * Called when the application is starting, before any other application objects have been created.
     * This is where global initialization should occur.
     */
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber logging framework.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Firelog.d("Timber DebugTree planted.")
        } else {
            // JD TODO: Plant a release tree for production logging
            Firelog.w("Timber release tree not yet implemented. Logging will be minimal in release builds.")
        }
        
        // Initialize Firelog wrapper
        Firelog.initialize(TimberLogger())
        
        Firelog.i("CampfireApplication created and logging initialized.")
    }
}