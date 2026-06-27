package com.ezcanvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.ezcanvas.model.LineStyle
import com.ezcanvas.model.ShapeElement
import com.ezcanvas.model.ShapeKind
import com.ezcanvas.model.StrokeElement
import com.ezcanvas.model.StrokePoint
import com.ezcanvas.model.Tool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [EzCanvasState] history logic and the Saver's element (de)serialization. */
class EzCanvasStateTest {

    private fun stroke() = StrokeElement(
        points = listOf(StrokePoint(0f, 0f), StrokePoint(1f, 1f)),
        tool = Tool.PEN,
        color = Color.Black,
        widthPx = 8f,
    )

    @Test
    fun commit_then_undo_then_redo() {
        val state = EzCanvasState()
        assertTrue(state.isEmpty)
        assertFalse(state.canUndo)

        state.commit(stroke())
        state.commit(stroke())
        assertEquals(2, state.elements.size)
        assertTrue(state.canUndo)
        assertFalse(state.canRedo)

        state.undo()
        assertEquals(1, state.elements.size)
        assertTrue(state.canRedo)

        state.redo()
        assertEquals(2, state.elements.size)
        assertFalse(state.canRedo)
    }

    @Test
    fun commit_clears_redo_stack() {
        val state = EzCanvasState()
        state.commit(stroke())
        state.undo()
        assertTrue(state.canRedo)

        state.commit(stroke())
        assertFalse(state.canRedo)
    }

    @Test
    fun clear_removes_everything() {
        val state = EzCanvasState()
        state.commit(stroke())
        state.commit(stroke())
        state.clear()
        assertTrue(state.isEmpty)
        assertFalse(state.canUndo)
        assertFalse(state.canRedo)
    }

    @Test
    fun history_is_capped_at_maxHistorySize() {
        val state = EzCanvasState().apply { maxHistorySize = 3 }
        repeat(5) { state.commit(stroke()) }
        assertEquals(3, state.elements.size)
    }

    @Test
    fun stroke_round_trips_through_saver_encoding() {
        val original = StrokeElement(
            points = listOf(StrokePoint(3f, 4f, 0.5f), StrokePoint(5f, 6f, 1f)),
            tool = Tool.MARKER,
            color = Color.White, // 0xFFFFFFFF exercises the Float.fromBits/toRawBits color path
            widthPx = 12f,
            alpha = 0.7f,
            style = LineStyle.Dashed,
        )
        assertEquals(original, decodeElement(encodeElement(original)))
    }

    @Test
    fun shape_round_trips_through_saver_encoding() {
        val original = ShapeElement(
            kind = ShapeKind.Circle,
            start = Offset(10f, 20f),
            end = Offset(40f, 50f),
            color = Color(0xFF2563EB),
            widthPx = 6f,
            alpha = 0.9f,
            style = LineStyle.DashDot,
        )
        assertEquals(original, decodeElement(encodeElement(original)))
    }

    @Test
    fun scanline_flood_stops_at_boundary() {
        val w = 5
        val h = 5
        val empty = 0                  // transparent
        val wall = 0xFFFFFFFF.toInt()  // opaque boundary
        val pixels = IntArray(w * h) { empty }
        for (y in 0 until h) pixels[y * w + 2] = wall // vertical wall at column 2

        val region = scanlineFlood(pixels, w, h, 0, 0, empty, 60)!!

        for (y in 0 until h) {
            assertTrue(region.mask[y * w + 0])  // left of wall: filled
            assertTrue(region.mask[y * w + 1])
            assertFalse(region.mask[y * w + 2]) // wall itself: not filled
            assertFalse(region.mask[y * w + 3]) // right of wall: unreachable
            assertFalse(region.mask[y * w + 4])
        }
        assertEquals(0, region.minX)
        assertEquals(1, region.maxX)
    }
}
