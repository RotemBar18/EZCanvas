package com.ezcanvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ezcanvas.model.BackgroundPattern
import com.ezcanvas.model.LineStyle
import androidx.compose.ui.platform.LocalConfiguration
import com.ezcanvas.model.Tool
import com.ezcanvas.ui.DialogAction
import com.ezcanvas.ui.EzDialog
import com.ezcanvas.ui.EzPromptDialog
import com.ezcanvas.render.dashIntervals
import com.ezcanvas.model.isShape

/**
 * Every control [EzToolbar] can render. The toolbar is a complete, ready-made control surface:
 * the developer enables features by passing a subset here (and [EzToolbar]'s `enabledTools`),
 * and the bar renders all of them. There is no toolbar to design or wire by hand.
 */
enum class ToolbarControl {
    ToolSelector, ColorPicker, StrokeWidth, Opacity, EraserSize, Style,
    Background, Pattern, Image, Rename, Undo, Redo, Clear, Export
}

/** All controls (the default). */
val DefaultToolbarControls: Set<ToolbarControl> = ToolbarControl.entries.toSet()

/** Default stroke-color swatches. */
val DefaultPalette: List<Color> = listOf(
    Color.Black, Color.White,
    Color(0xFF14B8A6), Color(0xFF06B6D4), Color(0xFFF43F5E),
    Color(0xFFF59E0B), Color(0xFF6366F1), Color(0xFF22C55E),
)

/** Default background swatches. */
val DefaultBackgroundPalette: List<Color> = listOf(
    Color.White, Color(0xFFF4F7FB), Color(0xFFFFFDE7), Color(0xFF0F172A), Color(0xFF0B2A4A),
)

/**
 * A complete, configurable toolbar bound to an [EzCanvasState]. Pass the same state you gave
 * [EzCanvas]; choose which tools appear with [enabledTools] and which controls with [controls].
 * Colors follow the app's [MaterialTheme], so the bar matches the host's theme.
 *
 * Set [allowCustomColor] to false to lock users to the [palette] you supply. Leave it true to add
 * a palette button that opens a full color chooser.
 *
 * Export works out of the box by sharing a PNG. Pass [onExport] to override it.
 *
 * The bar fits the space it is given, so it never needs a scrolling wrapper around it. With every
 * control enabled its sections stack to roughly 700dp, which would swallow a phone screen, so two
 * things keep it in check. It caps itself at [maxHeight] and scrolls inside that. And on a short
 * screen, which in practice means landscape, it drops to a single scrolling row about 90dp tall,
 * because stacking there would leave no room to draw.
 *
 * @param maxHeight the tallest the stacked layout may grow before it scrolls inside itself.
 */
@Composable
fun EzToolbar(
    state: EzCanvasState,
    modifier: Modifier = Modifier,
    controls: Set<ToolbarControl> = DefaultToolbarControls,
    enabledTools: Set<Tool> = Tool.entries.toSet(),
    palette: List<Color> = DefaultPalette,
    backgroundPalette: List<Color> = DefaultBackgroundPalette,
    allowCustomColor: Boolean = true,
    onExport: (() -> Unit)? = null,
    maxHeight: Dp = 300.dp,
) {
    val context = LocalContext.current
    val pickImage = rememberBackgroundImagePicker(state)
    val exportAction = onExport ?: { state.shareAsPng(context) }
    val tools = Tool.entries.filter { it in enabledTools }
    var pickingStrokeColor by remember { mutableStateOf(false) }
    var pickingBackground by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }

    // A landscape phone is around 390dp tall. Stacking sections there leaves nothing to draw on,
    // so the bar lays out sideways instead. The screen is the right thing to measure: the height
    // handed to the bar is often unbounded, because a Column gives the canvas the weight.
    val compact = LocalConfiguration.current.screenHeightDp < CompactScreenHeightDp

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        CompositionLocalProvider(LocalCompactToolbar provides compact) {
            ToolbarLayout(compact, maxHeight) {
            if (ToolbarControl.Rename in controls) {
                Row(
                    RenameWidth
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { renaming = true }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        state.drawingName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Filled.Edit,
                        "Rename drawing",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }

            if (ToolbarControl.ToolSelector in controls && tools.isNotEmpty()) {
                ScrollRow {
                    tools.forEach { tool ->
                        IconButtonTile(
                            icon = tool.icon(),
                            description = tool.label(),
                            selected = state.tool == tool,
                            onClick = { state.tool = tool },
                        )
                    }
                }
            }

            if (ToolbarControl.ColorPicker in controls) {
                Section("Color") {
                    SwatchRow(
                        colors = palette,
                        selected = state.strokeColor,
                        allowCustom = allowCustomColor,
                        onCustom = { pickingStrokeColor = true },
                        onPick = { state.strokeColor = it },
                    )
                }
            }

            if (ToolbarControl.StrokeWidth in controls) {
                ValueSlider("Size", state.strokeWidthPx, 2f, 60f, "${state.strokeWidthPx.toInt()}") {
                    state.strokeWidthPx = it
                }
            }
            if (ToolbarControl.Opacity in controls) {
                ValueSlider("Opacity", state.strokeAlpha, 0f, 1f, "${(state.strokeAlpha * 100).toInt()}%") {
                    state.strokeAlpha = it
                }
            }
            if (ToolbarControl.EraserSize in controls) {
                ValueSlider("Eraser", state.eraserWidthPx, 10f, 120f, "${state.eraserWidthPx.toInt()}") {
                    state.eraserWidthPx = it
                }
            }

            if (ToolbarControl.Style in controls && (state.tool == Tool.Pen || state.tool.isShape)) {
                Section("Line style") {
                    ScrollRow {
                        LineStyle.entries.forEach { style ->
                            SampleTile(
                                selected = state.lineStyle == style,
                                width = 58.dp,
                                onClick = { state.lineStyle = style },
                            ) { tint ->
                                drawLine(
                                    color = tint,
                                    start = Offset(6f, size.height / 2f),
                                    end = Offset(size.width - 6f, size.height / 2f),
                                    strokeWidth = 3f,
                                    cap = if (style == LineStyle.Dotted || style == LineStyle.DashDot) {
                                        StrokeCap.Round
                                    } else {
                                        StrokeCap.Butt
                                    },
                                    pathEffect = dashIntervals(style, 3f)?.let { PathEffect.dashPathEffect(it, 0f) },
                                )
                            }
                        }
                    }
                }
            }

            if (ToolbarControl.Background in controls) {
                Section("Background") {
                    SwatchRow(
                        colors = backgroundPalette,
                        selected = state.backgroundColor,
                        allowCustom = allowCustomColor,
                        onCustom = { pickingBackground = true },
                        onPick = {
                            state.backgroundColor = it
                            state.backgroundImage = null
                        },
                    )
                }
            }
            if (ToolbarControl.Pattern in controls) {
                ScrollRow {
                    BackgroundPattern.entries.forEach { pattern ->
                        SampleTile(
                            selected = state.backgroundPattern == pattern,
                            width = 44.dp,
                            onClick = { state.backgroundPattern = pattern },
                        ) { tint -> drawPatternSample(pattern, tint) }
                    }
                }
            }
            if (ToolbarControl.Image in controls) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PillButton("Upload image", Icons.Filled.Image, filled = true, onClick = pickImage)
                    if (state.backgroundImage != null) {
                        PillButton("Remove", null, filled = false) { state.backgroundImage = null }
                    }
                }
            }

            if (hasActions(controls)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (ToolbarControl.Undo in controls) {
                        IconButtonTile(Icons.AutoMirrored.Filled.Undo, "Undo", enabled = state.canUndo) { state.undo() }
                    }
                    if (ToolbarControl.Redo in controls) {
                        IconButtonTile(Icons.AutoMirrored.Filled.Redo, "Redo", enabled = state.canRedo) { state.redo() }
                    }
                    if (ToolbarControl.Clear in controls) {
                        IconButtonTile(Icons.Filled.DeleteOutline, "Clear", enabled = !state.isEmpty) { state.clear() }
                    }
                    if (ToolbarControl.Export in controls) {
                        Spacer(Modifier.weight(1f))
                        PillButton(
                            "Export",
                            Icons.Filled.IosShare,
                            filled = true,
                            enabled = !state.isEmpty,
                            onClick = exportAction,
                        )
                    }
                }
            }
            }
        }
    }

    if (pickingStrokeColor) {
        ColorChooserDialog(
            title = "Pick a color",
            selected = state.strokeColor,
            onPick = { state.strokeColor = it },
            onDismiss = { pickingStrokeColor = false },
        )
    }
    if (pickingBackground) {
        ColorChooserDialog(
            title = "Background color",
            selected = state.backgroundColor,
            onPick = {
                state.backgroundColor = it
                state.backgroundImage = null
            },
            onDismiss = { pickingBackground = false },
        )
    }
    if (renaming) {
        EzPromptDialog(
            title = "Drawing name",
            placeholder = "Name",
            initial = state.drawingName,
            confirmLabel = "Save",
            helper = { typed -> "Exports as ${typed.trim().ifBlank { "drawing" }}.png" },
            onConfirm = { state.drawingName = it },
            onDismiss = { renaming = false },
        )
    }
}

private fun hasActions(controls: Set<ToolbarControl>): Boolean =
    ToolbarControl.Undo in controls || ToolbarControl.Redo in controls ||
        ToolbarControl.Clear in controls || ToolbarControl.Export in controls

// --- Color chooser ----------------------------------------------------------

/** A full spectrum of hues and shades, plus a grayscale row. Built once and reused. */
private val SpectrumRows: List<List<Color>> = buildList {
    val hues = List(12) { it * 30f }
    add(hues.map { Color.hsv(it, 0.35f, 1f) })  // pastel
    add(hues.map { Color.hsv(it, 0.65f, 1f) })  // light
    add(hues.map { Color.hsv(it, 1f, 1f) })     // vivid
    add(hues.map { Color.hsv(it, 1f, 0.72f) })  // deep
    add(hues.map { Color.hsv(it, 1f, 0.45f) })  // dark
    add(List(12) { Color.hsv(0f, 0f, 1f - it / 11f) }) // white to black
}

/** Opens when the developer allows custom colors, so users are not limited to the fixed swatches. */
@Composable
private fun ColorChooserDialog(
    title: String,
    selected: Color,
    onPick: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    EzDialog(title = title, onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            SpectrumRows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    row.forEach { color ->
                        val isSel = color == selected
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(7.dp))
                                .background(color)
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) scheme.primary else scheme.outlineVariant,
                                    shape = RoundedCornerShape(7.dp),
                                )
                                .clickable {
                                    onPick(color)
                                    onDismiss()
                                },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row {
            Spacer(Modifier.weight(1f))
            DialogAction("Done", filled = true, onClick = onDismiss)
        }
    }
}

// --- Building blocks --------------------------------------------------------

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/** Below this screen height the bar lays out sideways. Covers landscape phones, not small tablets. */
private const val CompactScreenHeightDp = 500

/**
 * True while the bar is in its sideways layout. The section composables read it rather than take a
 * parameter each, because it changes only two things: nothing may scroll horizontally inside a bar
 * that already scrolls that way, and nothing may ask to fill an unbounded width.
 */
private val LocalCompactToolbar = compositionLocalOf { false }

/** Sections stack when there is room, and run in one scrolling line when there is not. */
@Composable
private fun ToolbarLayout(compact: Boolean, maxHeight: Dp, content: @Composable () -> Unit) {
    if (compact) {
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    } else {
        Column(
            Modifier
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) { content() }
    }
}

/** Sliders need a definite width; sideways there is no parent width to fill. */
private val SliderWidth: Modifier
    @Composable get() = if (LocalCompactToolbar.current) Modifier.width(190.dp) else Modifier.fillMaxWidth()

private val RenameWidth: Modifier
    @Composable get() = if (LocalCompactToolbar.current) Modifier.width(150.dp) else Modifier.fillMaxWidth()

@Composable
private fun ScrollRow(content: @Composable () -> Unit) {
    // Sideways, the whole bar is already one scrolling row, so these must not scroll again.
    val scroll = if (LocalCompactToolbar.current) Modifier else Modifier.horizontalScroll(rememberScrollState())
    Row(
        scroll,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun IconButtonTile(
    icon: ImageVector,
    description: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tint = when {
        selected -> scheme.onPrimary
        enabled -> scheme.onSurface
        else -> scheme.onSurface.copy(alpha = 0.3f)
    }
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) scheme.primary else scheme.surfaceVariant)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(21.dp))
    }
}

/** A wider tile that previews a value, used for line styles and background patterns. */
@Composable
private fun SampleTile(
    selected: Boolean,
    width: Dp,
    onClick: () -> Unit,
    sample: DrawScope.(Color) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(width = width, height = 40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) scheme.primary else scheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            sample(if (selected) scheme.onPrimary else scheme.onSurface)
        }
    }
}

@Composable
private fun SwatchRow(
    colors: List<Color>,
    selected: Color,
    allowCustom: Boolean,
    onCustom: () -> Unit,
    onPick: (Color) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    ScrollRow {
        colors.forEach { color ->
            val isSel = color == selected
            // Fixed footprint with an outer ring, so picking a color never shifts the row.
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                if (isSel) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(2.dp, scheme.primary, CircleShape),
                    )
                }
                Box(
                    Modifier
                        .size(if (isSel) 26.dp else 30.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, scheme.outlineVariant, CircleShape)
                        .clickable { onPick(color) },
                )
            }
        }
        if (allowCustom) {
            val isCustom = colors.none { it == selected }
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isCustom) selected else scheme.surfaceVariant)
                        .border(
                            width = if (isCustom) 2.dp else 1.dp,
                            color = if (isCustom) scheme.primary else scheme.outlineVariant,
                            shape = CircleShape,
                        )
                        .clickable { onCustom() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Palette,
                        "More colors",
                        tint = if (isCustom) Color.White else scheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ValueSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    readout: String,
    onChange: (Float) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        SliderWidth,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = scheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp),
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = min..max,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = scheme.primary,
                activeTrackColor = scheme.primary,
                inactiveTrackColor = scheme.surfaceVariant,
            ),
        )
        Text(
            readout,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = scheme.onSurface,
            modifier = Modifier.width(38.dp),
        )
    }
}

@Composable
private fun PillButton(
    label: String,
    icon: ImageVector?,
    filled: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val alpha = if (enabled) 1f else 0.35f
    val content = (if (filled) scheme.onPrimary else scheme.onSurface).copy(alpha = alpha)
    val base = Modifier
        .clip(RoundedCornerShape(12.dp))
        .background(if (filled) scheme.primary.copy(alpha = alpha) else Color.Transparent)
    val shaped = if (filled) base else base.border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp))
    Row(
        shaped
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = content, modifier = Modifier.size(17.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

// --- Tool presentation ------------------------------------------------------

private fun Tool.icon(): ImageVector = when (this) {
    Tool.Pen -> Icons.Filled.Edit
    Tool.Marker -> Icons.Filled.Brush
    Tool.Neon -> Icons.Filled.AutoAwesome
    Tool.Calligraphy -> Icons.Filled.Gesture
    Tool.Eraser -> Icons.AutoMirrored.Filled.Backspace
    Tool.Line -> Icons.Filled.HorizontalRule
    Tool.Square -> Icons.Outlined.CropSquare
    Tool.Circle -> Icons.Outlined.Circle
    Tool.Bucket -> Icons.Filled.FormatColorFill
    Tool.Text -> Icons.Filled.TextFields
}

private fun Tool.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

private fun DrawScope.drawPatternSample(pattern: BackgroundPattern, tint: Color) {
    val w = size.width
    val h = size.height
    when (pattern) {
        BackgroundPattern.None -> drawLine(
            tint,
            Offset(w * 0.2f, h * 0.8f),
            Offset(w * 0.8f, h * 0.2f),
            strokeWidth = 2f,
            cap = StrokeCap.Round,
        )

        BackgroundPattern.Grid -> {
            for (i in 1..2) {
                drawLine(tint, Offset(w * i / 3f, 0f), Offset(w * i / 3f, h), 1.5f)
                drawLine(tint, Offset(0f, h * i / 3f), Offset(w, h * i / 3f), 1.5f)
            }
        }

        BackgroundPattern.Dots -> {
            for (row in 0..2) for (col in 0..2) {
                drawCircle(tint, radius = 1.6f, center = Offset(w * (col + 0.5f) / 3f, h * (row + 0.5f) / 3f))
            }
        }

        BackgroundPattern.Lined -> {
            for (i in 1..3) {
                drawLine(tint, Offset(0f, h * i / 4f), Offset(w, h * i / 4f), 1.5f)
            }
        }
    }
}
