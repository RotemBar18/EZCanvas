package com.ezcanvas

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.DashPathEffect
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
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
import com.ezcanvas.model.Tool
import kotlin.math.max
import kotlin.math.min

/**
 * Render the current drawing (background + every element) into a new [Bitmap], at the size the
 * canvas was last laid out. Returns null if the canvas has not been measured yet.
 *
 * Rendering mirrors what [EzCanvas] shows on screen, including the eraser, shapes and dash styles.
 */
fun EzCanvasState.exportBitmap(): Bitmap? {
    val w = widthPx
    val h = heightPx
    if (w <= 0 || h <= 0) return null

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    canvas.drawColor(backgroundColor.toArgb())
    backgroundImage?.let { img ->
        canvas.drawBitmap(
            img.asAndroidBitmap(),
            null,
            RectF(0f, 0f, w.toFloat(), h.toFloat()),
            null,
        )
    }
    drawPatternAndroid(canvas, w, h, backgroundPattern)

    val layer = canvas.saveLayer(0f, 0f, w.toFloat(), h.toFloat(), null)
    for (element in elements) drawElementAndroid(canvas, element, smoothing)
    canvas.restoreToCount(layer)

    return bitmap
}

/**
 * Render only the elements onto a transparent bitmap at [w]×[h] — used as the flood-fill source,
 * so empty areas stay transparent and drawn pixels act as fill boundaries.
 */
internal fun EzCanvasState.renderElementsBitmap(w: Int, h: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val layer = canvas.saveLayer(0f, 0f, w.toFloat(), h.toFloat(), null)
    for (element in elements) drawElementAndroid(canvas, element, smoothing)
    canvas.restoreToCount(layer)
    return bitmap
}

private fun drawElementAndroid(canvas: AndroidCanvas, element: CanvasElement, smoothing: Boolean) {
    when (element) {
        is StrokeElement -> drawStrokeAndroid(canvas, element, smoothing)
        is ShapeElement -> drawShapeAndroid(canvas, element)
        is FillElement -> canvas.drawBitmap(element.image.asAndroidBitmap(), element.topLeft.x, element.topLeft.y, null)
    }
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

private fun roundCapStyle(style: LineStyle): Boolean =
    style == LineStyle.Dotted || style == LineStyle.DashDot

private fun drawStrokeAndroid(canvas: AndroidCanvas, stroke: StrokeElement, smoothing: Boolean) {
    if (stroke.points.size < 2) return
    val path = buildAndroidPath(stroke.points, smoothing)
    val paint = AndroidPaint().apply {
        isAntiAlias = true
        style = AndroidPaint.Style.STROKE
        strokeJoin = AndroidPaint.Join.ROUND
    }

    when (stroke.tool) {
        Tool.ERASER -> {
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            paint.strokeCap = AndroidPaint.Cap.ROUND
            paint.strokeWidth = stroke.widthPx
            canvas.drawPath(path, paint)
        }

        Tool.NEON -> {
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
                roundCapStyle(stroke.style) -> AndroidPaint.Cap.ROUND
                stroke.tool == Tool.MARKER || stroke.tool == Tool.CALLIGRAPHY -> AndroidPaint.Cap.SQUARE
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

private fun drawShapeAndroid(canvas: AndroidCanvas, shape: ShapeElement) {
    val paint = AndroidPaint().apply {
        isAntiAlias = true
        color = shape.color.toArgb()
        alpha = (shape.alpha * 255f).toInt()
        strokeJoin = AndroidPaint.Join.MITER
        strokeCap = if (roundCapStyle(shape.style)) AndroidPaint.Cap.ROUND else AndroidPaint.Cap.BUTT
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

private fun drawPatternAndroid(canvas: AndroidCanvas, w: Int, h: Int, pattern: BackgroundPattern) {
    if (pattern == BackgroundPattern.None) return
    val paint = AndroidPaint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(64, 128, 128, 128)
        strokeWidth = 1f
        style = AndroidPaint.Style.FILL
    }
    val step = 48f
    when (pattern) {
        BackgroundPattern.Grid -> {
            var x = step
            while (x < w) { canvas.drawLine(x, 0f, x, h.toFloat(), paint); x += step }
            var y = step
            while (y < h) { canvas.drawLine(0f, y, w.toFloat(), y, paint); y += step }
        }
        BackgroundPattern.Dots -> {
            var y = step
            while (y < h) {
                var x = step
                while (x < w) { canvas.drawCircle(x, y, 2.5f, paint); x += step }
                y += step
            }
        }
        BackgroundPattern.Lined -> {
            var y = step
            while (y < h) { canvas.drawLine(0f, y, w.toFloat(), y, paint); y += step }
        }
        BackgroundPattern.None -> Unit
    }
}
