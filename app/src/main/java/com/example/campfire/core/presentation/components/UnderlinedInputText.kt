package com.example.campfire.core.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


/**
 * A composable that displays a text input field with an underline, suitable for brief inputs.
 * It appears as clickable text, and can optionally display a hint when the value is empty
 * and a dropdown arrow.
 *
 * This component is typically used for displaying information that, when clicked,
 * triggers an action like opening a picker or a dialog (e.g., country code selection).
 *
 * @param value The current text value to display.
 * @param hint An optional hint text to display if [value] is empty.
 * @param onClick The lambda to be invoked when this component is clicked.
 * @param modifier The [Modifier] to be applied to this component.
 * @param textStyle The [TextStyle] to be applied to the displayed [value] or [hint].
 *                  Defaults to [LocalTextStyle.current] with a font size of 18.sp.
 * @param underlineColor The [Color] of the underline when the component is not in an error state.
 *                       Defaults to [MaterialTheme.colorScheme.primary].
 * @param isError A boolean indicating whether the input is currently in an error state.
 *                If true, the [errorColor] will be used for the underline and text.
 * @param errorColor The [Color] of the underline and text when [isError] is true.
 *                   Defaults to [MaterialTheme.colorScheme.error].
 * @param showDropdownArrow A boolean indicating whether to display a dropdown arrow icon
 *                          at the end of the component. Defaults to false.
 */
@Composable
fun UnderlinedInputText(
    value: String,
    hint: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
    underlineColor: Color = MaterialTheme.colorScheme.primary,
    isError: Boolean = false,
    errorColor: Color = MaterialTheme.colorScheme.error,
    showDropdownArrow: Boolean = false
) {
    val effectiveUnderlineColor = if (isError) errorColor else underlineColor
    val textColor = if (isError) errorColor else MaterialTheme.colorScheme.onSurface
    
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (value.isEmpty() && hint != null) {
                Text(
                    text = hint,
                    style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.weight(1f, fill = false)
                    //.padding(end = if (showDropdownArrow) 4.dp else 0.dp)
                )
            } else {
                Text(
                    text = value,
                    style = textStyle.copy(color = textColor),
                    modifier = Modifier.weight(1f, fill = false)
                    //.padding(end = if (showDropdownArrow) 4.dp else 0.dp)
                )
            }
            
            if (showDropdownArrow) {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Select country code",
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(
            color = effectiveUnderlineColor,
            thickness = 1.dp
        )
    }
}
