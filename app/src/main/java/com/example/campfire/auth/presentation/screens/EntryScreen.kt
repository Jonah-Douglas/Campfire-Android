package com.example.campfire.auth.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.campfire.R
import com.example.campfire.auth.presentation.navigation.AuthAction
import com.example.campfire.core.common.logging.Firelog


// JD TODO: add these
const val TAG_URL = "URL"
const val ANNOTATION_TERMS = "https://example.com/terms" // Replace with actual URL
const val ANNOTATION_PRIVACY = "https://example.com/privacy" // Replace with actual URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    onNavigateToEnterPhoneNumber: (AuthAction) -> Unit
) {
    Firelog.i("Composing EntryScreen.")
    
    val uriHandler = LocalUriHandler.current
    
    // Create legal disclaimer text
    val legalText = buildAnnotatedString {
        append(stringResource(id = R.string.entry_agreement_part1))
        append(" ")
        
        pushStringAnnotation(tag = TAG_URL, annotation = ANNOTATION_TERMS)
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append(stringResource(id = R.string.terms_of_service))
        }
        pop()
        
        append(stringResource(id = R.string.entry_agreement_part2))
        append(" ")
        
        pushStringAnnotation(tag = TAG_URL, annotation = ANNOTATION_PRIVACY)
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append(stringResource(id = R.string.privacy_policy))
        }
        pop()
        append(".")
    }
    
    // To store the TextLayoutResult
    val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    
    // Create background gradient
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            colorResource(R.color.onPrimaryContainer),
            colorResource(R.color.orange_flame_accent)
        )
    )
    
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(brush = gradientBrush)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(100.dp))
            Text(
                text = stringResource(id = R.string.campfire),
                style = MaterialTheme.typography.headlineLarge,
                color = colorResource(R.color.orange_flame)
            )
            Text(
                text = stringResource(id = R.string.slogan),
                style = MaterialTheme.typography.headlineSmall,
                color = colorResource(R.color.orange_flame)
            )
            Spacer(modifier = Modifier.height(350.dp))
            
            // --- Info Textbox with Clickable Links ---
            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = legalText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center
                    ),
                    onTextLayout = { result ->
                        textLayoutResult.value = result
                    },
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures { offset ->
                            textLayoutResult.value?.let { layoutResult ->
                                val position = layoutResult.getOffsetForPosition(offset)
                                legalText.getStringAnnotations(TAG_URL, position, position)
                                    .firstOrNull()?.let { annotation ->
                                        Firelog.i("Clicked on legal text link: ${annotation.item}")
                                        try {
                                            uriHandler.openUri(annotation.item)
                                        } catch (e: Exception) {
                                            Firelog.e("Failed to open URI: ${annotation.item}", e)
                                            // JD TODO: Maybe show a Snackbar to the user (would need scope then)
                                            // scope.launch { snackbarHostState.showSnackbar("Could not open link.") }
                                        }
                                    }
                            }
                        }
                    }
                )
            }
            // --- End Info Textbox ---
            
            Button(
                onClick = {
                    Firelog.i("Create Account button clicked. Navigating with action: ${AuthAction.REGISTER}")
                    onNavigateToEnterPhoneNumber(AuthAction.REGISTER)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.onPrimaryContainer),
                    contentColor = colorResource(id = R.color.primaryContainer)
                )
            ) {
                Text(stringResource(id = R.string.create_account_button_text))
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    Firelog.i("Sign In button clicked. Navigating with action: ${AuthAction.LOGIN}")
                    onNavigateToEnterPhoneNumber(AuthAction.LOGIN)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        id = R.string.sign_in_button_text
                    ),
                    color = colorResource(id = R.color.onPrimaryContainer)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EntryScreenPreview() {
    MaterialTheme {
        EntryScreen(onNavigateToEnterPhoneNumber = {})
    }
}