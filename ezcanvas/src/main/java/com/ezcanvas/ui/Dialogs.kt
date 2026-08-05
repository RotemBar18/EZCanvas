package com.ezcanvas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/*
 * The library's own dialog styling, so a prompt looks like the rest of EZCanvas instead of a stock
 * alert. Everything is driven by MaterialTheme, so it still adopts the host app's colours.
 */

/** Rounded card, generous padding, title, then whatever the caller needs. */
@Composable
internal fun EzDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = scheme.surface,
            // No tonal elevation: Material tints elevated surfaces with the primary colour, which
            // washes the card in the accent instead of leaving it a clean sheet.
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(16.dp))
                content()
            }
        }
    }
}

/**
 * A dialog that asks for one line of text. Used for placing text on the canvas and for naming the
 * drawing, so both share the same look.
 */
@Composable
internal fun EzPromptDialog(
    title: String,
    placeholder: String,
    initial: String = "",
    confirmLabel: String,
    helper: (String) -> String? = { null },
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var value by remember { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }

    // Open with the keyboard ready, so the prompt takes one tap instead of two.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val confirm = {
        val trimmed = value.trim()
        if (trimmed.isNotEmpty()) onConfirm(trimmed)
        onDismiss()
    }

    EzDialog(title = title, onDismiss = onDismiss) {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.primary),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { confirm() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            decorationBox = { innerField ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.surfaceVariant)
                        .border(1.dp, scheme.outlineVariant, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                ) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                    innerField()
                }
            },
        )

        helper(value)?.let { hint ->
            Spacer(Modifier.height(10.dp))
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = scheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Spacer(Modifier.weight(1f))
            DialogAction("Cancel", filled = false, onClick = onDismiss)
            DialogAction(confirmLabel, filled = true, enabled = value.isNotBlank(), onClick = confirm)
        }
    }
}

/**
 * Pill buttons rather than the default text buttons of an alert. The confirm uses the theme's
 * secondary, which reads as a solid ink button, so a dialog does not compete with the accent
 * colour used for selection inside the toolbar.
 */
@Composable
internal fun DialogAction(
    label: String,
    filled: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val fade = if (enabled) 1f else 0.35f
    val content = (if (filled) scheme.onSecondary else scheme.onSurface).copy(alpha = fade)
    val base = Modifier
        .clip(RoundedCornerShape(12.dp))
        .background(if (filled) scheme.secondary.copy(alpha = fade) else Color.Transparent)
    val shaped = if (filled) base else base.border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp))
    Box(
        shaped
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = content)
    }
}
