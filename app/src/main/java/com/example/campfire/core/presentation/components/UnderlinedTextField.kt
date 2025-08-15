package com.example.campfire.core.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


/**
 * A [VisualTransformation] that formats a raw phone number string (expected to be composed of digits)
 * into a US-style phone number visual representation: "XXX XXX XXXX".
 *
 * This transformation is purely for visual display and does not alter the actual
 * underlying [TextFieldValue.text] being edited. It correctly handles cursor positioning
 * and text selection transformations through its implemented [OffsetMapping].
 *
 * It is designed to format the first 10 digits it encounters in the input string.
 * Any characters beyond the 10th digit are ignored for formatting purposes but remain
 * in the underlying text.
 *
 * Example:
 * Input text: "1234567890" -> Visual output: "123 456 7890"
 * Input text: "123" -> Visual output: "123"
 * Input text: "123456" -> Visual output: "123 456"
 */
class UsPhoneNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Take only the first 10 characters for formatting to match US phone number length.
        val trimmed = if (text.text.length >= 10) text.text.substring(0..9) else text.text
        var out = "" // The string builder for the formatted output.
        
        // Iterate through the (potentially trimmed) input digits to build the formatted string.
        for (i in trimmed.indices) {
            out += trimmed[i]
            // Add a space after the 3rd digit (area code) and 6th digit (prefix) if there are more digits to follow
            if (i == 2 || i == 5) {
                if (i < trimmed.length - 1) {
                    out += " "
                }
            }
        }
        
        val offsetMapping = object : OffsetMapping {
            /**
             * Maps cursor/selection position from original (unformatted) text to transformed (formatted) text.
             */
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0 // No transformation at or before the start.
                
                var transformedOffset = offset
                // If original offset is past the first group of 3 digits, add 1 for the first space.
                if (offset > 3) {
                    transformedOffset += 1
                }
                // If original offset is past the second group of 3 digits, add 1 for the second space.
                if (offset > 6) {
                    transformedOffset += 1
                }
                
                // Ensure the transformed offset does not exceed the length of the formatted string.
                return transformedOffset.coerceAtMost(out.length)
            }
            
            /**
             * Maps cursor/selection position from transformed (formatted) text back to original (unformatted) text.
             */
            override fun transformedToOriginal(offset: Int): Int {
                // If transformed offset is past the first space (index 3), subtract 1 for that space.
                var originalOffset = offset
                if (offset > 3) { // After "XXX "
                    originalOffset -= 1
                }
                // If transformed offset is past the second space (index 7), subtract 1 for that space.
                if (offset > 7) { // After "XXX XXX "
                    originalOffset -= 1
                }
                // Ensure the original offset is within the bounds of the unformatted (trimmed) string.
                return originalOffset.coerceIn(0, trimmed.length)
            }
        }
        
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

/**
 * A Composable that provides a customizable text input field styled with an underline,
 * built upon [BasicTextField]. This component is designed for scenarios requiring
 * direct user text input with a clear, minimal underline visual.
 *
 * It supports standard text field features like hint text, keyboard options,
 * focus control, and visual transformations. Error states are visually indicated
 * by changing the underline color.
 *
 * This component is well-suited for forms and inputs where a distinct underlined field
 * is part of the design language, such as for entering names, phone numbers (potentially
 * with [UsPhoneNumberVisualTransformation]), OTP codes, or other short to medium length text.
 *
 * @param value The [TextFieldValue] representing the current text, selection, and composition
 *              state of the input field.
 * @param onValueChange The callback that is invoked when the [TextFieldValue] changes
 *                      due to user input or other programmatic modification.
 * @param hint An optional hint text (placeholder) to display within the input field when
 *             [value] is empty. The hint is displayed using the [decorationBox].
 * @param modifier The [Modifier] to be applied to the `BasicTextField` component.
 *                 Defaults to [Modifier].
 * @param textStyle The [TextStyle] to be applied to the input text.
 *                  Defaults to [LocalTextStyle.current] with a font size of 18.sp.
 *                  The text color is explicitly set to [MaterialTheme.colorScheme.onSurface]
 *                  within the component, overriding any color defined in the passed [textStyle].
 * @param keyboardOptions Software keyboard options that configure the keyboard's behavior,
 *                        such as [KeyboardType] or [ImeAction]. Defaults to [KeyboardOptions.Default].
 * @param keyboardActions Actions to be triggered in response to IME actions performed on the
 *                        software keyboard (e.g., when a "Done" or "Next" button is pressed).
 *                        Defaults to [KeyboardActions.Default].
 * @param focusRequester An optional [FocusRequester] that can be used to programmatically
 *                       control focus on this text field (e.g., request focus).
 *                       A default one is remembered if not provided.
 * @param singleLine When set to true (the default), this text field becomes a single horizontally
 *                   scrolling text field. When false, the text can wrap to multiple lines.
 * @param cursorBrush The [Brush] used for painting the cursor within the text field.
 *                    Defaults to a [SolidColor] of [MaterialTheme.colorScheme.primary].
 *                    This can be changed to [errorColor] if [isError] is true, if desired,
 *                    by passing it dynamically.
 * @param underlineColor The [Color] of the underline when the component is not in an error state
 *                       ([isError] is `false`). Defaults to [MaterialTheme.colorScheme.primary].
 * @param isError A boolean indicating whether the input is currently in an error state.
 *                If `true`, the [errorColor] will be used for the underline.
 *                Defaults to `false`.
 * @param errorColor The [Color] of the underline to be used when [isError] is `true`.
 *                   Defaults to [MaterialTheme.colorScheme.error].
 * @param visualTransformation A [VisualTransformation] that transforms the visual representation
 *                             of the input text. Useful for tasks like password masking
 *                             (e.g., `PasswordVisualTransformation`) or formatting numbers
 *                             (e.g., [UsPhoneNumberVisualTransformation]).
 *                             Defaults to [VisualTransformation.None].
 */
@Composable
fun UnderlinedTextField(
    value: TextFieldValue,
    modifier: Modifier = Modifier,
    onValueChange: (TextFieldValue) -> Unit,
    hint: String? = null,
    textStyle: TextStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester = remember { FocusRequester() },
    singleLine: Boolean = true,
    cursorBrush: Brush = SolidColor(MaterialTheme.colorScheme.primary),
    underlineColor: Color = MaterialTheme.colorScheme.primary, // Color for normal state
    isError: Boolean = false,
    errorColor: Color = MaterialTheme.colorScheme.error, // Color for error state
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    // Determine the color for the underline based on the error state.
    val currentUnderlineColor = if (isError) errorColor else underlineColor
    
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.focusRequester(focusRequester),
        // Apply the provided textStyle but ensure the text color is from the theme's onSurface.
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        cursorBrush = cursorBrush,
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            Column {
                // Determine if the hint should be displayed.
                val displayHint = value.text.isEmpty() && hint != null
                
                if (displayHint) {
                    Text(
                        text = hint,
                        style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                } else {
                    innerTextField()
                }
                
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = currentUnderlineColor
                )
            }
        }
    )
}