package com.ezcanvas

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.RectF
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import com.ezcanvas.render.drawElementAndroid
import com.ezcanvas.render.drawPatternAndroid

/**
 * Render the current drawing into a new [Bitmap], at the size the canvas was last laid out.
 * Returns null if the canvas has not been measured yet.
 *
 * Rendering mirrors what [EzCanvas] shows on screen, including the eraser, shapes and dash styles.
 *
 * Set [transparentBackground] to leave out the background colour, image and pattern, so only what
 * was drawn is exported. A signature then drops onto a document without a white box behind it.
 */
fun EzCanvasState.exportBitmap(transparentBackground: Boolean = false): Bitmap? {
    val w = widthPx
    val h = heightPx
    if (w <= 0 || h <= 0) return null

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    if (!transparentBackground) {
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
    }

    val layer = canvas.saveLayer(0f, 0f, w.toFloat(), h.toFloat(), null)
    for (element in elements) drawElementAndroid(canvas, element, smoothing)
    canvas.restoreToCount(layer)

    return bitmap
}

/**
 * Render only the elements onto a transparent bitmap, used as the flood-fill source, so empty
 * areas stay transparent and drawn pixels act as fill boundaries.
 *
 * @param elementCount how many elements from the bottom to include; all of them by default.
 */
internal fun EzCanvasState.renderElementsBitmap(
    width: Int,
    height: Int,
    elementCount: Int = elements.size,
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
    for (index in 0 until elementCount.coerceAtMost(elements.size)) {
        drawElementAndroid(canvas, elements[index], smoothing)
    }
    canvas.restoreToCount(layer)
    return bitmap
}
