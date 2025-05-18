package com.example.campfire.core.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.campfire.auth.presentation.AuthActivity
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column {
                    Text(text = "Welcome to the App!", modifier = Modifier.padding(16.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    // Button to start the authentication flow
                    Button(onClick = {
                        val intent = Intent(this@MainActivity, AuthActivity::class.java)
                        startActivity(intent)
                    }) {
                        Text(text = "Go to Authentication")
                    }
                    // Add more UI elements as needed for your main app
                }
            }
        }
    }
}