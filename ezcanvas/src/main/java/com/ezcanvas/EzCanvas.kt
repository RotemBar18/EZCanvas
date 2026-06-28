package com.ezcanvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
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
import com.ezcanvas.model.Tool
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

    Canvas(
        modifier = modifier
            .onSizeChanged { newSize ->
                state.widthPx = newSize.width
                state.heightPx = newSize.height
            }
            .pointerInput(state, state.tool) {
                if (state.tool == Tool.BUCKET) {
                    detectTapGestures(
                        onTap = { offset -> state.floodFillAt(offset)?.let { state.commit(it) } },
                    )
                } else {
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
        drawBackground(state.backgroundColor, state.backgroundImage, state.backgroundPattern)

        drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
        state.elements.forEach { drawElement(it, state.smoothing) }
        buildLiveElement(state, livePoints.toList(), liveStart, liveEnd)
            ?.let { drawElement(it, state.smoothing) }
        drawContext.canvas.restore()
    }
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

private fun DrawScope.drawElement(element: CanvasElement, smoothing: Boolean) {
    when (element) {
        is StrokeElement -> drawStrokeElement(element, smoothing)
        is ShapeElement -> drawShapeElement(element)
        is FillElement -> drawImage(element.image, topLeft = element.topLeft, alpha = element.alpha)
    }
}

private fun DrawScope.drawStrokeElement(stroke: StrokeElement, smoothing: Boolean) {
    if (stroke.points.size < 2) return
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
