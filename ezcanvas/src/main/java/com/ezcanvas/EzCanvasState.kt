package com.ezcanvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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

/**
 * Hoisted state holder for an [EzCanvas] (and an [EzToolbar] bound to the same canvas).
 *
 * Create it with [rememberEzCanvasState]; the same instance drives both the canvas and the
 * toolbar. All settings are observable, so changing them recomposes any UI that reads them.
 * The full state (settings + elements) survives configuration changes via [Saver].
 */
@Stable
class EzCanvasState {

    /** Committed elements (strokes & shapes), drawn bottom-to-top. Internal: mutated by [EzCanvas]. */
    internal val elements = mutableStateListOf<CanvasElement>()
    private val redoStack = mutableStateListOf<CanvasElement>()

    /** Last laid-out canvas size in pixels (reported by [EzCanvas]; used by export). */
    internal var widthPx: Int = 0
    internal var heightPx: Int = 0

    // --- Drawing settings -------------------------------------------------

    var tool by mutableStateOf(Tool.PEN)
    var strokeColor by mutableStateOf(Color.Black)
    var strokeWidthPx by mutableFloatStateOf(10f)
    var strokeAlpha by mutableFloatStateOf(1f)
    var eraserWidthPx by mutableFloatStateOf(40f)

    /** Outline dash style for the pen and every shape. Brushes other than pen are always solid. */
    var lineStyle by mutableStateOf(LineStyle.Solid)

    /** Smooth strokes with quadratic curves (vs. straight segments). */
    var smoothing by mutableStateOf(true)

    // --- Canvas / background ---------------------------------------------

    var backgroundColor by mutableStateOf(Color.White)
    var backgroundPattern by mutableStateOf(BackgroundPattern.None)

    /** Optional image drawn as the background (scaled to fill). Not persisted across rotation. */
    var backgroundImage by mutableStateOf<ImageBitmap?>(null)

    // --- History ----------------------------------------------------------

    var maxHistorySize: Int = 200

    val isEmpty: Boolean get() = elements.isEmpty()
    val canUndo: Boolean get() = elements.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    internal fun commit(element: CanvasElement) {
        elements.add(element)
        if (elements.size > maxHistorySize) elements.removeAt(0)
        redoStack.clear()
    }

    fun undo() {
        if (elements.isNotEmpty()) redoStack.add(elements.removeAt(elements.lastIndex))
    }

    fun redo() {
        if (redoStack.isNotEmpty()) elements.add(redoStack.removeAt(redoStack.lastIndex))
    }

    fun clear() {
        elements.clear()
        redoStack.clear()
    }

    companion object {
        /** Saves settings + every element so the drawing survives rotation / process recreation. */
        val Saver = mapSaver(
            save = { state ->
                mapOf(
                    "v" to SAVE_VERSION,
                    "tool" to state.tool.ordinal,
                    "color" to state.strokeColor.toArgb(),
                    "width" to state.strokeWidthPx,
                    "alpha" to state.strokeAlpha,
                    "eraser" to state.eraserWidthPx,
                    "style" to state.lineStyle.ordinal,
                    "smoothing" to state.smoothing,
                    "bg" to state.backgroundColor.toArgb(),
                    "pattern" to state.backgroundPattern.ordinal,
                    // Fills are baked bitmaps; skip them (re-created by tapping again after rotation).
                    "elements" to ArrayList(state.elements.filter { it !is FillElement }.map { encodeElement(it) }),
                )
            },
            restore = { savedMap ->
                EzCanvasState().apply {
                    tool = Tool.entries[savedMap["tool"] as Int]
                    strokeColor = Color(savedMap["color"] as Int)
                    strokeWidthPx = savedMap["width"] as Float
                    strokeAlpha = savedMap["alpha"] as Float
                    eraserWidthPx = savedMap["eraser"] as Float
                    lineStyle = LineStyle.entries[savedMap["style"] as Int]
                    smoothing = savedMap["smoothing"] as Boolean
                    backgroundColor = Color(savedMap["bg"] as Int)
                    backgroundPattern = BackgroundPattern.entries[savedMap["pattern"] as Int]
                    @Suppress("UNCHECKED_CAST")
                    (savedMap["elements"] as ArrayList<FloatArray>).forEach { elements.add(decodeElement(it)) }
                }
            },
        )
    }
}

/** Create and remember an [EzCanvasState] that survives configuration changes. */
@Composable
fun rememberEzCanvasState(): EzCanvasState =
    rememberSaveable(saver = EzCanvasState.Saver) { EzCanvasState() }

// --- Element (de)serialization for the Saver ------------------------------
// Each element is a FloatArray whose first slot tags the type, so new element types are
// additive. Colors are stored losslessly via Float.fromBits / toRawBits.
//
// Stroke: [TYPE_STROKE, colorBits, width, alpha, styleOrdinal, toolOrdinal, x0,y0,ws0, x1,y1,ws1, ...]
// Shape:  [TYPE_SHAPE,  colorBits, width, alpha, styleOrdinal, kindOrdinal, sx, sy, ex, ey]
// Fills are not serialized (baked bitmaps) — filter them out before calling encodeElement.

internal const val SAVE_VERSION = 2
private const val TYPE_STROKE = 0f
private const val TYPE_SHAPE = 1f

internal fun encodeElement(element: CanvasElement): FloatArray = when (element) {
    is StrokeElement -> FloatArray(6 + element.points.size * 3).also { serializedData ->
        serializedData[0] = TYPE_STROKE
        serializedData[1] = Float.fromBits(element.color.toArgb())
        serializedData[2] = element.widthPx
        serializedData[3] = element.alpha
        serializedData[4] = element.style.ordinal.toFloat()
        serializedData[5] = element.tool.ordinal.toFloat()
        var dataIndex = 6
        for (point in element.points) {
            serializedData[dataIndex++] = point.x
            serializedData[dataIndex++] = point.y
            serializedData[dataIndex++] = point.widthScale
        }
    }

    is ShapeElement -> floatArrayOf(
        TYPE_SHAPE,
        Float.fromBits(element.color.toArgb()),
        element.widthPx,
        element.alpha,
        element.style.ordinal.toFloat(),
        element.kind.ordinal.toFloat(),
        element.start.x, element.start.y, element.end.x, element.end.y,
    )

    is FillElement -> error("FillElement is not serialized; filter it out before encoding")
}

internal fun decodeElement(serializedData: FloatArray): CanvasElement = if (serializedData[0] == TYPE_SHAPE) {
    ShapeElement(
        kind = ShapeKind.entries[serializedData[5].toInt()],
        start = Offset(serializedData[6], serializedData[7]),
        end = Offset(serializedData[8], serializedData[9]),
        color = Color(serializedData[1].toRawBits()),
        widthPx = serializedData[2],
        alpha = serializedData[3],
        style = LineStyle.entries[serializedData[4].toInt()],
    )
} else {
    val points = ArrayList<StrokePoint>((serializedData.size - 6) / 3)
    var dataIndex = 6
    while (dataIndex + 2 < serializedData.size) {
        points.add(StrokePoint(serializedData[dataIndex], serializedData[dataIndex + 1], serializedData[dataIndex + 2]))
        dataIndex += 3
    }
    StrokeElement(
        points = points,
        tool = Tool.entries[serializedData[5].toInt()],
        color = Color(serializedData[1].toRawBits()),
        widthPx = serializedData[2],
        alpha = serializedData[3],
        style = LineStyle.entries[serializedData[4].toInt()],
    )
}
