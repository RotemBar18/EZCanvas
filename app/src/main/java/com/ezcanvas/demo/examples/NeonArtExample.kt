package com.ezcanvas.demo.examples

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ezcanvas.EzCanvas
import com.ezcanvas.EzToolbar
import com.ezcanvas.ToolbarControl
import com.ezcanvas.rememberEzCanvasState
import com.ezcanvas.model.Tool

internal val NeonPalette = listOf(
    Color(0xFF06B6D4), Color(0xFFF43F5E), Color(0xFF22C55E), Color(0xFFF59E0B), Color(0xFFE879F9),
)

private val NeonInk = Color(0xFF0B1120)

/**
 * One brush on a dark canvas.
 *
 * The toolbar takes its colours from the host `MaterialTheme`, so wrapping it in a dark scheme is
 * all it takes to make the bar match the artwork. Nothing about the toolbar itself is restyled.
 */
@Composable
internal fun NeonArtExample(onBack: () -> Unit) = ExampleScaffold(
    title = "Neon Art",
    subtitle = "EzToolbar in a dark theme",
    onBack = onBack,
    background = NeonInk,
) {
    val state = rememberEzCanvasState()

    var configured by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (configured) return@LaunchedEffect
        configured = true
        state.backgroundColor = Color(0xFF0F172A)
        state.tool = Tool.NEON
        state.strokeColor = NeonPalette.first()
        state.strokeWidthPx = 12f
        state.drawingName = "neon-art"
    }

    EzCanvas(
        state,
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(18.dp)),
    )

    // The same EzToolbar, handed a dark colour scheme.
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF06B6D4),
            onPrimary = Color(0xFF04212B),
            surface = Color(0xFF151C2E),
            onSurface = Color(0xFFE7EAF3),
            surfaceVariant = Color(0xFF1F2942),
            onSurfaceVariant = Color(0xFF9AA3BD),
            outlineVariant = Color(0xFF2C3A57),
        ),
    ) {
        ToolbarArea {
            EzToolbar(
                state,
                enabledTools = setOf(Tool.NEON),
                controls = setOf(
                    ToolbarControl.ColorPicker, ToolbarControl.StrokeWidth,
                    ToolbarControl.Undo, ToolbarControl.Clear, ToolbarControl.Export,
                ),
                palette = NeonPalette,
                allowCustomColor = false,
            )
        }
    }
}

/** Gallery card art: two glowing strokes on black. */
internal fun DrawScope.neonPreview() {
    val w = size.width
    val h = size.height
    fun glow(color: Color, path: Path) {
        drawPath(path, color, alpha = 0.25f, style = Stroke(width = 18f, cap = StrokeCap.Round))
        drawPath(path, color, style = Stroke(width = 4f, cap = StrokeCap.Round))
    }
    val first = Path().apply {
        moveTo(w * 0.15f, h * 0.7f)
        cubicTo(w * 0.3f, h * 0.2f, w * 0.5f, h * 0.8f, w * 0.6f, h * 0.4f)
    }
    val second = Path().apply {
        moveTo(w * 0.5f, h * 0.72f)
        cubicTo(w * 0.65f, h * 0.3f, w * 0.8f, h * 0.72f, w * 0.9f, h * 0.36f)
    }
    glow(Color(0xFF06B6D4), first)
    glow(Color(0xFFE879F9), second)
}
