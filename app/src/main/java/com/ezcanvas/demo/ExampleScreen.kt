package com.ezcanvas.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ezcanvas.EzCanvas
import com.ezcanvas.EzToolbar
import com.ezcanvas.ToolbarControl
import com.ezcanvas.rememberEzCanvasState
import com.ezcanvas.demo.ui.EzColors
import com.ezcanvas.model.BackgroundPattern
import com.ezcanvas.model.Tool

/** Card metadata for the gallery; the real demo is the matching composable in [ExampleScreen]. */
data class ExampleInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    val tags: List<String>,
    val background: Color,
    val preview: DrawScope.() -> Unit,
)

/** Seven genuinely different products, each a deliberate slice of the library. */
val Examples: List<ExampleInfo> = listOf(
    ExampleInfo("signature", "Signature Pad", "Capture a name to a PNG", listOf("Pen only", "Export"), Color.White) { signaturePreview() },
    ExampleInfo("kids", "Kids Doodle", "Big, bright, forgiving", listOf("Fat brushes", "Bucket fill"), Color(0xFFFFFDE7)) { kidsPreview() },
    ExampleInfo("painting", "Painting Studio", "The whole library, on", listOf("Every brush", "Every control"), Color(0xFFFCFBF8)) { paintingPreview() },
    ExampleInfo("markup", "Photo Markup", "Circle it, ship it", listOf("Shapes + photo", "Dashed"), Color(0xFFEFEDE7)) { markupPreview() },
    ExampleInfo("whiteboard", "Classroom Whiteboard", "Teach on a grid", listOf("Markers + shapes", "Undo/redo"), Color.White) { whiteboardPreview() },
    ExampleInfo("game", "Drawing Game", "Sketch, pass, guess", listOf("One pen", "A few colors"), Color.White) { gamePreview() },
    ExampleInfo("neon", "Neon Art", "Glow on black", listOf("Neon brush", "Dark canvas"), Color(0xFF0F172A)) { neonPreview() },
)

private val KidsPalette = listOf(
    Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFFA855F7), Color(0xFFEC4899),
)
private val NeonPalette = listOf(
    Color(0xFF06B6D4), Color(0xFFF43F5E), Color(0xFF22C55E), Color(0xFFF59E0B), Color(0xFFE879F9),
)
private val MarkupPalette = listOf(Color(0xFFE0463B), Color(0xFFF59E0B), Color(0xFF22C55E), Color(0xFF2563EB))
private val GamePalette = listOf(Color(0xFF4F46E5), Color(0xFFFB6F61), Color(0xFF14B8A6), Color(0xFF111827))

/** Opens one example by id. Each branch is a tiny, self-contained use of the library. */
@Composable
fun ExampleScreen(exampleId: String, onBack: () -> Unit) {
    when (exampleId) {
        "kids" -> KidsDoodle(onBack)
        "painting" -> PaintingStudio(onBack)
        "markup" -> PhotoMarkup(onBack)
        "whiteboard" -> Whiteboard(onBack)
        "game" -> DrawingGame(onBack)
        "neon" -> NeonArt(onBack)
        else -> SignaturePad(onBack)
    }
}

// --- Each example: pick the config that fits the product, drop in EzCanvas + EzToolbar. ---

@Composable
private fun SignaturePad(onBack: () -> Unit) = ExampleScaffold("Signature Pad", "Pen only · clear · export", onBack) {
    val state = rememberEzCanvasState()
    LaunchedEffect(Unit) { state.strokeWidthPx = 5f }
    EzCanvas(state, Modifier.weight(1f).fillMaxWidth())
    ToolbarArea {
        EzToolbar(
            state,
            enabledTools = setOf(Tool.PEN),
            controls = setOf(ToolbarControl.Clear, ToolbarControl.Export),
        )
    }
}

@Composable
private fun KidsDoodle(onBack: () -> Unit) = ExampleScaffold("Kids Doodle", "Fat brushes · bucket fill", onBack) {
    val state = rememberEzCanvasState()
    LaunchedEffect(Unit) {
        state.backgroundColor = Color(0xFFFFFDE7)
        state.backgroundPattern = BackgroundPattern.Dots
        state.strokeColor = KidsPalette.first()
        state.strokeWidthPx = 26f
    }
    EzCanvas(state, Modifier.weight(1f).fillMaxWidth())
    ToolbarArea {
        EzToolbar(
            state,
            enabledTools = setOf(Tool.PEN, Tool.MARKER, Tool.BUCKET, Tool.ERASER),
            controls = setOf(ToolbarControl.ToolSelector, ToolbarControl.ColorPicker, ToolbarControl.Clear),
            palette = KidsPalette,
        )
    }
}

@Composable
private fun PaintingStudio(onBack: () -> Unit) = ExampleScaffold("Painting Studio", "Every brush · every control", onBack) {
    val state = rememberEzCanvasState()
    LaunchedEffect(Unit) { state.strokeColor = Color(0xFF6366F1) }
    EzCanvas(state, Modifier.weight(1f).fillMaxWidth())
    ToolbarArea {
        // The full library: every tool and every control enabled.
        EzToolbar(state)
    }
}

@Composable
private fun PhotoMarkup(onBack: () -> Unit) = ExampleScaffold("Photo Markup", "Upload a photo, then mark it up", onBack) {
    val state = rememberEzCanvasState()
    LaunchedEffect(Unit) {
        state.strokeColor = MarkupPalette.first()
        state.tool = Tool.CIRCLE
        state.strokeWidthPx = 5f
    }
    EzCanvas(state, Modifier.weight(1f).fillMaxWidth())
    ToolbarArea {
        EzToolbar(
            state,
            enabledTools = setOf(Tool.PEN, Tool.MARKER, Tool.LINE, Tool.SQUARE, Tool.CIRCLE),
            controls = setOf(
                ToolbarControl.ToolSelector, ToolbarControl.ColorPicker, ToolbarControl.StrokeWidth,
                ToolbarControl.Style, ToolbarControl.Image, ToolbarControl.Undo, ToolbarControl.Clear, ToolbarControl.Export,
            ),
            palette = MarkupPalette,
        )
    }
}

@Composable
private fun Whiteboard(onBack: () -> Unit) = ExampleScaffold("Classroom Whiteboard", "Shapes · line styles · grid", onBack) {
    val state = rememberEzCanvasState()
    LaunchedEffect(Unit) {
        state.backgroundPattern = BackgroundPattern.Grid
        state.tool = Tool.MARKER
        state.strokeWidthPx = 10f
    }
    EzCanvas(state, Modifier.weight(1f).fillMaxWidth())
    ToolbarArea {
        EzToolbar(
            state,
            enabledTools = setOf(Tool.MARKER, Tool.PEN, Tool.LINE, Tool.SQUARE, Tool.CIRCLE, Tool.ERASER),
            controls = setOf(
                ToolbarControl.ToolSelector, ToolbarControl.ColorPicker, ToolbarControl.StrokeWidth,
                ToolbarControl.Style, ToolbarControl.Undo, ToolbarControl.Redo, ToolbarControl.Clear,
            ),
        )
    }
}

@Composable
private fun DrawingGame(onBack: () -> Unit) = ExampleScaffold("Drawing Game", "Sketch fast · one pen · few colors", onBack) {
    val state = rememberEzCanvasState()
    LaunchedEffect(Unit) {
        state.strokeColor = GamePalette.first()
        state.strokeWidthPx = 8f
    }
    EzCanvas(state, Modifier.weight(1f).fillMaxWidth())
    ToolbarArea {
        EzToolbar(
            state,
            enabledTools = setOf(Tool.PEN, Tool.ERASER),
            controls = setOf(ToolbarControl.ColorPicker, ToolbarControl.Undo, ToolbarControl.Clear),
            palette = GamePalette,
        )
    }
}

@Composable
private fun NeonArt(onBack: () -> Unit) = ExampleScaffold("Neon Art", "Glow brush on dark", onBack) {
    val state = rememberEzCanvasState()
    LaunchedEffect(Unit) {
        state.backgroundColor = Color(0xFF0F172A)
        state.tool = Tool.NEON
        state.strokeColor = NeonPalette.first()
        state.strokeWidthPx = 12f
    }
    EzCanvas(state, Modifier.weight(1f).fillMaxWidth())
    ToolbarArea {
        EzToolbar(
            state,
            enabledTools = setOf(Tool.NEON),
            controls = setOf(ToolbarControl.ColorPicker, ToolbarControl.StrokeWidth, ToolbarControl.Undo, ToolbarControl.Clear),
            palette = NeonPalette,
        )
    }
}

/** Back bar + title, then the example's canvas and toolbar. */
@Composable
private fun ExampleScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(EzColors.Surface)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(EzColors.ChipBg)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = EzColors.Ink, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = EzColors.Ink)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = EzColors.Subtle)
            }
        }
        content()
    }
}

/** Keeps a (possibly tall) toolbar bounded and scrollable so the canvas keeps its space. */
@Composable
private fun ColumnScope.ToolbarArea(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .verticalScroll(rememberScrollState()),
    ) { content() }
}

// --- Card previews: a small illustration that looks like each product. ---

private fun DrawScope.signaturePreview() {
    val w = size.width
    val h = size.height
    drawLine(Color(0xFFD8D5CE), Offset(w * 0.12f, h * 0.72f), Offset(w * 0.88f, h * 0.72f), 2f)
    val mx = w * 0.16f
    val my = h * 0.72f
    drawLine(Color(0xFFE0463B), Offset(mx - 6f, my - 14f), Offset(mx + 6f, my - 2f), 2.5f)
    drawLine(Color(0xFFE0463B), Offset(mx + 6f, my - 14f), Offset(mx - 6f, my - 2f), 2.5f)
    val p = Path().apply {
        moveTo(w * 0.22f, h * 0.6f)
        cubicTo(w * 0.30f, h * 0.28f, w * 0.38f, h * 0.82f, w * 0.48f, h * 0.52f)
        cubicTo(w * 0.55f, h * 0.26f, w * 0.62f, h * 0.74f, w * 0.72f, h * 0.48f)
        cubicTo(w * 0.77f, h * 0.32f, w * 0.84f, h * 0.58f, w * 0.9f, h * 0.44f)
    }
    drawPath(p, Color(0xFF1B1B1F), style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.kidsPreview() {
    val w = size.width
    val h = size.height
    drawCircle(Color(0xFFFB6F61), radius = h * 0.2f, center = Offset(w * 0.3f, h * 0.5f))
    val p = Path().apply {
        moveTo(w * 0.15f, h * 0.7f)
        cubicTo(w * 0.3f, h * 0.35f, w * 0.45f, h * 0.85f, w * 0.62f, h * 0.5f)
    }
    drawPath(p, Color(0xFFF59E0B), style = Stroke(width = 12f, cap = StrokeCap.Round))
    drawLine(Color(0xFF3B82F6), Offset(w * 0.5f, h * 0.3f), Offset(w * 0.85f, h * 0.4f), 12f, cap = StrokeCap.Round)
    drawCircle(Color(0xFF22C55E), radius = h * 0.1f, center = Offset(w * 0.78f, h * 0.68f))
}

private fun DrawScope.paintingPreview() {
    val w = size.width
    val h = size.height
    drawLine(Color(0xFF14B8A6).copy(alpha = 0.6f), Offset(w * 0.12f, h * 0.7f), Offset(w * 0.6f, h * 0.3f), 22f, cap = StrokeCap.Round)
    drawLine(Color(0xFFFB6F61).copy(alpha = 0.6f), Offset(w * 0.2f, h * 0.35f), Offset(w * 0.72f, h * 0.7f), 18f, cap = StrokeCap.Round)
    drawLine(Color(0xFF6366F1).copy(alpha = 0.85f), Offset(w * 0.45f, h * 0.25f), Offset(w * 0.9f, h * 0.55f), 8f, cap = StrokeCap.Round)
    drawCircle(Color(0xFFF59E0B).copy(alpha = 0.8f), radius = h * 0.12f, center = Offset(w * 0.8f, h * 0.34f))
}

private fun DrawScope.markupPreview() {
    val w = size.width
    val h = size.height
    drawRoundRect(Color(0xFFDCD8D0), topLeft = Offset(w * 0.16f, h * 0.16f), size = Size(w * 0.68f, h * 0.66f), cornerRadius = CornerRadius(10f, 10f))
    drawCircle(Color(0xFFC4BEB1), radius = h * 0.1f, center = Offset(w * 0.66f, h * 0.38f))
    drawLine(Color(0xFFC4BEB1), Offset(w * 0.2f, h * 0.62f), Offset(w * 0.8f, h * 0.62f), 4f)
    val dash = PathEffect.dashPathEffect(floatArrayOf(9f, 7f))
    drawCircle(Color(0xFFE0463B), radius = h * 0.17f, center = Offset(w * 0.42f, h * 0.5f), style = Stroke(width = 3f, pathEffect = dash))
    drawLine(Color(0xFFE0463B), Offset(w * 0.42f, h * 0.5f), Offset(w * 0.18f, h * 0.26f), 3f, cap = StrokeCap.Round)
}

private fun DrawScope.whiteboardPreview() {
    val w = size.width
    val h = size.height
    val grid = Color(0xFFE9E7E1)
    var x = w * 0.12f
    while (x < w) { drawLine(grid, Offset(x, 0f), Offset(x, h), 1f); x += w * 0.12f }
    var y = h * 0.2f
    while (y < h) { drawLine(grid, Offset(0f, y), Offset(w, y), 1f); y += h * 0.2f }
    drawRoundRect(Color(0xFF1B1B1F), topLeft = Offset(w * 0.16f, h * 0.34f), size = Size(w * 0.26f, h * 0.32f), cornerRadius = CornerRadius(6f, 6f), style = Stroke(3f))
    drawLine(Color(0xFF1B1B1F), Offset(w * 0.44f, h * 0.5f), Offset(w * 0.6f, h * 0.5f), 3f, cap = StrokeCap.Round)
    drawLine(Color(0xFF1B1B1F), Offset(w * 0.6f, h * 0.5f), Offset(w * 0.54f, h * 0.44f), 3f, cap = StrokeCap.Round)
    drawLine(Color(0xFF1B1B1F), Offset(w * 0.6f, h * 0.5f), Offset(w * 0.54f, h * 0.56f), 3f, cap = StrokeCap.Round)
    drawLine(Color(0xFF2563EB).copy(alpha = 0.5f), Offset(w * 0.64f, h * 0.34f), Offset(w * 0.86f, h * 0.62f), 10f, cap = StrokeCap.Square)
}

private fun DrawScope.gamePreview() {
    val w = size.width
    val h = size.height
    val ink = Color(0xFF4F46E5)
    val cx = w * 0.5f
    val cy = h * 0.54f
    val r = h * 0.24f
    drawCircle(ink, radius = r, center = Offset(cx, cy), style = Stroke(3.5f))
    drawLine(ink, Offset(cx - r * 0.7f, cy - r * 0.7f), Offset(cx - r * 0.95f, cy - r * 1.4f), 3.5f, cap = StrokeCap.Round)
    drawLine(ink, Offset(cx - r * 0.95f, cy - r * 1.4f), Offset(cx - r * 0.2f, cy - r * 0.95f), 3.5f, cap = StrokeCap.Round)
    drawLine(ink, Offset(cx + r * 0.7f, cy - r * 0.7f), Offset(cx + r * 0.95f, cy - r * 1.4f), 3.5f, cap = StrokeCap.Round)
    drawLine(ink, Offset(cx + r * 0.95f, cy - r * 1.4f), Offset(cx + r * 0.2f, cy - r * 0.95f), 3.5f, cap = StrokeCap.Round)
    drawCircle(ink, radius = 3f, center = Offset(cx - r * 0.35f, cy - r * 0.1f))
    drawCircle(ink, radius = 3f, center = Offset(cx + r * 0.35f, cy - r * 0.1f))
    drawCircle(Color(0xFFFB6F61), radius = 3.5f, center = Offset(cx, cy + r * 0.2f))
    drawLine(ink, Offset(cx - r * 0.1f, cy + r * 0.28f), Offset(cx - r * 0.7f, cy + r * 0.18f), 2f)
    drawLine(ink, Offset(cx + r * 0.1f, cy + r * 0.28f), Offset(cx + r * 0.7f, cy + r * 0.18f), 2f)
}

private fun DrawScope.neonPreview() {
    val w = size.width
    val h = size.height
    fun glow(color: Color, path: Path) {
        drawPath(path, color, alpha = 0.25f, style = Stroke(width = 18f, cap = StrokeCap.Round))
        drawPath(path, color, style = Stroke(width = 4f, cap = StrokeCap.Round))
    }
    val p1 = Path().apply {
        moveTo(w * 0.15f, h * 0.7f)
        cubicTo(w * 0.3f, h * 0.2f, w * 0.5f, h * 0.8f, w * 0.6f, h * 0.4f)
    }
    val p2 = Path().apply {
        moveTo(w * 0.5f, h * 0.72f)
        cubicTo(w * 0.65f, h * 0.3f, w * 0.8f, h * 0.72f, w * 0.9f, h * 0.36f)
    }
    glow(Color(0xFF06B6D4), p1)
    glow(Color(0xFFE879F9), p2)
}
