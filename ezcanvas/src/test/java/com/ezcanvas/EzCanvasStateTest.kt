package com.ezcanvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.ezcanvas.model.FillElement
import com.ezcanvas.model.LineStyle
import com.ezcanvas.model.ShapeElement
import com.ezcanvas.model.ShapeKind
import com.ezcanvas.model.StrokeElement
import com.ezcanvas.model.StrokePoint
import com.ezcanvas.model.TextElement
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
        assertFalse(state.canRedo)
    }

    @Test
    fun clear_is_undoable_in_one_step() {
        val state = EzCanvasState()
        state.commit(stroke())
        state.commit(stroke())

        state.clear()
        assertTrue(state.isEmpty)
        assertTrue(state.canUndo) // an accidental clear is recoverable

        state.undo()
        assertEquals(2, state.elements.size) // the whole drawing returns at once
    }

    @Test
    fun drawing_again_makes_a_clear_permanent() {
        val state = EzCanvasState()
        state.commit(stroke())
        state.clear()
        state.commit(stroke())

        state.undo() // undoes the new stroke, not the clear
        assertTrue(state.isEmpty)
        assertFalse(state.canUndo)
    }

    @Test
    fun undo_is_capped_without_losing_any_of_the_drawing() {
        val state = EzCanvasState().apply { maxUndoSteps = 2 }
        repeat(5) { state.commit(stroke()) }

        // Everything drawn is still on the canvas; only the reach of undo is limited.
        assertEquals(5, state.elements.size)

        state.undo()
        state.undo()
        assertEquals(3, state.elements.size)

        // The third step back is refused, because it would pass the cap.
        assertFalse(state.canUndo)
        state.undo()
        assertEquals(3, state.elements.size)
    }

    @Test
    fun undo_is_unlimited_by_default() {
        val state = EzCanvasState()
        repeat(40) { state.commit(stroke()) }
        repeat(40) { state.undo() }
        assertTrue(state.isEmpty)
    }

    @Test
    fun stroke_round_trips_through_saver_encoding() {
        val original = StrokeElement(
            points = listOf(StrokePoint(3f, 4f), StrokePoint(5f, 6f)),
            tool = Tool.MARKER,
            color = Color.White, // 0xFFFFFFFF exercises the Float.fromBits/toRawBits color path
            widthPx = 12f,
            alpha = 0.7f,
            style = LineStyle.Dashed,
        )
        assertEquals(original, decodeElement(encodeElement(original, 0), emptyList()))
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
        assertEquals(original, decodeElement(encodeElement(original, 0), emptyList()))
    }

    @Test
    fun text_round_trips_with_its_contents_alongside() {
        val original = TextElement(
            text = "Check this",
            topLeft = Offset(12f, 34f),
            sizePx = 40f,
            color = Color(0xFFE0463B),
            alpha = 0.8f,
        )
        // Text contents live in a parallel list; the row stores the index into it.
        assertEquals(original, decodeElement(encodeElement(original, 0), listOf("Check this")))
    }

    @Test
    fun fill_survives_as_a_recipe_rather_than_pixels() {
        val original = FillElement(
            seed = Offset(120f, 80f),
            color = Color(0xFF22C55E),
            image = null,
            topLeft = Offset.Zero,
            alpha = 0.9f,
        )

        val restored = decodeElement(encodeElement(original, 0), emptyList()) as FillElement

        // Only the seed and the paint are persisted; the pixels are replayed after layout.
        assertEquals(original.seed, restored.seed)
        assertEquals(original.color, restored.color)
        assertEquals(original.alpha, restored.alpha, 0.01f)
        assertTrue(restored.isPending)
    }

    @Test
    fun editing_settings_updates_the_selected_text_only() {
        val state = EzCanvasState()
        state.commit(stroke())
        state.commit(
            TextElement(text = "Label", topLeft = Offset(5f, 5f), sizePx = 25f, color = Color.Black),
        )

        state.selectTextAt(1)
        // Selecting pulls the text's own size back into the toolbar.
        assertEquals(25f / TextSizeFactor, state.strokeWidthPx, 0.01f)

        // Setting the colour alone is enough; no extra call is needed to make it stick.
        state.strokeColor = Color.Red

        assertEquals(Color.Red, (state.elements[1] as TextElement).color)
        assertEquals(Color.Black, (state.elements[0] as StrokeElement).color) // stroke untouched
    }

    @Test
    fun selecting_text_does_not_overwrite_it_with_the_previous_settings() {
        val state = EzCanvasState()
        // The toolbar is on a big red brush.
        state.strokeColor = Color.Red
        state.strokeWidthPx = 50f
        state.strokeAlpha = 0.2f

        state.commit(
            TextElement(text = "Label", topLeft = Offset.Zero, sizePx = 25f, color = Color.Blue, alpha = 1f),
        )
        state.selectTextAt(0)

        // Selecting reads the text, it must not push the previous brush settings onto it.
        val selected = state.elements[0] as TextElement
        assertEquals(Color.Blue, selected.color)
        assertEquals(25f, selected.sizePx, 0.01f)
        assertEquals(1f, selected.alpha, 0.01f)
    }

    @Test
    fun remap_recentres_without_resizing() {
        val state = EzCanvasState()
        state.widthPx = 100
        state.heightPx = 200
        // A stroke from the canvas centre to a point 25px to its right.
        state.commit(
            StrokeElement(
                points = listOf(StrokePoint(50f, 100f), StrokePoint(75f, 100f)),
                tool = Tool.PEN,
                color = Color.Black,
                widthPx = 10f,
            ),
        )

        // Rotate: 100x200 becomes 200x100.
        state.remapTo(200, 100)

        val remapped = state.elements.first() as StrokeElement
        assertEquals(100f, remapped.points[0].x, 0.01f) // centre stays the centre
        assertEquals(50f, remapped.points[0].y, 0.01f)
        assertEquals(125f, remapped.points[1].x, 0.01f) // still 25px from the centre
        assertEquals(50f, remapped.points[1].y, 0.01f)
        assertEquals(10f, remapped.widthPx, 0.01f) // stroke width untouched
    }

    @Test
    fun remap_is_a_no_op_without_a_previous_size() {
        val state = EzCanvasState()
        state.commit(stroke())
        val before = state.elements.first()
        state.remapTo(500, 500)
        assertEquals(before, state.elements.first())
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
