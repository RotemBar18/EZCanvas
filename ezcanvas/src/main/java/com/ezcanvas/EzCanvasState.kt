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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.ezcanvas.model.BackgroundPattern
import com.ezcanvas.model.CanvasElement
import com.ezcanvas.model.FillElement
import com.ezcanvas.model.LineStyle
import com.ezcanvas.model.ShapeElement
import com.ezcanvas.model.StrokeElement
import com.ezcanvas.model.StrokePoint
import com.ezcanvas.model.TextElement
import com.ezcanvas.model.Tool
import kotlin.math.max
import kotlin.math.min

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

    var tool by mutableStateOf(Tool.Pen)
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
    }

    fun undo() {
        selectedIndex = null
        val cleared = clearedElements
        if (cleared != null) {
            // Undoing a clear restores the whole drawing in one step.
            elements.addAll(cleared)
            clearedElements = null
            undoFloor = 0
            return
        }
        if (elements.size > undoFloor) {
            redoStack.add(elements.removeAt(elements.lastIndex))
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            elements.add(redoStack.removeAt(redoStack.lastIndex))
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
        }
    }

    /** Move the selected text by [delta] pixels. */
    internal fun moveSelectionBy(delta: Offset) {
        val index = selectedIndex ?: return
        val element = elements.getOrNull(index) as? TextElement ?: return
        elements[index] = element.copy(topLeft = element.topLeft + delta)
    }

    /** The box every element sits inside, in canvas pixels, or null when nothing is drawn. */
    private fun drawingBounds(): Rect? {
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE

        fun include(x: Float, y: Float) {
            if (x < left) left = x
            if (x > right) right = x
            if (y < top) top = y
            if (y > bottom) bottom = y
        }

        for (element in elements) {
            when (element) {
                is StrokeElement -> element.points.forEach { include(it.x, it.y) }
                is ShapeElement -> {
                    include(element.start.x, element.start.y)
                    include(element.end.x, element.end.y)
                }
                is TextElement -> include(element.topLeft.x, element.topLeft.y)
                is FillElement -> include(element.topLeft.x, element.topLeft.y)
            }
        }
        if (left > right) return null
        return Rect(left, top, right, bottom)
    }

    /** Wipe the canvas. A single [undo] brings the whole drawing back. */
    fun clear() {
        if (elements.isEmpty()) return
        clearedElements = elements.toList()
        elements.clear()
        redoStack.clear()
        undoFloor = 0
        selectedIndex = null
    }

    /**
     * Move every element from the last canvas size to [newWidth] x [newHeight].
     *
     * Coordinates are stored in canvas pixels, so without this a drawing would stay pinned to the
     * top left when the canvas changes shape, for example on rotation. The drawing keeps its real
     * size and stroke widths, and is shifted so its centre matches the new canvas centre, then
     * pulled back far enough to stay in view. Work is never distorted or scaled to fit.
     */
    internal fun remapTo(newWidth: Int, newHeight: Int) {
        val oldWidth = widthPx
        val oldHeight = heightPx
        if (oldWidth <= 0 || oldHeight <= 0) return
        // A collapsed layout pass is not a resize. A host whose canvas is briefly squeezed to
        // nothing, which a Column does to a weighted child when its siblings no longer fit, would
        // otherwise have the drawing shifted by half the old canvas and left there: the recorded
        // size is then zero, so the pass that restores a real size is refused by the guard above.
        if (newWidth <= 0 || newHeight <= 0) return
        if (oldWidth == newWidth && oldHeight == newHeight) return
        if (elements.isEmpty() && redoStack.isEmpty()) return

        var shiftX = (newWidth - oldWidth) / 2f
        var shiftY = (newHeight - oldHeight) / 2f

        // Centring alone pushes work out of sight when the canvas gets much shorter, which is what
        // rotating into landscape does. Two limits hold it in view: one puts the drawing's leading
        // edge at the canvas edge, the other its trailing edge. A drawing smaller than the canvas
        // is kept fully inside; one larger than the canvas is kept covering it. Which limit is the
        // lower of the two flips as the drawing outgrows the canvas, so they are sorted rather
        // than assumed, and the clamp then reads the same in both cases.
        val bounds = drawingBounds()
        if (bounds != null) {
            val leadingX = -bounds.left
            val trailingX = newWidth - bounds.right
            shiftX = shiftX.coerceIn(min(leadingX, trailingX), max(leadingX, trailingX))

            val leadingY = -bounds.top
            val trailingY = newHeight - bounds.bottom
            shiftY = shiftY.coerceIn(min(leadingY, trailingY), max(leadingY, trailingY))
        }
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

/**
 * Create and remember an [EzCanvasState] that survives configuration changes.
 *
 * The parameters are starting values, applied only when the state is first created. After a
 * rotation the saved state wins, so a name the user typed or a colour they picked is kept. Setting
 * these here rather than assigning properties afterwards is what makes that true: an assignment in
 * a `LaunchedEffect` runs again when the activity is recreated and would overwrite their choices.
 */
@Composable
fun rememberEzCanvasState(
    tool: Tool = Tool.Pen,
    strokeColor: Color = Color.Black,
    strokeWidthPx: Float = 10f,
    strokeAlpha: Float = 1f,
    eraserWidthPx: Float = 40f,
    lineStyle: LineStyle = LineStyle.Solid,
    backgroundColor: Color = Color.White,
    backgroundPattern: BackgroundPattern = BackgroundPattern.None,
    drawingName: String = "drawing",
): EzCanvasState = rememberSaveable(saver = EzCanvasState.Saver) {
    EzCanvasState().also {
        it.tool = tool
        it.strokeColor = strokeColor
        it.strokeWidthPx = strokeWidthPx
        it.strokeAlpha = strokeAlpha
        it.eraserWidthPx = eraserWidthPx
        it.lineStyle = lineStyle
        it.backgroundColor = backgroundColor
        it.backgroundPattern = backgroundPattern
        it.drawingName = drawingName
    }
}
