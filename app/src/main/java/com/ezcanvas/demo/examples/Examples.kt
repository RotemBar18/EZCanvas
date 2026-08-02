package com.ezcanvas.demo.examples

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Card metadata for the gallery. Each entry points at one example file, so a developer can open a
 * single file and see one complete integration of the library.
 */
data class ExampleInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    val tags: List<String>,
    val background: Color,
    val preview: DrawScope.() -> Unit,
)

/**
 * Seven integrations, ordered from a canvas embedded in a form up to a full screen app.
 * Each one lives in its own file next to this list.
 */
val Examples: List<ExampleInfo> = listOf(
    ExampleInfo(
        id = "signature",
        title = "Signature Pad",
        subtitle = "A field inside a delivery form",
        tags = listOf("No toolbar", "Canvas only"),
        background = Color.White,
        preview = { signaturePreview() },
    ),
    ExampleInfo(
        id = "markup",
        title = "Photo Markup",
        subtitle = "Pick a photo, then annotate it",
        tags = listOf("Photo picker", "Trimmed toolbar"),
        background = Color(0xFFEFEDE7),
        preview = { markupPreview() },
    ),
    ExampleInfo(
        id = "game",
        title = "Drawing Game",
        subtitle = "Sketch, pass, guess",
        tags = listOf("One pen", "A few colors"),
        background = Color.White,
        preview = { gamePreview() },
    ),
    ExampleInfo(
        id = "whiteboard",
        title = "Classroom Whiteboard",
        subtitle = "Teach on a grid",
        tags = listOf("Markers + shapes", "Undo/redo"),
        background = Color.White,
        preview = { whiteboardPreview() },
    ),
    ExampleInfo(
        id = "kids",
        title = "Kids Doodle",
        subtitle = "Big, bright, forgiving",
        tags = listOf("3 controls", "Locked palette"),
        background = Color(0xFFFFFDE7),
        preview = { kidsPreview() },
    ),
    ExampleInfo(
        id = "painting",
        title = "Painting Studio",
        subtitle = "The whole library, on",
        tags = listOf("Every brush", "Every control"),
        background = Color(0xFFFCFBF8),
        preview = { paintingPreview() },
    ),
    ExampleInfo(
        id = "neon",
        title = "Neon Art",
        subtitle = "Glow on black",
        tags = listOf("Dark theme", "Neon brush"),
        background = Color(0xFF0F172A),
        preview = { neonPreview() },
    ),
)

/** Opens one example by id. */
@Composable
fun ExampleScreen(exampleId: String, onBack: () -> Unit) {
    when (exampleId) {
        "markup" -> PhotoMarkupExample(onBack)
        "game" -> DrawingGameExample(onBack)
        "whiteboard" -> WhiteboardExample(onBack)
        "kids" -> KidsDoodleExample(onBack)
        "painting" -> PaintingStudioExample(onBack)
        "neon" -> NeonArtExample(onBack)
        else -> SignaturePadExample(onBack)
    }
}
