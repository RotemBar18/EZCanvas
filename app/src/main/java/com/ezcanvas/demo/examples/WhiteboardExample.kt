package com.ezcanvas.demo.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ezcanvas.EzCanvas
import com.ezcanvas.EzToolbar
import com.ezcanvas.ToolbarControl
import com.ezcanvas.rememberEzCanvasState
import com.ezcanvas.shareAsPng
import com.ezcanvas.demo.ui.EzColors
import com.ezcanvas.model.BackgroundPattern
import com.ezcanvas.model.Tool

/**
 * A teacher's board inside a lesson screen. The grid background and the shape tools are what make
 * a board useful for diagrams, and undo and redo matter because a board is worked on live.
 */
@Composable
internal fun WhiteboardExample(onBack: () -> Unit) = ExampleScaffold(
    title = "Classroom Whiteboard",
    subtitle = "EzCanvas + EzToolbar, shapes enabled",
    onBack = onBack,
    background = EzColors.AppBg,
) {
    val state = rememberEzCanvasState(
        tool = Tool.Marker,
        strokeWidthPx = 10f,
        backgroundPattern = BackgroundPattern.Grid,
    )
    val context = LocalContext.current
    var board by rememberSaveable { mutableIntStateOf(1) }

    LaunchedEffect(board) { state.drawingName = "lesson-4-board-$board" }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "LESSON 4  /  FRACTIONS",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = EzColors.SectionLabel,
            )
            Text(
                "Board $board",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = EzColors.Ink,
            )
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(EzColors.ChipBg)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            Text("Class 7B", style = MaterialTheme.typography.labelMedium, color = EzColors.Muted)
        }
    }

    Box(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, EzColors.Divider, RoundedCornerShape(16.dp)),
    ) {
        EzCanvas(state, Modifier.fillMaxSize())
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = { state.shareAsPng(context) },
            enabled = !state.isEmpty,
            modifier = Modifier.weight(1f),
        ) { Text("Share board") }
        OutlinedButton(
            onClick = {
                state.clear()
                board++
            },
            modifier = Modifier.weight(1f),
        ) { Text("New board") }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, EzColors.Divider, RoundedCornerShape(14.dp)),
    ) {
        EzToolbar(
            state,
            enabledTools = setOf(Tool.Marker, Tool.Pen, Tool.Line, Tool.Square, Tool.Circle, Tool.Eraser),
            controls = setOf(
                ToolbarControl.ToolSelector, ToolbarControl.ColorPicker, ToolbarControl.StrokeWidth,
                ToolbarControl.Style, ToolbarControl.Undo, ToolbarControl.Redo,
            ),
        )
    }
}

/** Gallery card art: a box and an arrow on a faint grid. */
internal fun DrawScope.whiteboardPreview() {
    val w = size.width
    val h = size.height
    val grid = Color(0xFFE9E7E1)
    var x = w * 0.12f
    while (x < w) {
        drawLine(grid, Offset(x, 0f), Offset(x, h), 1f)
        x += w * 0.12f
    }
    var y = h * 0.2f
    while (y < h) {
        drawLine(grid, Offset(0f, y), Offset(w, y), 1f)
        y += h * 0.2f
    }
    drawRoundRect(
        Color(0xFF1B1B1F),
        topLeft = Offset(w * 0.16f, h * 0.34f),
        size = Size(w * 0.26f, h * 0.32f),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(3f),
    )
    drawLine(Color(0xFF1B1B1F), Offset(w * 0.44f, h * 0.5f), Offset(w * 0.6f, h * 0.5f), 3f, cap = StrokeCap.Round)
    drawLine(Color(0xFF1B1B1F), Offset(w * 0.6f, h * 0.5f), Offset(w * 0.54f, h * 0.44f), 3f, cap = StrokeCap.Round)
    drawLine(Color(0xFF1B1B1F), Offset(w * 0.6f, h * 0.5f), Offset(w * 0.54f, h * 0.56f), 3f, cap = StrokeCap.Round)
    drawLine(
        Color(0xFF2563EB).copy(alpha = 0.5f),
        Offset(w * 0.64f, h * 0.34f),
        Offset(w * 0.86f, h * 0.62f),
        10f,
        cap = StrokeCap.Square,
    )
}
