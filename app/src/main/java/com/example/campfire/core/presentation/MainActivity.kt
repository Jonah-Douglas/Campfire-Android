package com.example.campfire.core.presentation

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campfire.auth.presentation.AuthActivity
import com.example.campfire.core.presentation.utils.CampfireTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    private val _isDataReady = MutableStateFlow(false)
    val isDataReady = _isDataReady.asStateFlow()
    
    init {
        Log.d("MainViewModel", "ViewModel Initialized: Loading initial data...")
        viewModelScope.launch {
            loadInitialResources()
            _isDataReady.value = true
            Log.d("MainViewModel", "Initial data loaded.")
        }
    }
    
    private suspend fun loadInitialResources() {
        // TODO: Replace this with your actual data loading logic
        // e.g., fetching user session, initial app configuration, etc.
        
        // Simulate network delay or complex data processing
        delay(3000) // Example: 3 seconds
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val fadeOut = android.view.animation.AlphaAnimation(1f, 0f).apply {
                duration = 300L
                setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        splashScreenViewProvider.remove()
                    }
                    
                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                })
            }
            splashScreenViewProvider.view.startAnimation(fadeOut)
        }
        
        setContent {
            CampfireTheme {
                val isDataReady by viewModel.isDataReady.collectAsState()
                
                LaunchedEffect(isDataReady) {
                    if (isDataReady) {
                        keepSplashOnScreen = false
                        Log.d("MainActivity", "Data is ready. Hiding splash screen.")
                        delay(100) // Let exit animation breathe
                        val intent = Intent(this@MainActivity, AuthActivity::class.java)
                        
                        val options = ActivityOptions.makeCustomAnimation(
                            this@MainActivity,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                        )
                        
                        startActivity(intent, options.toBundle())
                        finish()
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Log.d("MainActivity", "Splash screen active, waiting for data.")
                }
            }
        }
    }
}