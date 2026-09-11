package com.mindspring.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mindspring.app.ui.theme.InputShape
import com.mindspring.app.ui.theme.MsTheme

/** Outlined, 8dp-radius field with a floating label; teal 2dp border when focused. */
@Composable
fun MsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isPassword: Boolean = false,
    isError: Boolean = false,
    errorText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val c = MsTheme.colors
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it, color = c.textTertiary) } },
        isError = isError,
        supportingText = if (isError && errorText != null) ({ Text(errorText) }) else null,
        singleLine = singleLine,
        minLines = minLines,
        shape = InputShape,
        textStyle = MaterialTheme.typography.bodyLarge,
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (isPassword) keyboardOptions.copy(keyboardType = KeyboardType.Password) else keyboardOptions,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (visible) "Hide password" else "Show password",
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.tealInk,
            unfocusedBorderColor = c.textTertiary.copy(alpha = 0.45f),
            focusedLabelColor = c.tealInk,
            unfocusedLabelColor = c.textTertiary,
            cursorColor = c.tealInk,
            focusedTextColor = c.textPrimary,
            unfocusedTextColor = c.textPrimary,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
    )
}

/** Pill-shaped Week / Month / Year style selector. */
@Composable
fun <T> SegmentedTabs(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val c = MsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(c.cardMuted)
            .padding(4.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            val bg by animateColorAsState(if (active) c.card else Color.Transparent, label = "segment")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .then(if (active && !c.isDark) Modifier.shadow(2.dp, CircleShape) else Modifier)
                    .clip(CircleShape)
                    .background(bg)
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (active) c.textPrimary else c.textSecondary,
                )
            }
        }
    }
}

/** Selectable outlined chip that fills teal when chosen (feeling tags, categories). */
@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = MsTheme.colors.teal,
    selectedContent: Color = MsTheme.colors.onTeal,
) {
    val c = MsTheme.colors
    val bg by animateColorAsState(if (selected) selectedColor else Color.Transparent, label = "chipBg")
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = bg,
        border = if (selected) null else BorderStroke(1.dp, c.tealInk.copy(alpha = 0.7f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) selectedContent else c.tealInk,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
        )
    }
}

/** Circular toggle for weekday selection in the habit editor. */
@Composable
fun DayToggle(letter: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = MsTheme.colors
    val bg by animateColorAsState(if (selected) c.amber else Color.Transparent, label = "dayBg")
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .then(if (selected) Modifier else Modifier.border(1.dp, c.divider, CircleShape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letter,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) c.onAmber else c.textSecondary,
        )
    }
}

@Composable
fun msSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = MsTheme.colors.amber,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = MsTheme.colors.textTertiary,
    uncheckedTrackColor = MsTheme.colors.cardMuted,
    uncheckedBorderColor = MsTheme.colors.divider,
)
