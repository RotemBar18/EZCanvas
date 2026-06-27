package com.ezcanvas

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.ezcanvas.model.FillElement
import kotlin.math.abs

/** Max summed per-channel difference (0..1020) for two pixels to count as the same region. */
private const val FILL_TOLERANCE = 60

/**
 * Flood-fill the region under [seed] with the current [EzCanvasState.strokeColor], producing a
 * [FillElement] (a cropped bitmap of just the filled pixels) ready to commit — or null if the
 * canvas isn't laid out, the seed already matches the fill color, or nothing was filled.
 *
 * Runs synchronously on the caller's thread; for very large canvases this is a brief hitch.
 */
internal fun EzCanvasState.floodFillAt(seed: Offset): FillElement? {
    val w = widthPx
    val h = heightPx
    if (w <= 0 || h <= 0) return null
    val sx = seed.x.toInt().coerceIn(0, w - 1)
    val sy = seed.y.toInt().coerceIn(0, h - 1)

    val src = renderElementsBitmap(w, h)
    val pixels = IntArray(w * h)
    src.getPixels(pixels, 0, w, 0, 0, w, h)
    src.recycle()

    val fillColor = strokeColor.copy(alpha = strokeAlpha).toArgb()
    val target = pixels[sy * w + sx]
    if (colorDistance(target, fillColor) <= FILL_TOLERANCE) return null

    val region = scanlineFlood(pixels, w, h, sx, sy, target, FILL_TOLERANCE) ?: return null

    val bw = region.maxX - region.minX + 1
    val bh = region.maxY - region.minY + 1
    val out = IntArray(bw * bh)
    for (y in region.minY..region.maxY) {
        val rowBase = y * w
        val outBase = (y - region.minY) * bw
        for (x in region.minX..region.maxX) {
            if (region.mask[rowBase + x]) out[outBase + (x - region.minX)] = fillColor
        }
    }
    val outBmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
    outBmp.setPixels(out, 0, bw, 0, 0, bw, bh)

    return FillElement(
        seed = seed,
        color = strokeColor.copy(alpha = strokeAlpha),
        image = outBmp.asImageBitmap(),
        topLeft = Offset(region.minX.toFloat(), region.minY.toFloat()),
    )
}

/** The pixels a flood fill covered, plus the bounding box, in source-bitmap coordinates. */
internal class FloodRegion(
    val mask: BooleanArray,
    val minX: Int,
    val minY: Int,
    val maxX: Int,
    val maxY: Int,
)

/** Summed absolute per-channel (A,R,G,B) difference of two ARGB ints — cheap color distance. */
internal fun colorDistance(a: Int, b: Int): Int {
    val da = abs(((a ushr 24) and 0xFF) - ((b ushr 24) and 0xFF))
    val dr = abs(((a ushr 16) and 0xFF) - ((b ushr 16) and 0xFF))
    val dg = abs(((a ushr 8) and 0xFF) - ((b ushr 8) and 0xFF))
    val db = abs((a and 0xFF) - (b and 0xFF))
    return da + dr + dg + db
}

/**
 * Scanline flood fill over a raw ARGB pixel array. Pure (no Android types) so it is unit-testable.
 * Fills 4-connected pixels within [tolerance] of the seed's color; returns the covered region,
 * or null if the seed itself doesn't match.
 */
internal fun scanlineFlood(
    pixels: IntArray,
    w: Int,
    h: Int,
    sx: Int,
    sy: Int,
    target: Int,
    tolerance: Int,
): FloodRegion? {
    val mask = BooleanArray(w * h)
    var stack = IntArray(1024)
    var sp = 0
    fun push(i: Int) {
        if (sp == stack.size) stack = stack.copyOf(stack.size * 2)
        stack[sp++] = i
    }
    fun match(i: Int) = !mask[i] && colorDistance(pixels[i], target) <= tolerance

    if (!match(sy * w + sx)) return null
    push(sy * w + sx)
    var minX = sx
    var maxX = sx
    var minY = sy
    var maxY = sy

    while (sp > 0) {
        val idx = stack[--sp]
        if (mask[idx]) continue
        val y = idx / w
        val seedX = idx % w

        var lx = seedX
        while (lx - 1 >= 0 && match(y * w + lx - 1)) lx--
        var rx = seedX
        while (rx + 1 < w && match(y * w + rx + 1)) rx++

        val rowBase = y * w
        for (x in lx..rx) mask[rowBase + x] = true
        if (lx < minX) minX = lx
        if (rx > maxX) maxX = rx
        if (y < minY) minY = y
        if (y > maxY) maxY = y

        for (ny in intArrayOf(y - 1, y + 1)) {
            if (ny < 0 || ny >= h) continue
            val nBase = ny * w
            var x = lx
            while (x <= rx) {
                if (match(nBase + x)) {
                    push(nBase + x)
                    while (x <= rx && match(nBase + x)) x++ // skip the rest of this run
                } else {
                    x++
                }
            }
        }
    }
    return FloodRegion(mask, minX, minY, maxX, maxY)
}
