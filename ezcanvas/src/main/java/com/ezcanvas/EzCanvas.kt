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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import com.ezcanvas.model.CanvasElement
import com.ezcanvas.model.LineStyle
import com.ezcanvas.model.ShapeElement
import com.ezcanvas.model.ShapeKind
import com.ezcanvas.model.StrokeElement
import com.ezcanvas.model.StrokePoint
import com.ezcanvas.model.TextElement
import com.ezcanvas.model.Tool
import com.ezcanvas.ui.EzPromptDialog
import com.ezcanvas.model.isShape
import com.ezcanvas.model.shapeKind
import com.ezcanvas.render.baseAlpha
import com.ezcanvas.render.drawBackground
import com.ezcanvas.render.drawElement
import com.ezcanvas.render.textStyle
import kotlin.math.abs
import kotlin.math.max

/** Drags shorter than this (in px) don't commit a shape, which avoids invisible zero-size shapes. */
private const val MIN_SHAPE_PX = 6f

/**
 * A drawing surface driven by an [EzCanvasState].
 *
 * Renders the background (color + optional pattern), then the committed and live elements in an
 * isolated layer so the [Tool.Eraser] clears strokes without erasing the background. Freehand
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
        if (state.tool != Tool.Text) state.clearSelection()
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { newSize ->
                // A canvas with no width or height is a layout pass, not a size to remember. The
                // last real size is what a later pass has to remap from.
                if (newSize.width <= 0 || newSize.height <= 0) return@onSizeChanged
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
                            state.tool == Tool.Bucket -> state.floodFillAt(offset)?.let { state.commit(it) }

                            state.tool == Tool.Text -> {
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
                if (state.tool == Tool.Text) {
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
                } else if (state.tool != Tool.Bucket) {
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
private fun EzCanvasState.textIndexAt(point: Offset, textMeasurer: TextMeasurer, density: Density): Int? {
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
    val isEraser = state.tool == Tool.Eraser
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
    val kind = state.tool.shapeKind
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
    if (state.tool == Tool.Bucket) return null

    if (points.size < 2) return null
    val isEraser = state.tool == Tool.Eraser
    return StrokeElement(
        points = points.map { StrokePoint(it.x, it.y) },
        tool = state.tool,
        color = state.strokeColor,
        widthPx = if (isEraser) state.eraserWidthPx else state.strokeWidthPx,
        alpha = baseAlpha(state.tool) * state.strokeAlpha,
        // Only the pen honours the dash style; textured brushes stay solid.
        style = if (state.tool == Tool.Pen) state.lineStyle else LineStyle.Solid,
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
