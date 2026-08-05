package com.ezcanvas.demo.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ezcanvas.EzCanvas
import com.ezcanvas.EzToolbar
import com.ezcanvas.ToolbarControl
import com.ezcanvas.rememberBackgroundImagePicker
import com.ezcanvas.rememberEzCanvasState
import com.ezcanvas.shareAsPng
import com.ezcanvas.demo.ui.EzColors
import com.ezcanvas.model.Tool

private val MarkupPalette = listOf(
    Color(0xFFE0463B), Color(0xFFF59E0B), Color(0xFF22C55E), Color(0xFF2563EB),
)

/**
 * A photo annotation app. The user picks a photo from the device, it becomes the canvas
 * background, and the strokes go on top. Exporting flattens both into one PNG.
 *
 * `rememberBackgroundImagePicker` is the library's one call photo picker, so the app never touches
 * a content Uri or decodes a bitmap. The canvas is sized to the photo's aspect ratio so nothing
 * is stretched.
 */
@Composable
internal fun PhotoMarkupExample(onBack: () -> Unit) = ExampleScaffold(
    title = "Photo Markup",
    subtitle = "EzCanvas + EzToolbar, trimmed to markup",
    onBack = onBack,
    background = EzColors.AppBg,
) {
    val state = rememberEzCanvasState(
        tool = Tool.Circle,
        strokeColor = MarkupPalette.first(),
        strokeWidthPx = 6f,
        drawingName = "markup",
    )
    val context = LocalContext.current
    val pickPhoto = rememberBackgroundImagePicker(state)
    val photo = state.backgroundImage


    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (photo == null) {
            EmptyState(onPick = pickPhoto)
        } else {
            // The canvas takes the photo's own shape, so the image is never distorted.
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(photo.width.toFloat() / photo.height.toFloat())
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, EzColors.Divider, RoundedCornerShape(14.dp)),
            ) {
                EzCanvas(state, Modifier.fillMaxSize())
            }

            // A trimmed toolbar: only what an annotation pass needs.
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, EzColors.Divider, RoundedCornerShape(14.dp))
                    .background(EzColors.Surface),
            ) {
                EzToolbar(
                    state,
                    enabledTools = setOf(Tool.Pen, Tool.Marker, Tool.Line, Tool.Circle, Tool.Square, Tool.Text),
                    controls = setOf(
                        ToolbarControl.ToolSelector, ToolbarControl.ColorPicker, ToolbarControl.StrokeWidth,
                        ToolbarControl.Style, ToolbarControl.Undo, ToolbarControl.Clear,
                    ),
                    palette = MarkupPalette,
                    allowCustomColor = false,
                )
            }

            Button(
                onClick = { state.shareAsPng(context) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) { Text("Share the marked photo") }

            OutlinedButton(
                onClick = pickPhoto,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Choose a different photo") }
        }
    }
}

@Composable
private fun EmptyState(onPick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(EzColors.Surface)
            .border(1.dp, EzColors.Divider, RoundedCornerShape(14.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.AddPhotoAlternate,
            null,
            tint = EzColors.SectionLabel,
            modifier = Modifier.size(46.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text("No photo yet", style = MaterialTheme.typography.titleMedium, color = EzColors.Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            "Pick a photo from your device, then circle, draw and label anything on it. Tap with the text tool to add a label, and drag it to move it.",
            style = MaterialTheme.typography.bodySmall,
            color = EzColors.Subtle,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Button(onClick = onPick) { Text("Choose a photo") }
    }
}

/** Gallery card art: a dashed circle and a leader line over a grey photo. */
internal fun DrawScope.markupPreview() {
    val w = size.width
    val h = size.height
    drawRoundRect(
        Color(0xFFDCD8D0),
        topLeft = Offset(w * 0.16f, h * 0.16f),
        size = Size(w * 0.68f, h * 0.66f),
        cornerRadius = CornerRadius(10f, 10f),
    )
    drawCircle(Color(0xFFC4BEB1), radius = h * 0.1f, center = Offset(w * 0.66f, h * 0.38f))
    drawLine(Color(0xFFC4BEB1), Offset(w * 0.2f, h * 0.62f), Offset(w * 0.8f, h * 0.62f), 4f)
    val dash = PathEffect.dashPathEffect(floatArrayOf(9f, 7f))
    drawCircle(
        Color(0xFFE0463B),
        radius = h * 0.17f,
        center = Offset(w * 0.42f, h * 0.5f),
        style = Stroke(width = 3f, pathEffect = dash),
    )
    drawLine(Color(0xFFE0463B), Offset(w * 0.42f, h * 0.5f), Offset(w * 0.18f, h * 0.26f), 3f, cap = StrokeCap.Round)
}
