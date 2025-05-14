package com.example.campfire.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campfire.core.presentation.utils.CampfireTheme


@Preview(showBackground = true)
@Composable
fun LandingScreenPreview() {
    CampfireTheme {
        LandingScreen()
    }
}

@Composable
fun LandingScreen() {
    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize()) {
        Column (
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Campfire",
                fontSize = 16.sp,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sample landing screen built with Jetpack Compose",
                fontSize = 14.sp,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}