package com.ezcanvas.demo.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ezcanvas.EzCanvas
import com.ezcanvas.rememberEzCanvasState
import com.ezcanvas.shareAsPng
import com.ezcanvas.demo.ui.EzColors

/**
 * A delivery form with the canvas as one field.
 *
 * The smallest possible integration: no toolbar at all. The library defaults are already a black
 * pen on white, so the recipient just signs. Everything else on this screen is ordinary app UI,
 * and the app drives Clear and Confirm through the state.
 */
@Composable
internal fun SignaturePadExample(onBack: () -> Unit) = ExampleScaffold(
    title = "Signature Pad",
    subtitle = "EzCanvas only, no toolbar",
    onBack = onBack,
    background = EzColors.AppBg,
) {
    val state = rememberEzCanvasState()
    val context = LocalContext.current
    var signed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        state.strokeWidthPx = 5f
        state.drawingName = "signature-4821"
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Confirm delivery", style = MaterialTheme.typography.headlineSmall, color = EzColors.Ink)

        FormCard {
            FormRow("Recipient", "Rotem Bar")
            FormRow("Order", "#4821")
            FormRow("Address", "12 Rothschild Blvd, Tel Aviv")
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "SIGNATURE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = EzColors.SectionLabel,
            )

            // The whole integration: a sized box and one EzCanvas.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(EzColors.Surface)
                    .border(1.dp, EzColors.Divider, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                EzCanvas(state, Modifier.fillMaxSize())
                if (state.isEmpty) {
                    Text("Sign here", style = MaterialTheme.typography.bodyMedium, color = EzColors.SectionLabel)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (signed) "Signed and saved as ${state.drawingName}.png" else "Draw your signature above",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (signed) EzColors.Teal else EzColors.Subtle,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        state.clear()
                        signed = false
                    },
                    enabled = !state.isEmpty,
                ) { Text("Clear") }
            }
        }

        Button(
            onClick = { signed = true },
            enabled = !state.isEmpty && !signed,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) { Text(if (signed) "Delivery confirmed" else "Confirm delivery") }

        if (signed) {
            // Transparent, so the signature drops onto a document without a white box behind it.
            TextButton(
                onClick = { state.shareAsPng(context, transparentBackground = true) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Share the signature") }
        }
    }
}

/** Gallery card art: a signature resting on a ruled line. */
internal fun DrawScope.signaturePreview() {
    val w = size.width
    val h = size.height
    drawLine(Color(0xFFD8D5CE), Offset(w * 0.12f, h * 0.72f), Offset(w * 0.88f, h * 0.72f), 2f)
    val path = Path().apply {
        moveTo(w * 0.22f, h * 0.6f)
        cubicTo(w * 0.30f, h * 0.28f, w * 0.38f, h * 0.82f, w * 0.48f, h * 0.52f)
        cubicTo(w * 0.55f, h * 0.26f, w * 0.62f, h * 0.74f, w * 0.72f, h * 0.48f)
        cubicTo(w * 0.77f, h * 0.32f, w * 0.84f, h * 0.58f, w * 0.9f, h * 0.44f)
    }
    drawPath(path, Color(0xFF1B1B1F), style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
