package com.ezcanvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ezcanvas.model.BackgroundPattern
import com.ezcanvas.model.LineStyle
import com.ezcanvas.model.Tool
import com.ezcanvas.model.isShape

/**
 * Every control [EzToolbar] can render. The toolbar is a complete, ready-made control surface:
 * the developer enables features by passing a subset here (and [EzToolbar]'s `enabledTools`),
 * and the bar renders all of them — no toolbar to design or wire by hand.
 */
enum class ToolbarControl {
    ToolSelector, ColorPicker, StrokeWidth, Opacity, EraserSize, Style,
    Background, Pattern, Image, Undo, Redo, Clear, Export
}

/** All controls (the default). */
val DefaultToolbarControls: Set<ToolbarControl> = ToolbarControl.entries.toSet()

/** Default stroke-color swatches. */
val DefaultSwatches: List<Color> = listOf(
    Color.Black, Color.White,
    Color(0xFF14B8A6), Color(0xFF06B6D4), Color(0xFFF43F5E),
    Color(0xFFF59E0B), Color(0xFF6366F1), Color(0xFF22C55E),
)

/** Default background swatches. */
val DefaultBackgrounds: List<Color> = listOf(
    Color.White, Color(0xFFF4F7FB), Color(0xFFFFFDE7), Color(0xFF0F172A), Color(0xFF0B2A4A),
)

/**
 * A complete, configurable toolbar bound to an [EzCanvasState]. Pass the same state you gave
 * [EzCanvas]; choose which tools appear with [enabledTools] and which controls with [controls].
 * Colors follow the app's [MaterialTheme], so the bar matches the host's theme.
 *
 * [Export] works out of the box (shares a PNG); pass [onExport] to override it. The bar lays its
 * sections in a [Column] with no internal scroll — place it in a scrollable container (or a bottom
 * sheet) if you enable many controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EzToolbar(
    state: EzCanvasState,
    modifier: Modifier = Modifier,
    controls: Set<ToolbarControl> = DefaultToolbarControls,
    enabledTools: Set<Tool> = Tool.entries.toSet(),
    palette: List<Color> = DefaultSwatches,
    backgroundPalette: List<Color> = DefaultBackgrounds,
    onExport: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val pickImage = rememberBackgroundImagePicker(state)
    val exportAction = onExport ?: { state.shareAsPng(context) }

    Surface(tonalElevation = 2.dp, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Current selection indicator.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(state.strokeColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                )
                Text(state.tool.label(), style = MaterialTheme.typography.labelLarge)
            }

            if (ToolbarControl.ToolSelector in controls) {
                Label("Tool")
                ScrollRow {
                    Tool.entries.filter { it in enabledTools }.forEach { tool ->
                        FilterChip(
                            selected = state.tool == tool,
                            onClick = { state.tool = tool },
                            label = { Text(tool.label()) },
                        )
                    }
                }
            }

            if (ToolbarControl.ColorPicker in controls) {
                Label("Color")
                SwatchRow(palette, state.strokeColor) { state.strokeColor = it }
            }

            if (ToolbarControl.StrokeWidth in controls) {
                LabeledSlider("Width", state.strokeWidthPx, 2f, 60f) { state.strokeWidthPx = it }
            }
            if (ToolbarControl.Opacity in controls) {
                LabeledSlider("Opacity", state.strokeAlpha, 0f, 1f) { state.strokeAlpha = it }
            }
            if (ToolbarControl.EraserSize in controls) {
                LabeledSlider("Eraser", state.eraserWidthPx, 10f, 120f) { state.eraserWidthPx = it }
            }

            if (ToolbarControl.Style in controls && (state.tool == Tool.PEN || state.tool.isShape)) {
                Label("Line style")
                ScrollRow {
                    LineStyle.entries.forEach { style ->
                        FilterChip(
                            selected = state.lineStyle == style,
                            onClick = { state.lineStyle = style },
                            label = { Text(style.label()) },
                        )
                    }
                }
            }

            if (ToolbarControl.Background in controls) {
                Label("Background")
                SwatchRow(backgroundPalette, state.backgroundColor) {
                    state.backgroundColor = it
                    state.backgroundImage = null
                }
            }
            if (ToolbarControl.Pattern in controls) {
                Label("Pattern")
                ScrollRow {
                    BackgroundPattern.entries.forEach { pattern ->
                        FilterChip(
                            selected = state.backgroundPattern == pattern,
                            onClick = { state.backgroundPattern = pattern },
                            label = { Text(pattern.name) },
                        )
                    }
                }
            }
            if (ToolbarControl.Image in controls) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = pickImage) { Text("Upload image") }
                    if (state.backgroundImage != null) {
                        OutlinedButton(onClick = { state.backgroundImage = null }) { Text("Remove") }
                    }
                }
            }

            if (hasActions(controls)) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Spacer(Modifier.weight(1f))
                    if (ToolbarControl.Undo in controls) {
                        TextButton(onClick = { state.undo() }, enabled = state.canUndo) { Text("Undo") }
                    }
                    if (ToolbarControl.Redo in controls) {
                        TextButton(onClick = { state.redo() }, enabled = state.canRedo) { Text("Redo") }
                    }
                    if (ToolbarControl.Clear in controls) {
                        TextButton(onClick = { state.clear() }, enabled = !state.isEmpty) { Text("Clear") }
                    }
                    if (ToolbarControl.Export in controls) {
                        TextButton(onClick = exportAction, enabled = !state.isEmpty) { Text("Export") }
                    }
                }
            }
        }
    }
}

private fun hasActions(controls: Set<ToolbarControl>): Boolean =
    ToolbarControl.Undo in controls || ToolbarControl.Redo in controls ||
        ToolbarControl.Clear in controls || ToolbarControl.Export in controls

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ScrollRow(content: @Composable () -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun SwatchRow(colors: List<Color>, selected: Color, onPick: (Color) -> Unit) {
    ScrollRow {
        colors.forEach { color ->
            val sel = color == selected
            Box(
                Modifier
                    .size(if (sel) 32.dp else 28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (sel) 2.dp else 1.dp,
                        color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    )
                    .clickable { onPick(color) },
            )
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(64.dp))
        Slider(value = value, onValueChange = onChange, valueRange = min..max, modifier = Modifier.weight(1f))
    }
}

private fun Tool.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

private fun LineStyle.label(): String = when (this) {
    LineStyle.Solid -> "Solid"
    LineStyle.Dotted -> "Dotted"
    LineStyle.Dashed -> "Dashed"
    LineStyle.DashDot -> "Dash-dot"
}
