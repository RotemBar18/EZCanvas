package com.ezcanvas.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

/**
 * The drawing tool currently selected on an [com.ezcanvas.EzCanvasState].
 *
 * Freehand brushes & eraser produce a [StrokeElement]; the shape tools
 * ([LINE], [SQUARE], [CIRCLE]) produce a [ShapeElement].
 *
 * - [PEN] opaque round stroke (honours the selected [LineStyle])
 * - [MARKER] translucent flat stroke (always solid)
 * - [NEON] glowing stroke (always solid)
 * - [CALLIGRAPHY] thicker flat stroke (always solid)
 * - [ERASER] removes previously drawn elements
 * - [LINE] straight line between two points
 * - [SQUARE] perfect 1:1 square (drag to size)
 * - [CIRCLE] perfect 1:1 circle (drag to size)
 * - [BUCKET] flood-fills the region under the tap with the selected color (paint bucket)
 */
enum class Tool { PEN, MARKER, NEON, CALLIGRAPHY, ERASER, LINE, SQUARE, CIRCLE, BUCKET }

/** Background grid drawn behind the strokes. */
enum class BackgroundPattern { None, Grid, Dots, Lined }

/** How an element's outline is dashed. Applies to the pen and to every shape outline. */
enum class LineStyle { Solid, Dotted, Dashed, DashDot }

/** The geometric shapes the shape tools can draw. Square & circle are constrained 1:1. */
enum class ShapeKind { Line, Square, Circle }

/** The shape a shape-tool draws, or `null` for the freehand/eraser tools. */
fun Tool.shapeKind(): ShapeKind? = when (this) {
    Tool.LINE -> ShapeKind.Line
    Tool.SQUARE -> ShapeKind.Square
    Tool.CIRCLE -> ShapeKind.Circle
    else -> null
}

/** True when this tool draws a [ShapeElement] rather than a freehand [StrokeElement]. */
val Tool.isShape: Boolean get() = shapeKind() != null

/**
 * A single point in a freehand stroke. [widthScale] (0f..1f, 1f = full width) lets a stroke
 * taper — reserved for pressure/velocity brushes; current rendering uses a uniform width.
 */
data class StrokePoint(val x: Float, val y: Float, val widthScale: Float = 1f)

/**
 * One drawn thing on the canvas: either a freehand [StrokeElement] or a [ShapeElement].
 *
 * A single ordered list of these powers undo/redo, save/restore and export — adding a new
 * element type never touches that machinery.
 */
sealed interface CanvasElement {
    val color: Color
    val widthPx: Float
    val alpha: Float
    val style: LineStyle
}

/**
 * A freehand stroke: an ordered list of points plus its paint settings.
 *
 * @property points ordered touch points, in canvas pixels.
 * @property tool the brush/eraser that produced it (controls how it is rendered).
 */
data class StrokeElement(
    val points: List<StrokePoint>,
    val tool: Tool,
    override val color: Color,
    override val widthPx: Float,
    override val alpha: Float = 1f,
    override val style: LineStyle = LineStyle.Solid,
) : CanvasElement

/**
 * A shape defined by two points: where the drag started ([start]) and ended ([end]).
 * [ShapeKind.Square] and [ShapeKind.Circle] are constrained to 1:1 before commit, so for them
 * `end` already encodes an equal-sided box. Shapes are outlines; use [Tool.BUCKET] to fill them.
 */
data class ShapeElement(
    val kind: ShapeKind,
    val start: Offset,
    val end: Offset,
    override val color: Color,
    override val widthPx: Float,
    override val alpha: Float = 1f,
    override val style: LineStyle = LineStyle.Solid,
) : CanvasElement

/**
 * A paint-bucket fill: the result of flooding an enclosed region with a color. Because flood fill
 * is a pixel operation, the filled region is baked into [image] (a cropped bitmap positioned at
 * [topLeft]) rather than stored as vector data. [seed] and [color] record what produced it.
 *
 * Not persisted across rotation — like a background image, a fill is re-created by tapping again.
 */
class FillElement(
    val seed: Offset,
    override val color: Color,
    val image: ImageBitmap,
    val topLeft: Offset,
    override val alpha: Float = 1f,
) : CanvasElement {
    override val widthPx: Float get() = 0f
    override val style: LineStyle get() = LineStyle.Solid
}
