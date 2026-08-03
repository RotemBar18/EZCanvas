package com.ezcanvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.ezcanvas.model.TextElement
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

    /**
     * Bumped whenever an element changes. [EzCanvas] reads it while drawing, which guarantees a
     * repaint for edits made in place, such as recolouring the selected text. Replacing an item in
     * the list alone does not reliably invalidate the draw phase.
     */
    internal var revision by mutableIntStateOf(0)
        private set

    private fun invalidateCanvas() {
        revision++
    }

    // --- Drawing settings -------------------------------------------------

    var tool by mutableStateOf(Tool.PEN)
    var eraserWidthPx by mutableFloatStateOf(40f)

    // Colour, size and opacity apply to the selected text the moment they are set, so a toolbar
    // edit lands immediately rather than waiting for the canvas to recompose.
    private var strokeColorState by mutableStateOf(Color.Black)
    private var strokeWidthState by mutableFloatStateOf(10f)
    private var strokeAlphaState by mutableFloatStateOf(1f)

    var strokeColor: Color
        get() = strokeColorState
        set(value) {
            strokeColorState = value
            applySettingsToSelection()
        }

    var strokeWidthPx: Float
        get() = strokeWidthState
        set(value) {
            strokeWidthState = value
            applySettingsToSelection()
        }

    var strokeAlpha: Float
        get() = strokeAlphaState
        set(value) {
            strokeAlphaState = value
            applySettingsToSelection()
        }

    /** Outline dash style for the pen and every shape. Brushes other than pen are always solid. */
    var lineStyle by mutableStateOf(LineStyle.Solid)

    /** Smooth strokes with quadratic curves (vs. straight segments). */
    var smoothing by mutableStateOf(true)

    /**
     * Name of the drawing. Used as the exported file name, and shown by the
     * [ToolbarControl.Rename] control. Set it in code for a fixed name, or enable that control to
     * let the user edit it.
     */
    var drawingName by mutableStateOf("drawing")

    // --- Canvas / background ---------------------------------------------

    var backgroundColor by mutableStateOf(Color.White)
    var backgroundPattern by mutableStateOf(BackgroundPattern.None)

    /** Optional image drawn as the background (scaled to fill). Not persisted across rotation. */
    var backgroundImage by mutableStateOf<ImageBitmap?>(null)

    // --- History ----------------------------------------------------------

    /**
     * How many steps back [undo] can go. Unlimited by default.
     *
     * Set it to 1 for a single step back, or to any number for a shorter history. Older elements
     * stay on the canvas: this caps how far back the user can step, it never removes their work.
     */
    var maxUndoSteps: Int = Int.MAX_VALUE

    /** Elements before this index are permanent, because they fell outside [maxUndoSteps]. */
    private var undoFloor: Int = 0

    /** What [clear] wiped, so a single undo can put it back. */
    private var clearedElements: List<CanvasElement>? = null

    val isEmpty: Boolean get() = elements.isEmpty()
    val canUndo: Boolean get() = elements.size > undoFloor || clearedElements != null
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    internal fun commit(element: CanvasElement) {
        elements.add(element)
        // Raise the floor rather than dropping the element, so the drawing is never damaged.
        if (maxUndoSteps in 1..<elements.size - undoFloor) {
            undoFloor = elements.size - maxUndoSteps
        }
        redoStack.clear()
        // Drawing again is the point of no return for an undo of clear.
        clearedElements = null
        invalidateCanvas()
    }

    fun undo() {
        selectedIndex = null
        val cleared = clearedElements
        if (cleared != null) {
            // Undoing a clear restores the whole drawing in one step.
            elements.addAll(cleared)
            clearedElements = null
            undoFloor = 0
            invalidateCanvas()
            return
        }
        if (elements.size > undoFloor) {
            redoStack.add(elements.removeAt(elements.lastIndex))
            invalidateCanvas()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            elements.add(redoStack.removeAt(redoStack.lastIndex))
            invalidateCanvas()
        }
    }

    // --- Text selection ---------------------------------------------------

    /** Index of the selected [TextElement], or null. Only text can be selected. */
    internal var selectedIndex by mutableStateOf<Int?>(null)

    /** True while a piece of text is selected, so callers can show it differently. */
    val hasSelection: Boolean get() = selectedIndex != null

    /**
     * Select the text at [index] and pull its colour, opacity and size into the toolbar.
     * The backing fields are written directly: going through the setters would push the toolbar's
     * previous values back onto the text before all three have been read.
     */
    internal fun selectTextAt(index: Int) {
        val element = elements.getOrNull(index) as? TextElement ?: return
        selectedIndex = index
        strokeColorState = element.color
        strokeAlphaState = element.alpha
        strokeWidthState = element.sizePx / TextSizeFactor
    }

    /** Drop the selection, for example when switching tool or tapping empty canvas. */
    fun clearSelection() {
        selectedIndex = null
    }

    /**
     * Push the current colour, opacity and size onto the selected text. Called whenever those
     * settings change, so editing the toolbar edits the selected text instead of the next stroke.
     */
    internal fun applySettingsToSelection() {
        val index = selectedIndex ?: return
        val element = elements.getOrNull(index) as? TextElement
        if (element == null) {
            selectedIndex = null
            return
        }
        val updated = element.copy(
            color = strokeColor,
            alpha = strokeAlpha,
            sizePx = strokeWidthPx * TextSizeFactor,
        )
        if (updated != element) {
            elements[index] = updated
            invalidateCanvas()
        }
    }

    /** Move the selected text by [delta] pixels. */
    internal fun moveSelectionBy(delta: Offset) {
        val index = selectedIndex ?: return
        val element = elements.getOrNull(index) as? TextElement ?: return
        elements[index] = element.copy(topLeft = element.topLeft + delta)
        invalidateCanvas()
    }

    /** Wipe the canvas. A single [undo] brings the whole drawing back. */
    fun clear() {
        if (elements.isEmpty()) return
        clearedElements = elements.toList()
        elements.clear()
        redoStack.clear()
        undoFloor = 0
        selectedIndex = null
        invalidateCanvas()
    }

    /**
     * Move every element from the last canvas size to [newWidth] x [newHeight].
     *
     * Coordinates are stored in canvas pixels, so without this a drawing would stay pinned to the
     * top left when the canvas changes shape, for example on rotation. The drawing keeps its real
     * size and stroke widths, and is shifted so its centre matches the new canvas centre. A
     * drawing wider or taller than the new canvas therefore runs past the edge, which is the
     * trade for never distorting or shrinking someone's work.
     */
    internal fun remapTo(newWidth: Int, newHeight: Int) {
        val oldWidth = widthPx
        val oldHeight = heightPx
        if (oldWidth <= 0 || oldHeight <= 0) return
        if (oldWidth == newWidth && oldHeight == newHeight) return
        if (elements.isEmpty() && redoStack.isEmpty()) return

        val shiftX = (newWidth - oldWidth) / 2f
        val shiftY = (newHeight - oldHeight) / 2f
        if (shiftX == 0f && shiftY == 0f) return

        fun mapPoint(x: Float, y: Float) = Offset(x + shiftX, y + shiftY)

        fun remap(element: CanvasElement): CanvasElement = when (element) {
            is StrokeElement -> element.copy(
                points = element.points.map { StrokePoint(it.x + shiftX, it.y + shiftY) },
            )

            is ShapeElement -> element.copy(
                start = mapPoint(element.start.x, element.start.y),
                end = mapPoint(element.end.x, element.end.y),
            )

            is TextElement -> element.copy(
                topLeft = mapPoint(element.topLeft.x, element.topLeft.y),
            )

            is FillElement -> FillElement(
                seed = mapPoint(element.seed.x, element.seed.y),
                color = element.color,
                image = element.image,
                topLeft = mapPoint(element.topLeft.x, element.topLeft.y),
                alpha = element.alpha,
            )
        }

        for (i in elements.indices) elements[i] = remap(elements[i])
        for (i in redoStack.indices) redoStack[i] = remap(redoStack[i])
        invalidateCanvas()
    }

    companion object {
        /** Saves settings + every element so the drawing survives rotation / process recreation. */
        val Saver = mapSaver(
            save = { state ->
                // Element rows are numbers, so text contents travel in a parallel list and each
                // text row stores its index into it.
                val texts = ArrayList<String>()
                val rows = ArrayList<FloatArray>()
                for (element in state.elements) {
                    if (element is TextElement) {
                        rows.add(encodeElement(element, texts.size))
                        texts.add(element.text)
                    } else {
                        rows.add(encodeElement(element, 0))
                    }
                }
                mapOf(
                    "v" to SAVE_VERSION,
                    "tool" to state.tool.ordinal,
                    "color" to state.strokeColor.toArgb(),
                    "width" to state.strokeWidthPx,
                    "alpha" to state.strokeAlpha,
                    "eraser" to state.eraserWidthPx,
                    "style" to state.lineStyle.ordinal,
                    "smoothing" to state.smoothing,
                    "name" to state.drawingName,
                    // The canvas size travels with the drawing so it can be remapped after a
                    // rotation instead of staying pinned to the top left.
                    "canvasW" to state.widthPx,
                    "canvasH" to state.heightPx,
                    "bg" to state.backgroundColor.toArgb(),
                    "pattern" to state.backgroundPattern.ordinal,
                    "elements" to rows,
                    "texts" to texts,
                )
            },
            restore = { savedMap ->
                // A bundle written by an older format cannot be read safely, so start fresh
                // rather than index into rows that have a different shape.
                if (savedMap["v"] != SAVE_VERSION) return@mapSaver null
                EzCanvasState().apply {
                    tool = Tool.entries[savedMap["tool"] as Int]
                    strokeColor = Color(savedMap["color"] as Int)
                    strokeWidthPx = savedMap["width"] as Float
                    strokeAlpha = savedMap["alpha"] as Float
                    eraserWidthPx = savedMap["eraser"] as Float
                    lineStyle = LineStyle.entries[savedMap["style"] as Int]
                    smoothing = savedMap["smoothing"] as Boolean
                    drawingName = savedMap["name"] as String
                    widthPx = savedMap["canvasW"] as Int
                    heightPx = savedMap["canvasH"] as Int
                    backgroundColor = Color(savedMap["bg"] as Int)
                    backgroundPattern = BackgroundPattern.entries[savedMap["pattern"] as Int]
                    @Suppress("UNCHECKED_CAST")
                    val texts = savedMap["texts"] as ArrayList<String>
                    @Suppress("UNCHECKED_CAST")
                    (savedMap["elements"] as ArrayList<FloatArray>).forEach { elements.add(decodeElement(it, texts)) }
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
// Stroke: [TYPE_STROKE, colorBits, width, alpha, styleOrdinal, toolOrdinal, x0,y0, x1,y1, ...]
// Shape:  [TYPE_SHAPE,  colorBits, width, alpha, styleOrdinal, kindOrdinal, sx, sy, ex, ey]
// Text:   [TYPE_TEXT,   colorBits, sizePx, alpha, textIndex, 0, x, y]
// Fill:   [TYPE_FILL,   colorBits, 0, alpha, 0, 0, seedX, seedY]
//
// A fill stores only its recipe, the seed and the colour, because the filled pixels are far too
// large for saved state. It is replayed from those after the canvas is laid out again.

internal const val SAVE_VERSION = 4
private const val TYPE_STROKE = 0f
private const val TYPE_SHAPE = 1f
private const val TYPE_TEXT = 2f
private const val TYPE_FILL = 3f

/** Toolbar size slider values are stroke widths, so text scales them up to a readable font size. */
internal const val TextSizeFactor = 2.5f

internal fun encodeElement(element: CanvasElement, textIndex: Int): FloatArray = when (element) {
    is StrokeElement -> FloatArray(6 + element.points.size * 2).also { serializedData ->
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

    is TextElement -> floatArrayOf(
        TYPE_TEXT,
        Float.fromBits(element.color.toArgb()),
        element.sizePx,
        element.alpha,
        textIndex.toFloat(),
        0f,
        element.topLeft.x, element.topLeft.y,
    )

    is FillElement -> floatArrayOf(
        TYPE_FILL,
        Float.fromBits(element.color.toArgb()),
        0f,
        element.alpha,
        0f,
        0f,
        element.seed.x, element.seed.y,
    )
}

internal fun decodeElement(serializedData: FloatArray, texts: List<String>): CanvasElement = if (serializedData[0] == TYPE_FILL) {
    // Pending: the pixels are replayed from the seed once the canvas has a size again.
    FillElement(
        seed = Offset(serializedData[6], serializedData[7]),
        color = Color(serializedData[1].toRawBits()),
        image = null,
        topLeft = Offset.Zero,
        alpha = serializedData[3],
    )
} else if (serializedData[0] == TYPE_TEXT) {
    TextElement(
        text = texts.getOrElse(serializedData[4].toInt()) { "" },
        topLeft = Offset(serializedData[6], serializedData[7]),
        sizePx = serializedData[2],
        color = Color(serializedData[1].toRawBits()),
        alpha = serializedData[3],
    )
} else if (serializedData[0] == TYPE_SHAPE) {
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
    val points = ArrayList<StrokePoint>((serializedData.size - 6) / 2)
    var dataIndex = 6
    while (dataIndex + 1 < serializedData.size) {
        points.add(StrokePoint(serializedData[dataIndex], serializedData[dataIndex + 1]))
        dataIndex += 2
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
