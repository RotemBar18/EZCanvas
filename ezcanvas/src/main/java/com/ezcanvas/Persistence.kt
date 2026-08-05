package com.ezcanvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ezcanvas.model.CanvasElement
import com.ezcanvas.model.FillElement
import com.ezcanvas.model.LineStyle
import com.ezcanvas.model.ShapeElement
import com.ezcanvas.model.ShapeKind
import com.ezcanvas.model.StrokeElement
import com.ezcanvas.model.StrokePoint
import com.ezcanvas.model.TextElement
import com.ezcanvas.model.Tool

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
