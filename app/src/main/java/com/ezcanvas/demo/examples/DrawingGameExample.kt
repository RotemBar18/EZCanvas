package com.ezcanvas.demo.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ezcanvas.EzCanvas
import com.ezcanvas.EzToolbar
import com.ezcanvas.ToolbarControl
import com.ezcanvas.rememberEzCanvasState
import com.ezcanvas.demo.ui.EzColors
import com.ezcanvas.model.Tool
import kotlinx.coroutines.delay

internal val GamePalette = listOf(
    Color(0xFF4F46E5), Color(0xFFFB6F61), Color(0xFF14B8A6), Color(0xFF111827),
)

private val Words = listOf("ELEPHANT", "LIGHTHOUSE", "ROCKET", "PIZZA", "GUITAR", "CACTUS", "OCTOPUS")
private const val RoundSeconds = 60

/**
 * A guessing game round. The player has a word and a timer, so the canvas has to be instant:
 * one pen, four colors, undo and clear. Everything else would slow the round down.
 */
@Composable
internal fun DrawingGameExample(onBack: () -> Unit) = ExampleScaffold(
    title = "Drawing Game",
    subtitle = "EzCanvas + EzToolbar, three controls",
    onBack = onBack,
    background = EzColors.AppBg,
) {
    val state = rememberEzCanvasState(strokeColor = GamePalette.first(), strokeWidthPx = 8f)
    var round by rememberSaveable { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(RoundSeconds) }
    val word = Words[round % Words.size]

    // Restart the clock whenever a new round begins.
    LaunchedEffect(round) {
        secondsLeft = RoundSeconds
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }

    val outOfTime = secondsLeft == 0

    // Round, word and timer share one row. Stacked, they cost about 60dp more, which is the
    // difference between a usable board and no board at all on a landscape phone.
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "ROUND ${round + 1}",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = EzColors.SectionLabel,
            )
            Text(
                word,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                color = EzColors.Ink,
            )
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (outOfTime) EzColors.Coral else EzColors.ChipBg)
                .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text(
                if (outOfTime) "TIME" else "0:%02d".format(secondsLeft),
                style = MaterialTheme.typography.labelLarge,
                color = if (outOfTime) EzColors.Surface else EzColors.Ink,
            )
        }
    }

    // The drawing area, sized as a card so it reads as part of the game board.
    Box(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(EzColors.Surface)
            .border(1.dp, EzColors.Divider, RoundedCornerShape(16.dp)),
    ) {
        EzCanvas(state, Modifier.fillMaxSize())
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Four colors, undo and clear. Nothing to read, nothing to configure.
        EzToolbar(
            state,
            enabledTools = setOf(Tool.Pen),
            controls = setOf(ToolbarControl.ColorPicker, ToolbarControl.Undo, ToolbarControl.Clear),
            palette = GamePalette,
            allowCustomColor = false,
        )
        Button(
            onClick = {
                state.clear()
                round++
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) { Text(if (outOfTime) "Next round" else "Done, next player") }
    }
}

/** Gallery card art: a quick sketched cat. */
internal fun DrawScope.gamePreview() {
    val w = size.width
    val h = size.height
    val ink = Color(0xFF4F46E5)
    val cx = w * 0.5f
    val cy = h * 0.54f
    val r = h * 0.24f
    drawCircle(ink, radius = r, center = Offset(cx, cy), style = Stroke(3.5f))
    drawLine(ink, Offset(cx - r * 0.7f, cy - r * 0.7f), Offset(cx - r * 0.95f, cy - r * 1.4f), 3.5f, cap = StrokeCap.Round)
    drawLine(ink, Offset(cx - r * 0.95f, cy - r * 1.4f), Offset(cx - r * 0.2f, cy - r * 0.95f), 3.5f, cap = StrokeCap.Round)
    drawLine(ink, Offset(cx + r * 0.7f, cy - r * 0.7f), Offset(cx + r * 0.95f, cy - r * 1.4f), 3.5f, cap = StrokeCap.Round)
    drawLine(ink, Offset(cx + r * 0.95f, cy - r * 1.4f), Offset(cx + r * 0.2f, cy - r * 0.95f), 3.5f, cap = StrokeCap.Round)
    drawCircle(ink, radius = 3f, center = Offset(cx - r * 0.35f, cy - r * 0.1f))
    drawCircle(ink, radius = 3f, center = Offset(cx + r * 0.35f, cy - r * 0.1f))
    drawCircle(Color(0xFFFB6F61), radius = 3.5f, center = Offset(cx, cy + r * 0.2f))
    drawLine(ink, Offset(cx - r * 0.1f, cy + r * 0.28f), Offset(cx - r * 0.7f, cy + r * 0.18f), 2f)
    drawLine(ink, Offset(cx + r * 0.1f, cy + r * 0.28f), Offset(cx + r * 0.7f, cy + r * 0.18f), 2f)
}
