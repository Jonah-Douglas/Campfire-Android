package com.example.campfire.core.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign // Import TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


/**
 * A Composable that displays text with an underline, simulating an input field appearance.
 * It's designed to be clickable, making it suitable for scenarios where tapping the "field"
 * should trigger an action, such as opening a dialog or a picker (e.g., for date or country selection).
 *
 * The component can display:
 * - A primary [value] string.
 * - A [hint] string if the [value] is empty.
 * - An optional [leadingIcon].
 * - An optional [showDropdownArrow] at the end.
 *
 * The text content ([value] or [hint]) is aligned to the end (right) of the available space
 * within the row, especially when a [leadingIcon] is present or when no leading icon is
 * present but a dropdown arrow is shown.
 *
 * Error states are visually indicated by changing the color of the underline and potentially
 * the tint of the text and icons, based on the [isError] and [errorColor] parameters.
 *
 * @param value The current text value to display in the component.
 * @param hint An optional hint text to display if [value] is empty.
 *             The hint will be styled with a secondary color.
 * @param onClick The lambda function to be invoked when this component is clicked.
 * @param modifier The [Modifier] to be applied to the root `Column` of this component.
 *                 Defaults to [Modifier].
 * @param textStyle The [TextStyle] to be applied to the displayed [value] or [hint].
 *                  Defaults to [LocalTextStyle.current] with a font size of 18.sp.
 *                  The color from this style will be used unless overridden by error state or hint display.
 * @param underlineColor The [Color] of the underline when the component is not in an error state ([isError] is false).
 *                       Defaults to [MaterialTheme.colorScheme.primary].
 * @param isError A boolean indicating whether the input is currently in an error state.
 *                If true, the [errorColor] will be used for the underline. The text and icon
 *                tints will also use [errorColor] unless their style explicitly defines a color.
 * @param errorColor The [Color] to use for the underline, text, and icon tint when [isError] is true.
 *                   Defaults to [MaterialTheme.colorScheme.error].
 * @param showDropdownArrow A boolean indicating whether to display a dropdown arrow icon
 *                          (e.g., [Icons.Filled.ArrowDropDown]) at the end of the component.
 *                          Defaults to `false`.
 * @param leadingIcon An optional Composable lambda that renders an icon or other content
 *                    at the beginning of the input field. If provided, the main text content
 *                    will be pushed towards the end.
 * @param dropdownArrowContentDescription The content description string for the dropdown arrow icon,
 *                                        used for accessibility. Defaults to "Select".
 */
@Composable
fun UnderlinedInputText(
    value: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    onClick: () -> Unit,
    textStyle: TextStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
    underlineColor: Color = MaterialTheme.colorScheme.primary, // Default underline color
    isError: Boolean = false,
    errorColor: Color = MaterialTheme.colorScheme.error, // Default error color
    showDropdownArrow: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    dropdownArrowContentDescription: String = "Select" // Default for accessibility
) {
    // Determine the color for the underline based on the error state
    val effectiveUnderlineColor = if (isError) errorColor else underlineColor
    
    // Determine the color for text content (value, icons)
    // Use errorColor if in error state, otherwise use color from textStyle or default surface color
    val currentContentColor = if (isError) {
        errorColor
    } else {
        textStyle.color.takeOrElse { MaterialTheme.colorScheme.onSurface }
    }
    
    // Color for the hint text
    val hintColor = if (isError) {
        errorColor // Hint also takes error color if applicable
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Display Leading Icon if provided
            if (leadingIcon != null) {
                Spacer(Modifier.width(8.dp)) // Padding before the icon
                leadingIcon()
                Spacer(Modifier.width(8.dp)) // Padding after the icon
            }
            
            // This Spacer pushes the text to the right if a leadingIcon is present.
            // If no leadingIcon, this Spacer is not added, allowing text to start from the left
            // unless it's pushed by its own weight modifier later.
            if (leadingIcon != null) {
                Spacer(Modifier.weight(1f))
            }
            
            // Display Text
            // Text is aligned to the End.
            val textToShow = value.ifEmpty { hint }
            if (textToShow != null) {
                Text(
                    text = textToShow,
                    style = textStyle.copy(
                        color = if (value.isEmpty() && hint != null) hintColor else currentContentColor,
                        textAlign = TextAlign.End
                    ),
                    // Add a weight only if there's NO leading icon, to push arrow to the end
                    modifier = if (leadingIcon == null) Modifier.weight(1f) else Modifier
                )
            }
            
            // Display Dropdown Arrow if requested
            if (showDropdownArrow) {
                Spacer(Modifier.width(4.dp)) // Small space before the arrow
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = dropdownArrowContentDescription,
                    tint = currentContentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp)) // Space between the text row and the underline
        
        // Underline
        HorizontalDivider(
            color = effectiveUnderlineColor, // Color changes based on error state
            thickness = 1.dp
        )
    }
}
