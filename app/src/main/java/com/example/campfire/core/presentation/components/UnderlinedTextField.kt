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
 * A [VisualTransformation] that formats a raw phone number string (expected to be digits)
 * into a US-style phone number format: "XXX XXX XXXX".
 *
 * This transformation is purely visual and does not change the underlying text
 * being edited. It correctly handles cursor positioning and text selection
 * via its [OffsetMapping].
 *
 * It expects the input text to be composed primarily of digits. It will attempt
 * to format the first 10 digits it encounters.
 */
class UsPhoneNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 10) text.text.substring(0..9) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 2 || i == 5) {
                if (i < trimmed.length - 1) {
                    out += " "
                }
            }
        }
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                
                var transformedOffset = offset
                if (offset > 3) {
                    transformedOffset += 1
                }
                if (offset > 6) {
                    transformedOffset += 1
                }
                
                return transformedOffset.coerceAtMost(out.length)
            }
            
            override fun transformedToOriginal(offset: Int): Int {
                var originalOffset = offset
                if (offset > 3) {
                    originalOffset -= 1
                }
                if (offset > 7) {
                    originalOffset -= 1
                }
                return originalOffset.coerceIn(0, trimmed.length)
            }
        }
        
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

/**
 * A composable that provides a text input field styled with an underline.
 * It's built upon [BasicTextField] and allows for customization of appearance and behavior.
 *
 * This component is suitable for user input where a distinct underlined field is desired,
 * such as for entering phone numbers, OTP codes, or other short pieces of text.
 *
 * @param value The [TextFieldValue] representing the current state of the text input
 *              (text, selection, composition).
 * @param onValueChange The callback that is invoked when the [TextFieldValue] changes.
 * @param hint An optional hint text to display within the input field when [value] is empty.
 *             The hint is displayed within the [BasicTextField]'s decorationBox.
 * @param modifier The [Modifier] to be applied to this component.
 * @param textStyle The [TextStyle] to be applied to the input text.
 *                  Defaults to [LocalTextStyle.current] with a font size of 18.sp.
 *                  The text color will be [MaterialTheme.colorScheme.onSurface].
 * @param keyboardOptions Software keyboard options that instruct the keyboard on how to behave
 *                        (e.g., [KeyboardType], [ImeAction]). Defaults to [KeyboardOptions.Default].
 * @param keyboardActions Actions to be triggered in response to IME actions performed on the
 *                        software keyboard (e.g., when the "Done" button is pressed).
 *                        Defaults to [KeyboardActions.Default].
 * @param focusRequester An optional [FocusRequester] that can be used to control focus
 *                       on this text field programmatically.
 * @param singleLine When set to true, this text field becomes a single horizontally scrolling
 *                   text field instead of wrapping onto multiple lines. Defaults to true.
 * @param cursorBrush The [Brush] to be used for painting the cursor.
 *                    Defaults to a [SolidColor] of [MaterialTheme.colorScheme.primary].
 * @param underlineColor The [Color] of the underline when the component is not in an error state.
 *                       Defaults to [MaterialTheme.colorScheme.primary].
 * @param isError A boolean indicating whether the input is currently in an error state.
 *                If true, the [errorColor] will be used for the underline.
 * @param errorColor The [Color] of the underline when [isError] is true.
 *                   Defaults to [MaterialTheme.colorScheme.error].
 * @param visualTransformation A [VisualTransformation] that transforms the visual representation
 *                             of the input text (e.g., for password masking or phone number formatting).
 *                             Defaults to [VisualTransformation.None].
 */
@Composable
fun UnderlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    hint: String? = null,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester = remember { FocusRequester() },
    singleLine: Boolean = true,
    cursorBrush: Brush = SolidColor(MaterialTheme.colorScheme.primary),
    underlineColor: Color = MaterialTheme.colorScheme.primary,
    isError: Boolean = false,
    errorColor: Color = MaterialTheme.colorScheme.error,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val color = if (isError) errorColor else underlineColor
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.focusRequester(focusRequester),
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        cursorBrush = cursorBrush,
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            Column {
                val displayHint = value.text.isEmpty() && hint != null
                if (displayHint) {
                    Text(
                        text = hint,
                        style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
                if (!displayHint || value.text.isNotEmpty()) {
                    innerTextField()
                } else if (value.text.isEmpty() && hint == null) {
                    innerTextField()
                }
                
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = color
                )
            }
        }
    )
}