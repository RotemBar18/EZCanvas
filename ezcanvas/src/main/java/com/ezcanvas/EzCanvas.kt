package com.ezcanvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ezcanvas.model.BackgroundPattern
import com.ezcanvas.model.CanvasElement
import com.ezcanvas.model.FillElement
import com.ezcanvas.model.LineStyle
import com.ezcanvas.model.ShapeElement
import com.ezcanvas.model.ShapeKind
import com.ezcanvas.model.StrokeElement
import com.ezcanvas.model.StrokePoint
import com.ezcanvas.model.TextElement
import com.ezcanvas.model.Tool
import com.ezcanvas.model.isShape
import com.ezcanvas.model.shapeKind
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Drags shorter than this (in px) don't commit a shape — avoids invisible zero-size shapes. */
private const val MIN_SHAPE_PX = 6f

/**
 * A drawing surface driven by an [EzCanvasState].
 *
 * Renders the background (color + optional pattern), then the committed and live elements in an
 * isolated layer so the [Tool.ERASER] clears strokes without erasing the background. Freehand
 * tools accumulate points; the shape tools rubber-band a [ShapeElement] between the drag's start
 * and end. When [EzCanvasState.smoothing] is on, strokes are drawn as quadratic curves.
 *
 * @param state hoisted state; create with [rememberEzCanvasState].
 * @param modifier layout modifier (size, etc.).
 */
@Composable
fun EzCanvas(state: EzCanvasState, modifier: Modifier = Modifier) {
    val livePoints = remember { mutableStateListOf<Offset>() }
    var liveStart by remember { mutableStateOf<Offset?>(null) }
    var liveEnd by remember { mutableStateOf<Offset?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var pendingTextAt by remember { mutableStateOf<Offset?>(null) }

    // Editing colour, opacity or size while text is selected edits that text; the state applies
    // it in the setters, so no effect is needed here.
    // A selection only makes sense while the text tool is active.
    LaunchedEffect(state.tool) {
        if (state.tool != Tool.TEXT) state.clearSelection()
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { newSize ->
                // Keep the drawing in the same relative place when the canvas changes shape.
                state.remapTo(newSize.width, newSize.height)
                state.widthPx = newSize.width
                state.heightPx = newSize.height
                // Fills persist as a seed and a colour, so redraw them now the canvas has a size.
                state.replayPendingFills()
            }
            // Taps and drags are detected separately so a single touch still marks the canvas.
            .pointerInput(state, state.tool) {
                detectTapGestures(
                    onTap = { offset ->
                        when {
                            state.tool == Tool.BUCKET -> state.floodFillAt(offset)?.let { state.commit(it) }

                            state.tool == Tool.TEXT -> {
                                val hit = state.textIndexAt(offset, textMeasurer, density)
                                // Tapping text selects it; tapping empty canvas starts a new one.
                                if (hit != null) state.selectTextAt(hit) else pendingTextAt = offset
                            }

                            // Shapes need a drag to have any size, so a tap does nothing for them.
                            state.tool.isShape -> Unit

                            else -> state.commit(dotAt(state, offset))
                        }
                    },
                )
            }
            .pointerInput(state, state.tool) {
                if (state.tool == Tool.TEXT) {
                    // Dragging text moves it, and grabbing a piece selects it first.
                    detectDragGestures(
                        onDragStart = { offset ->
                            state.textIndexAt(offset, textMeasurer, density)?.let { state.selectTextAt(it) }
                        },
                        onDrag = { pointerChange, dragAmount ->
                            if (state.hasSelection) {
                                state.moveSelectionBy(dragAmount)
                                pointerChange.consume()
                            }
                        },
                    )
                } else if (state.tool != Tool.BUCKET) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            livePoints.clear()
                            livePoints.add(offset)
                            liveStart = offset
                            liveEnd = offset
                        },
                        onDrag = { pointerChange, _ ->
                            livePoints.add(pointerChange.position)
                            liveEnd = pointerChange.position
                            pointerChange.consume()
                        },
                        onDragEnd = {
                            buildLiveElement(state, livePoints.toList(), liveStart, liveEnd)
                                ?.let { state.commit(it) }
                            livePoints.clear()
                            liveStart = null
                            liveEnd = null
                        },
                    )
                }
            }
    ) {
        // Reading the revision subscribes this draw to every element change, including edits made
        // in place such as recolouring the selected text.
        @Suppress("UNUSED_EXPRESSION")
        state.revision

        drawBackground(state.backgroundColor, state.backgroundImage, state.backgroundPattern)

        drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
        state.elements.forEach { drawElement(it, state.smoothing, textMeasurer) }
        buildLiveElement(state, livePoints.toList(), liveStart, liveEnd)
            ?.let { drawElement(it, state.smoothing, textMeasurer) }
        drawContext.canvas.restore()

        // The selection outline is a screen only affordance, so it never reaches an export.
        state.selectedIndex?.let { index ->
            (state.elements.getOrNull(index) as? TextElement)?.let { selected ->
                val measured = textMeasurer.measure(selected.text, selected.textStyle(this))
                drawRect(
                    color = Color(0xFF4F46E5),
                    topLeft = selected.topLeft - Offset(6f, 6f),
                    size = Size(measured.size.width + 12f, measured.size.height + 12f),
                    style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                )
            }
        }
    }

    pendingTextAt?.let { at ->
        EzPromptDialog(
            title = "Add text",
            placeholder = "Type something",
            confirmLabel = "Add",
            helper = { "Placed where you tapped." },
            onConfirm = { typed ->
                state.clearSelection()
                state.commit(
                    TextElement(
                        text = typed,
                        topLeft = at,
                        sizePx = state.strokeWidthPx * TextSizeFactor,
                        color = state.strokeColor,
                        alpha = state.strokeAlpha,
                    ),
                )
            },
            onDismiss = { pendingTextAt = null },
        )
    }
}

/**
 * Index of the topmost [TextElement] whose box contains [point], or null. Later elements are
 * checked first so the one drawn on top wins.
 */
internal fun EzCanvasState.textIndexAt(point: Offset, textMeasurer: TextMeasurer, density: Density): Int? {
    for (index in elements.indices.reversed()) {
        val element = elements[index] as? TextElement ?: continue
        val measured = textMeasurer.measure(element.text, element.textStyle(density))
        val within = point.x >= element.topLeft.x - TextHitPadding &&
            point.x <= element.topLeft.x + measured.size.width + TextHitPadding &&
            point.y >= element.topLeft.y - TextHitPadding &&
            point.y <= element.topLeft.y + measured.size.height + TextHitPadding
        if (within) return index
    }
    return null
}

/** A little slack so small text is still easy to grab with a finger. */
private const val TextHitPadding = 12f

/** A single tap: a one point stroke, rendered as a dot the width of the current brush. */
private fun dotAt(state: EzCanvasState, at: Offset): StrokeElement {
    val isEraser = state.tool == Tool.ERASER
    return StrokeElement(
        points = listOf(StrokePoint(at.x, at.y)),
        tool = state.tool,
        color = state.strokeColor,
        widthPx = if (isEraser) state.eraserWidthPx else state.strokeWidthPx,
        alpha = baseAlpha(state.tool) * state.strokeAlpha,
        // A dot has no length, so a dash pattern would make it vanish.
        style = LineStyle.Solid,
    )
}

/**
 * The element currently being drawn, or null when there isn't a committable one yet
 * (too few points, or a sub-threshold shape). Used for both the live preview and the commit.
 */
private fun buildLiveElement(
    state: EzCanvasState,
    points: List<Offset>,
    start: Offset?,
    end: Offset?,
): CanvasElement? {
    val kind = state.tool.shapeKind()
    if (kind != null) {
        if (start == null || end == null) return null
        val resolvedEnd = constrainShapeEnd(kind, start, end)
        if ((resolvedEnd - start).getDistance() < MIN_SHAPE_PX) return null
        return ShapeElement(
            kind = kind,
            start = start,
            end = resolvedEnd,
            color = state.strokeColor,
            widthPx = state.strokeWidthPx,
            alpha = state.strokeAlpha,
            style = state.lineStyle,
        )
    }

    // The bucket fills on tap (handled in the pointer input), never by dragging.
    if (state.tool == Tool.BUCKET) return null

    if (points.size < 2) return null
    val isEraser = state.tool == Tool.ERASER
    return StrokeElement(
        points = points.map { StrokePoint(it.x, it.y) },
        tool = state.tool,
        color = state.strokeColor,
        widthPx = if (isEraser) state.eraserWidthPx else state.strokeWidthPx,
        alpha = baseAlpha(state.tool) * state.strokeAlpha,
        // Only the pen honours the dash style; textured brushes stay solid.
        style = if (state.tool == Tool.PEN) state.lineStyle else LineStyle.Solid,
    )
}

/** Anchor a square/circle at [start] and grow it 1:1 toward the drag direction. Lines pass through. */
private fun constrainShapeEnd(kind: ShapeKind, start: Offset, end: Offset): Offset {
    if (kind == ShapeKind.Line) return end
    val deltaX = end.x - start.x
    val deltaY = end.y - start.y
    val maxSideLength = max(abs(deltaX), abs(deltaY))
    val directionX = if (deltaX < 0) -1f else 1f
    val directionY = if (deltaY < 0) -1f else 1f
    return Offset(start.x + maxSideLength * directionX, start.y + maxSideLength * directionY)
}

internal fun baseAlpha(tool: Tool): Float = if (tool == Tool.MARKER) 0.45f else 1f

/** Dash on/off intervals for a [LineStyle], scaled to the stroke width, or null when solid. */
internal fun dashIntervals(style: LineStyle, width: Float): FloatArray? {
    val strokeWidth = width.coerceAtLeast(1f)
    return when (style) {
        LineStyle.Solid -> null
        LineStyle.Dotted -> floatArrayOf(0.01f, strokeWidth * 2f)        // ~zero "on" + round cap = dots
        LineStyle.Dashed -> floatArrayOf(strokeWidth * 3f, strokeWidth * 2f)
        LineStyle.DashDot -> floatArrayOf(strokeWidth * 3f, strokeWidth * 2f, 0.01f, strokeWidth * 2f)
    }
}

/** Round caps make dotted / dash-dot render as actual dots; everything else uses butt/round as set. */
private fun dottedNeedsRoundCap(style: LineStyle): Boolean =
    style == LineStyle.Dotted || style == LineStyle.DashDot

/** Build a stroke path, optionally smoothing with quadratic curves through point midpoints. */
internal fun buildStrokePath(points: List<Offset>, smoothing: Boolean): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (!smoothing || points.size < 3) {
        for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
        return path
    }
    for (i in 1 until points.size - 1) {
        val midX = (points[i].x + points[i + 1].x) / 2f
        val midY = (points[i].y + points[i + 1].y) / 2f
        path.quadraticTo(points[i].x, points[i].y, midX, midY)
    }
    path.lineTo(points.last().x, points.last().y)
    return path
}

private fun DrawScope.drawElement(element: CanvasElement, smoothing: Boolean, textMeasurer: TextMeasurer) {
    when (element) {
        is StrokeElement -> drawStrokeElement(element, smoothing)
        is ShapeElement -> drawShapeElement(element)
        // A pending fill has no pixels yet; it is skipped until it has been replayed.
        is FillElement -> element.image?.let { drawImage(it, topLeft = element.topLeft, alpha = element.alpha) }
        // Colour and alpha are passed at draw time, not baked into the measured style. The
        // measurer caches layouts, and colour does not affect layout, so a recoloured text would
        // otherwise keep drawing with the cached paint.
        is TextElement -> drawText(
            textLayoutResult = textMeasurer.measure(element.text, element.textStyle(this)),
            color = element.color,
            topLeft = element.topLeft,
            alpha = element.alpha,
        )
    }
}

/**
 * Layout-affecting text style only, so measuring, drawing and export cannot disagree. Colour and
 * opacity are deliberately excluded: they are applied when drawing. The size is converted through
 * [density] so it lands on exactly [TextElement.sizePx] pixels, which is what the exporter uses.
 */
internal fun TextElement.textStyle(density: Density): TextStyle = TextStyle(
    fontSize = with(density) { sizePx.toSp() },
)

private fun DrawScope.drawStrokeElement(stroke: StrokeElement, smoothing: Boolean) {
    if (stroke.points.isEmpty()) return
    if (stroke.points.size == 1) {
        drawDot(stroke)
        return
    }
    val path = buildStrokePath(stroke.points.map { Offset(it.x, it.y) }, smoothing)
    val dashEffect = dashIntervals(stroke.style, stroke.widthPx)?.let { PathEffect.dashPathEffect(it, 0f) }
    when (stroke.tool) {
        Tool.ERASER -> drawPath(
            path = path,
            color = Color.Black,
            alpha = 1f,
            style = Stroke(stroke.widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
            blendMode = BlendMode.Clear,
        )

        Tool.NEON -> {
            drawPath(
                path = path,
                color = stroke.color,
                alpha = 0.25f * stroke.alpha,
                style = Stroke(stroke.widthPx * 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
                path = path,
                color = stroke.color,
                alpha = stroke.alpha,
                style = Stroke(stroke.widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        else -> {
            val strokeCap = when {
                dottedNeedsRoundCap(stroke.style) -> StrokeCap.Round
                stroke.tool == Tool.MARKER || stroke.tool == Tool.CALLIGRAPHY -> StrokeCap.Square
                else -> StrokeCap.Round
            }
            drawPath(
                path = path,
                color = stroke.color,
                alpha = stroke.alpha,
                style = Stroke(stroke.widthPx, cap = strokeCap, join = StrokeJoin.Round, pathEffect = dashEffect),
            )
        }
    }
}

/** A tap: a filled circle. The eraser clears one instead, and neon keeps its halo. */
private fun DrawScope.drawDot(stroke: StrokeElement) {
    val center = Offset(stroke.points[0].x, stroke.points[0].y)
    val radius = stroke.widthPx / 2f
    when (stroke.tool) {
        Tool.ERASER -> drawCircle(
            color = Color.Black,
            radius = radius,
            center = center,
            blendMode = BlendMode.Clear,
        )

        Tool.NEON -> {
            drawCircle(stroke.color, radius = radius * 2.4f, center = center, alpha = 0.25f * stroke.alpha)
            drawCircle(stroke.color, radius = radius, center = center, alpha = stroke.alpha)
        }

        else -> drawCircle(stroke.color, radius = radius, center = center, alpha = stroke.alpha)
    }
}

private fun DrawScope.drawShapeElement(shape: ShapeElement) {
    val dashEffect = dashIntervals(shape.style, shape.widthPx)?.let { PathEffect.dashPathEffect(it, 0f) }
    val strokeCap = if (dottedNeedsRoundCap(shape.style)) StrokeCap.Round else StrokeCap.Butt
    val topLeft = Offset(min(shape.start.x, shape.end.x), min(shape.start.y, shape.end.y))
    val boxSize = Size(abs(shape.end.x - shape.start.x), abs(shape.end.y - shape.start.y))
    when (shape.kind) {
        ShapeKind.Line -> drawLine(
            color = shape.color,
            start = shape.start,
            end = shape.end,
            strokeWidth = shape.widthPx,
            cap = strokeCap,
            pathEffect = dashEffect,
            alpha = shape.alpha,
        )

        ShapeKind.Square -> drawRect(
            color = shape.color,
            topLeft = topLeft,
            size = boxSize,
            alpha = shape.alpha,
            style = Stroke(shape.widthPx, cap = strokeCap, join = StrokeJoin.Miter, pathEffect = dashEffect),
        )

        ShapeKind.Circle -> drawOval(
            color = shape.color,
            topLeft = topLeft,
            size = boxSize,
            alpha = shape.alpha,
            style = Stroke(shape.widthPx, cap = strokeCap, pathEffect = dashEffect),
        )
    }
}

private fun DrawScope.drawBackground(color: Color, image: ImageBitmap?, pattern: BackgroundPattern) {
    if (image != null) {
        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, image.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1)),
        )
    } else {
        drawRect(color = color)
    }
    val gridColor = Color.Gray.copy(alpha = 0.25f)
    val step = 48f
    when (pattern) {
        BackgroundPattern.None -> Unit
        BackgroundPattern.Grid -> {
            var currentX = step
            while (currentX < size.width) {
                drawLine(gridColor, Offset(currentX, 0f), Offset(currentX, size.height), 1f)
                currentX += step
            }
            var currentY = step
            while (currentY < size.height) {
                drawLine(gridColor, Offset(0f, currentY), Offset(size.width, currentY), 1f)
                currentY += step
            }
        }
        BackgroundPattern.Dots -> {
            var currentY = step
            while (currentY < size.height) {
                var currentX = step
                while (currentX < size.width) {
                    drawCircle(gridColor, radius = 2.5f, center = Offset(currentX, currentY))
                    currentX += step
                }
                currentY += step
            }
        }
        BackgroundPattern.Lined -> {
            var currentY = step
            while (currentY < size.height) {
                drawLine(gridColor, Offset(0f, currentY), Offset(size.width, currentY), 1f)
                currentY += step
            }
        }
    }
}
