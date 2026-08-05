package com.ezcanvas.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ezcanvas.model.BackgroundPattern
import com.ezcanvas.model.CanvasElement
import com.ezcanvas.model.FillElement
import com.ezcanvas.model.ShapeElement
import com.ezcanvas.model.ShapeKind
import com.ezcanvas.model.StrokeElement
import com.ezcanvas.model.TextElement
import com.ezcanvas.model.Tool
import kotlin.math.abs
import kotlin.math.min

/**
 * Draws elements onto a Compose [DrawScope], which is what the user sees while drawing.
 *
 * It knows nothing about gestures or state: it takes elements and paints them. Its twin in
 * [BitmapRenderer] does the same onto an `android.graphics.Canvas` for export, and both take
 * their paint settings from [StrokeStyles] so the two cannot drift apart.
 */
internal fun DrawScope.drawElement(element: CanvasElement, smoothing: Boolean, textMeasurer: TextMeasurer) {
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

internal fun DrawScope.drawBackground(color: Color, image: ImageBitmap?, pattern: BackgroundPattern) {
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
    when (pattern) {
        BackgroundPattern.None -> Unit
        BackgroundPattern.Grid -> {
            var currentX = PatternStep
            while (currentX < size.width) {
                drawLine(PatternColor, Offset(currentX, 0f), Offset(currentX, size.height), 1f)
                currentX += PatternStep
            }
            var currentY = PatternStep
            while (currentY < size.height) {
                drawLine(PatternColor, Offset(0f, currentY), Offset(size.width, currentY), 1f)
                currentY += PatternStep
            }
        }
        BackgroundPattern.Dots -> {
            var currentY = PatternStep
            while (currentY < size.height) {
                var currentX = PatternStep
                while (currentX < size.width) {
                    drawCircle(PatternColor, radius = PatternDotRadius, center = Offset(currentX, currentY))
                    currentX += PatternStep
                }
                currentY += PatternStep
            }
        }
        BackgroundPattern.Lined -> {
            var currentY = PatternStep
            while (currentY < size.height) {
                drawLine(PatternColor, Offset(0f, currentY), Offset(size.width, currentY), 1f)
                currentY += PatternStep
            }
        }
    }
}

private fun DrawScope.drawStrokeElement(stroke: StrokeElement, smoothing: Boolean) {
    if (stroke.points.isEmpty()) return
    if (stroke.points.size == 1) {
        drawDot(stroke)
        return
    }
    val path = buildStrokePath(stroke.points.map { Offset(it.x, it.y) }, smoothing)
    val dashEffect = dashIntervals(stroke.style, stroke.widthPx)?.let { PathEffect.dashPathEffect(it, 0f) }
    when (stroke.tool) {
        Tool.Eraser -> drawPath(
            path = path,
            color = Color.Black,
            alpha = 1f,
            style = Stroke(stroke.widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
            blendMode = BlendMode.Clear,
        )

        Tool.Neon -> {
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
                stroke.style.needsRoundCap() -> StrokeCap.Round
                stroke.tool == Tool.Marker || stroke.tool == Tool.Calligraphy -> StrokeCap.Square
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
        Tool.Eraser -> drawCircle(
            color = Color.Black,
            radius = radius,
            center = center,
            blendMode = BlendMode.Clear,
        )

        Tool.Neon -> {
            drawCircle(stroke.color, radius = radius * 2.4f, center = center, alpha = 0.25f * stroke.alpha)
            drawCircle(stroke.color, radius = radius, center = center, alpha = stroke.alpha)
        }

        else -> drawCircle(stroke.color, radius = radius, center = center, alpha = stroke.alpha)
    }
}

private fun DrawScope.drawShapeElement(shape: ShapeElement) {
    val dashEffect = dashIntervals(shape.style, shape.widthPx)?.let { PathEffect.dashPathEffect(it, 0f) }
    val strokeCap = if (shape.style.needsRoundCap()) StrokeCap.Round else StrokeCap.Butt
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

/** Build a stroke path, optionally smoothing with quadratic curves through point midpoints. */
private fun buildStrokePath(points: List<Offset>, smoothing: Boolean): Path {
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
