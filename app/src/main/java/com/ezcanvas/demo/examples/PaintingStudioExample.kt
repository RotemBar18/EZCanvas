package com.ezcanvas.demo.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ezcanvas.EzCanvas
import com.ezcanvas.EzToolbar
import com.ezcanvas.rememberEzCanvasState
import com.ezcanvas.shareAsPng
import com.ezcanvas.demo.ui.EzColors

/**
 * The whole library with nothing switched off. `EzToolbar(state)` is the entire toolbar setup, so
 * the app only supplies a canvas, a share button and a place to put the bar.
 */
@Composable
internal fun PaintingStudioExample(onBack: () -> Unit) = ExampleScaffold(
    title = "Painting Studio",
    subtitle = "EzCanvas + EzToolbar, nothing switched off",
    onBack = onBack,
) {
    val state = rememberEzCanvasState(
        strokeColor = Color(0xFF6366F1),
        drawingName = "painting",
    )
    val context = LocalContext.current

    Box(Modifier.weight(1f).fillMaxWidth()) {
        EzCanvas(state, Modifier.fillMaxSize())

        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(EzColors.Surface.copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    "${state.strokeWidthPx.toInt()} px",
                    style = MaterialTheme.typography.labelMedium,
                    color = EzColors.Muted,
                )
            }
            Box(
                Modifier
                    .size(40.dp)
                    .shadow(4.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(EzColors.Ink)
                    .clickable(enabled = !state.isEmpty) { state.shareAsPng(context) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.IosShare, "Share", tint = EzColors.Surface, modifier = Modifier.size(18.dp))
            }
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        EzToolbar(state)
    }
}

/** Gallery card art: layered translucent paint strokes. */
internal fun DrawScope.paintingPreview() {
    val w = size.width
    val h = size.height
    drawLine(
        Color(0xFF14B8A6).copy(alpha = 0.6f),
        Offset(w * 0.12f, h * 0.7f),
        Offset(w * 0.6f, h * 0.3f),
        22f,
        cap = StrokeCap.Round,
    )
    drawLine(
        Color(0xFFFB6F61).copy(alpha = 0.6f),
        Offset(w * 0.2f, h * 0.35f),
        Offset(w * 0.72f, h * 0.7f),
        18f,
        cap = StrokeCap.Round,
    )
    drawLine(
        Color(0xFF6366F1).copy(alpha = 0.85f),
        Offset(w * 0.45f, h * 0.25f),
        Offset(w * 0.9f, h * 0.55f),
        8f,
        cap = StrokeCap.Round,
    )
    drawCircle(Color(0xFFF59E0B).copy(alpha = 0.8f), radius = h * 0.12f, center = Offset(w * 0.8f, h * 0.34f))
}
