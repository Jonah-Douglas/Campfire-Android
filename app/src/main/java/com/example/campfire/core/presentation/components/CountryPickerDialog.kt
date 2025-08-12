package com.example.campfire.core.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.campfire.R
import com.example.campfire.auth.presentation.CountryUIModel


@Composable
fun CountryPickerDialog(
    countries: List<CountryUIModel>,
    onCountrySelected: (CountryUIModel) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.select_country_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(
                        countries,
                        key = { it.regionCode }) { country ->
                        CountryRow(country = country, onClick = {
                            onCountrySelected(country)
                            // onDismissRequest() // Consider if selection should auto-dismiss or if user confirms
                        })
                        HorizontalDivider()
                    }
                }
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun CountryRow(country: CountryUIModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = country.flagEmoji,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = country.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f) // Allow name to take available space
        )
        // Removed Spacer(Modifier.weight(1f)) to let name expand
        Text(text = country.dialCode, style = MaterialTheme.typography.bodyMedium)
    }
}
