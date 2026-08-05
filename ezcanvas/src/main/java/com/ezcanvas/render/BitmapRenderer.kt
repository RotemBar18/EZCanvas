package com.ezcanvas.render

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.DashPathEffect
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
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
import kotlin.math.max
import kotlin.math.min

/**
 * Draws elements onto an `android.graphics.Canvas`, which is what ends up in an exported PNG.
 *
 * The twin of [ScreenRenderer]. Compose cannot draw into a [Bitmap] outside a composition, so the
 * two renderers exist separately by necessity, not by copy-paste. They stay in step because every
 * paint decision they share lives in [StrokeStyles].
 */
internal fun drawElementAndroid(canvas: AndroidCanvas, element: CanvasElement, smoothing: Boolean) {
    when (element) {
        is StrokeElement -> drawStrokeAndroid(canvas, element, smoothing)
        is ShapeElement -> drawShapeAndroid(canvas, element)
        // A pending fill has no pixels yet; it is skipped until it has been replayed.
        is FillElement -> element.image?.let {
            canvas.drawBitmap(it.asAndroidBitmap(), element.topLeft.x, element.topLeft.y, null)
        }
        is TextElement -> drawTextAndroid(canvas, element)
    }
}

internal fun drawPatternAndroid(canvas: AndroidCanvas, width: Int, height: Int, pattern: BackgroundPattern) {
    if (pattern == BackgroundPattern.None) return
    val paint = AndroidPaint().apply {
        isAntiAlias = true
        color = PatternColor.toArgb()
        strokeWidth = 1f
        style = AndroidPaint.Style.FILL
    }
    when (pattern) {
        BackgroundPattern.Grid -> {
            var x = PatternStep
            while (x < width) { canvas.drawLine(x, 0f, x, height.toFloat(), paint); x += PatternStep }
            var y = PatternStep
            while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, paint); y += PatternStep }
        }
        BackgroundPattern.Dots -> {
            var y = PatternStep
            while (y < height) {
                var x = PatternStep
                while (x < width) { canvas.drawCircle(x, y, PatternDotRadius, paint); x += PatternStep }
                y += PatternStep
            }
        }
        BackgroundPattern.Lined -> {
            var y = PatternStep
            while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, paint); y += PatternStep }
        }
        BackgroundPattern.None -> Unit
    }
}

/**
 * Draw text to match the screen. Compose positions text by its top left, while Android draws from
 * the baseline, so the ascent is added back to land on the same pixels.
 */
private fun drawTextAndroid(canvas: AndroidCanvas, element: TextElement) {
    val paint = AndroidPaint().apply {
        isAntiAlias = true
        style = AndroidPaint.Style.FILL
        color = element.color.toArgb()
        alpha = (element.color.alpha * element.alpha * 255f).toInt()
        textSize = element.sizePx
    }
    canvas.drawText(element.text, element.topLeft.x, element.topLeft.y - paint.fontMetrics.ascent, paint)
}

private fun buildAndroidPath(points: List<StrokePoint>, smoothing: Boolean): AndroidPath {
    val path = AndroidPath()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (!smoothing || points.size < 3) {
        for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
        return path
    }
    for (i in 1 until points.size - 1) {
        val midX = (points[i].x + points[i + 1].x) / 2f
        val midY = (points[i].y + points[i + 1].y) / 2f
        path.quadTo(points[i].x, points[i].y, midX, midY)
    }
    path.lineTo(points.last().x, points.last().y)
    return path
}

/** Translate a [LineStyle] into an Android [DashPathEffect], or null when solid. */
private fun androidDash(style: LineStyle, width: Float): DashPathEffect? =
    dashIntervals(style, width)?.let { DashPathEffect(it, 0f) }

private fun drawStrokeAndroid(canvas: AndroidCanvas, stroke: StrokeElement, smoothing: Boolean) {
    if (stroke.points.isEmpty()) return
    if (stroke.points.size == 1) {
        drawDotAndroid(canvas, stroke)
        return
    }
    val path = buildAndroidPath(stroke.points, smoothing)
    val paint = AndroidPaint().apply {
        isAntiAlias = true
        style = AndroidPaint.Style.STROKE
        strokeJoin = AndroidPaint.Join.ROUND
    }

    when (stroke.tool) {
        Tool.Eraser -> {
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            paint.strokeCap = AndroidPaint.Cap.ROUND
            paint.strokeWidth = stroke.widthPx
            canvas.drawPath(path, paint)
        }

        Tool.Neon -> {
            paint.strokeCap = AndroidPaint.Cap.ROUND
            paint.color = stroke.color.toArgb()
            paint.strokeWidth = stroke.widthPx * 2.4f
            paint.alpha = (0.25f * stroke.alpha * 255f).toInt()
            canvas.drawPath(path, paint)
            paint.strokeWidth = stroke.widthPx
            paint.alpha = (stroke.alpha * 255f).toInt()
            canvas.drawPath(path, paint)
        }

        else -> {
            paint.strokeCap = when {
                stroke.style.needsRoundCap() -> AndroidPaint.Cap.ROUND
                stroke.tool == Tool.Marker || stroke.tool == Tool.Calligraphy -> AndroidPaint.Cap.SQUARE
                else -> AndroidPaint.Cap.ROUND
            }
            paint.pathEffect = androidDash(stroke.style, stroke.widthPx)
            paint.color = stroke.color.toArgb()
            paint.alpha = (stroke.alpha * 255f).toInt()
            paint.strokeWidth = stroke.widthPx
            canvas.drawPath(path, paint)
        }
    }
}

/** The exported twin of the on screen dot, so a tap looks the same in the PNG. */
private fun drawDotAndroid(canvas: AndroidCanvas, stroke: StrokeElement) {
    val cx = stroke.points[0].x
    val cy = stroke.points[0].y
    val radius = stroke.widthPx / 2f
    val paint = AndroidPaint().apply {
        isAntiAlias = true
        style = AndroidPaint.Style.FILL
    }
    when (stroke.tool) {
        Tool.Eraser -> {
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            canvas.drawCircle(cx, cy, radius, paint)
        }

        Tool.Neon -> {
            paint.color = stroke.color.toArgb()
            paint.alpha = (0.25f * stroke.alpha * 255f).toInt()
            canvas.drawCircle(cx, cy, radius * 2.4f, paint)
            paint.alpha = (stroke.alpha * 255f).toInt()
            canvas.drawCircle(cx, cy, radius, paint)
        }

        else -> {
            paint.color = stroke.color.toArgb()
            paint.alpha = (stroke.alpha * 255f).toInt()
            canvas.drawCircle(cx, cy, radius, paint)
        }
    }
}

private fun drawShapeAndroid(canvas: AndroidCanvas, shape: ShapeElement) {
    val paint = AndroidPaint().apply {
        isAntiAlias = true
        color = shape.color.toArgb()
        alpha = (shape.alpha * 255f).toInt()
        strokeJoin = AndroidPaint.Join.MITER
        strokeCap = if (shape.style.needsRoundCap()) AndroidPaint.Cap.ROUND else AndroidPaint.Cap.BUTT
        pathEffect = androidDash(shape.style, shape.widthPx)
        strokeWidth = shape.widthPx
    }
    val left = min(shape.start.x, shape.end.x)
    val top = min(shape.start.y, shape.end.y)
    val right = max(shape.start.x, shape.end.x)
    val bottom = max(shape.start.y, shape.end.y)

    paint.style = AndroidPaint.Style.STROKE
    when (shape.kind) {
        ShapeKind.Line -> canvas.drawLine(shape.start.x, shape.start.y, shape.end.x, shape.end.y, paint)
        ShapeKind.Square -> canvas.drawRect(left, top, right, bottom, paint)
        ShapeKind.Circle -> canvas.drawOval(RectF(left, top, right, bottom), paint)
    }
}
