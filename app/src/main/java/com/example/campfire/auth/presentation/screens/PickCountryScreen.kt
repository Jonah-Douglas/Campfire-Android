package com.example.campfire.auth.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campfire.R
import com.example.campfire.auth.presentation.AuthContract
import com.example.campfire.auth.presentation.AuthViewModel
import com.example.campfire.auth.presentation.CountryUIModel
import com.example.campfire.auth.presentation.SendOTPUIState
import com.example.campfire.core.common.logging.Firelog
import com.example.campfire.core.presentation.utils.CampfireTheme
import com.example.campfire.core.presentation.utils.getFlagEmojiForRegionCode


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickCountryScreen(
    viewModel: AuthContract = hiltViewModel<AuthViewModel>(),
    onCountrySelectedAndNavigateBack: (regionCode: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Firelog.i("Composing PickCountryScreen. ViewModel hash: ${viewModel.hashCode()}")
    
    val uiState by viewModel.sendOTPUIState.collectAsState()
    val availableCountries = uiState.availableCountries
    
    LaunchedEffect(availableCountries) {
        Firelog.d("Available countries updated. Count: ${availableCountries.size}")
        if (availableCountries.isNotEmpty()) {
            Firelog.v(
                "First few available countries: ${
                    availableCountries.take(5).joinToString { it.name }
                }"
            )
        }
    }
    
    val topLevelRegionCodes: List<String> = listOf("US", "AU", "CA", "GB")
    
    val topLevelCountries = availableCountries
        .filter { topLevelRegionCodes.contains(it.regionCode) }
        .sortedBy { topLevelRegionCodes.indexOf(it.regionCode) }
        .also { Firelog.v("Processed top-level countries. Count: ${it.size}") }
    
    val alphabetizedCountries = availableCountries
        .sortedBy { it.name }
        .also { Firelog.v("Processed alphabetized countries. Count: ${it.size}") }
    
    BackHandler {
        Firelog.d("Back button pressed. Navigating back.")
        onNavigateBack()
    }
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.select_country_dialog_title),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            Firelog.d("TopAppBar navigation icon (Back) clicked. Navigating back.")
                            onNavigateBack()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors()
                )
                
                HorizontalDivider()
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (topLevelCountries.isNotEmpty()) {
                items(topLevelCountries, key = { "top_${it.regionCode}" }) { country ->
                    CountryRow(
                        country = country,
                        onClick = {
                            Firelog.i("Top-level country selected: ${country.name} (${country.regionCode}). Navigating back.")
                            onCountrySelectedAndNavigateBack(country.regionCode)
                        }
                    )
                    HorizontalDivider()
                }
                item {
                    HorizontalDivider(
                        thickness = 8.dp,
                        color = colorResource(id = R.color.primaryContainer),
                    )
                }
            }
            
            items(alphabetizedCountries, key = { it.regionCode }) { country ->
                CountryRow(
                    country = country,
                    onClick = {
                        Firelog.i("Alphabetized country selected: ${country.name} (${country.regionCode}). Navigating back.")
                        onCountrySelectedAndNavigateBack(country.regionCode)
                    }
                )
                
                if (country != alphabetizedCountries.lastOrNull()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            if (topLevelCountries.isEmpty() && alphabetizedCountries.isEmpty()) {
                item {
                    LaunchedEffect(Unit) {
                        Firelog.w("Both top-level and alphabetized country lists are empty. Displaying empty state.")
                    }
                    Text(
                        text = stringResource(R.string.no_countries_available),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun CountryRow(
    country: CountryUIModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = country.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = "${getFlagEmojiForRegionCode(country.regionCode)} ${country.dialCode}",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End
        )
    }
}

// Helper function for creating a preview ViewModel instance
@Composable
private fun pickCountryPreviewViewModel(
    initialCountries: List<CountryUIModel> = defaultPreviewCountries
): AuthContract {
    val initialState = remember(initialCountries) {
        SendOTPUIState(availableCountries = initialCountries)
    }
    return remember(initialState) {
        FakePickCountryViewModel(initialSendOTPState = initialState)
    }
}

@Preview(name = "Default Country List", showBackground = true)
@Composable
fun PickCountryScreenPreview_Default() {
    CampfireTheme {
        PickCountryScreen(
            viewModel = pickCountryPreviewViewModel(),
            onCountrySelectedAndNavigateBack = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Empty Country List", showBackground = true)
@Composable
fun PickCountryScreenPreview_Empty() {
    CampfireTheme {
        PickCountryScreen(
            viewModel = pickCountryPreviewViewModel(initialCountries = emptyList()), // Pass empty list
            onCountrySelectedAndNavigateBack = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Few Top Level Countries", showBackground = true)
@Composable
fun PickCountryScreenPreview_FewTopLevel() {
    CampfireTheme {
        PickCountryScreen(
            viewModel = pickCountryPreviewViewModel(
                initialCountries = listOf( // Only provide the ones that would be top-level
                    CountryUIModel("United States", "US", "+1", "🇺🇸"),
                    CountryUIModel("United Kingdom", "GB", "+44", "🇬🇧")
                )
            ),
            onCountrySelectedAndNavigateBack = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Single Country", showBackground = true)
@Composable
fun PickCountryScreenPreview_SingleCountry() {
    CampfireTheme {
        PickCountryScreen(
            viewModel = pickCountryPreviewViewModel(
                initialCountries = listOf(
                    CountryUIModel("Canada", "CA", "+1", "🇨🇦")
                )
            ),
            onCountrySelectedAndNavigateBack = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Country Row Preview", showBackground = true, widthDp = 360)
@Composable
private fun CountryRowPreview() {
    CampfireTheme {
        CountryRow(
            country = CountryUIModel("Australia", "AU", "+61", "🇦🇺"),
            onClick = {}
        )
    }
}