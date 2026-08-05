package com.ezcanvas.render

import androidx.compose.ui.graphics.Color
import com.ezcanvas.model.LineStyle
import com.ezcanvas.model.Tool

/**
 * The rules that turn a tool or a line style into paint settings.
 *
 * Both renderers read them, which is the point: the screen and the exported PNG can only look the
 * same if they answer these questions from one place.
 */

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
internal fun LineStyle.needsRoundCap(): Boolean = this == LineStyle.Dotted || this == LineStyle.DashDot

/** The marker is translucent so overlapping strokes build up, like a real highlighter. */
internal fun baseAlpha(tool: Tool): Float = if (tool == Tool.Marker) 0.45f else 1f

// --- Background pattern geometry ------------------------------------------
// Shared for the same reason: a grid that is 48px on screen and 50px in the export would be a
// visible bug in every exported PNG.

/** Gap between grid lines, dots and rules, in canvas pixels. */
internal const val PatternStep = 48f

/** Radius of a single dot in [com.ezcanvas.model.BackgroundPattern.Dots]. */
internal const val PatternDotRadius = 2.5f

/** Faint enough to guide a drawing without competing with it. */
internal val PatternColor = Color.Gray.copy(alpha = 0.25f)
