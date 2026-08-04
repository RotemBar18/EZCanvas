package com.ezcanvas.demo.examples

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.ezcanvas.model.BackgroundPattern
import com.ezcanvas.model.Tool

internal val KidsPalette = listOf(
    Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF22C55E),
    Color(0xFF3B82F6), Color(0xFFA855F7), Color(0xFFEC4899),
)

/**
 * A children's drawing app. Only three controls are switched on, and the colour picker is locked
 * to a fixed set of bright colours, so there is nothing fiddly for a child to get wrong.
 */
@Composable
internal fun KidsDoodleExample(onBack: () -> Unit) = ExampleScaffold(
    title = "Kids Doodle",
    subtitle = "EzCanvas + EzToolbar, three controls",
    onBack = onBack,
    background = Color(0xFFFFFDE7),
) {
    val state = rememberEzCanvasState()

    var configured by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (configured) return@LaunchedEffect
        configured = true
        state.backgroundColor = Color(0xFFFFFDE7)
        state.backgroundPattern = BackgroundPattern.Dots
        state.strokeColor = KidsPalette.first()
        state.strokeWidthPx = 26f
        state.drawingName = "my-drawing"
    }

    EzCanvas(
        state,
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, Color(0xFFEFE7C4), RoundedCornerShape(20.dp)),
    )

    ToolbarArea {
        EzToolbar(
            state,
            enabledTools = setOf(Tool.PEN, Tool.MARKER, Tool.BUCKET, Tool.ERASER),
            controls = setOf(ToolbarControl.ToolSelector, ToolbarControl.ColorPicker, ToolbarControl.Clear),
            palette = KidsPalette,
            allowCustomColor = false,
        )
    }
}

/** Gallery card art: bright overlapping scribbles. */
internal fun DrawScope.kidsPreview() {
    val w = size.width
    val h = size.height
    drawCircle(Color(0xFFFB6F61), radius = h * 0.2f, center = Offset(w * 0.3f, h * 0.5f))
    val path = Path().apply {
        moveTo(w * 0.15f, h * 0.7f)
        cubicTo(w * 0.3f, h * 0.35f, w * 0.45f, h * 0.85f, w * 0.62f, h * 0.5f)
    }
    drawPath(path, Color(0xFFF59E0B), style = Stroke(width = 12f, cap = StrokeCap.Round))
    drawLine(Color(0xFF3B82F6), Offset(w * 0.5f, h * 0.3f), Offset(w * 0.85f, h * 0.4f), 12f, cap = StrokeCap.Round)
    drawCircle(Color(0xFF22C55E), radius = h * 0.1f, center = Offset(w * 0.78f, h * 0.68f))
}
