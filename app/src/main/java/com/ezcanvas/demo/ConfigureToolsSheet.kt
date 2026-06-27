package com.ezcanvas.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ezcanvas.ToolbarControl
import com.ezcanvas.demo.ui.EzColors
import com.ezcanvas.model.Tool

/** Every tool the demo offers, in display order. */
val AllTools: Set<Tool> = Tool.entries.toSet()

private val ToolChips: List<Pair<Tool, String>> = listOf(
    Tool.PEN to "Pen", Tool.MARKER to "Marker", Tool.NEON to "Neon", Tool.CALLIGRAPHY to "Calligraphy",
    Tool.ERASER to "Eraser", Tool.LINE to "Line", Tool.SQUARE to "Square", Tool.CIRCLE to "Circle",
    Tool.BUCKET to "Bucket",
)

private enum class Grp(val title: String) { Controls("CONTROLS"), Background("BACKGROUND"), Actions("ACTIONS") }

private val ControlChips: List<Triple<ToolbarControl, String, Grp>> = listOf(
    Triple(ToolbarControl.ColorPicker, "Color", Grp.Controls),
    Triple(ToolbarControl.StrokeWidth, "Brush size", Grp.Controls),
    Triple(ToolbarControl.Opacity, "Opacity", Grp.Controls),
    Triple(ToolbarControl.EraserSize, "Eraser size", Grp.Controls),
    Triple(ToolbarControl.Style, "Line style", Grp.Controls),
    Triple(ToolbarControl.Background, "Color", Grp.Background),
    Triple(ToolbarControl.Pattern, "Pattern", Grp.Background),
    Triple(ToolbarControl.Image, "Image", Grp.Background),
    Triple(ToolbarControl.Undo, "Undo", Grp.Actions),
    Triple(ToolbarControl.Redo, "Redo", Grp.Actions),
    Triple(ToolbarControl.Clear, "Clear", Grp.Actions),
    Triple(ToolbarControl.Export, "Export", Grp.Actions),
)

/**
 * "Configure tools" — toggles which tools and controls the library [com.ezcanvas.EzToolbar]
 * renders. The toolbar itself is the library's; this sheet just decides what it shows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureToolsSheet(
    tools: Set<Tool>,
    controls: Set<ToolbarControl>,
    onToggleTool: (Tool) -> Unit,
    onToggleControl: (ToolbarControl) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = EzColors.Surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Configure tools", style = MaterialTheme.typography.titleMedium, color = EzColors.Ink)
                    Text(
                        "Toggle what your users get",
                        style = MaterialTheme.typography.bodySmall,
                        color = EzColors.Subtle,
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(EzColors.Ink)
                        .clickable { onDismiss() }
                        .padding(horizontal = 20.dp, vertical = 9.dp),
                ) {
                    Text("Done", color = EzColors.Surface, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = EzColors.Divider)

            SectionLabel("TOOLS")
            ChipFlow {
                ToolChips.forEach { (tool, label) ->
                    ToggleChip(label, tool in tools) { onToggleTool(tool) }
                }
            }

            Grp.entries.forEach { group ->
                SectionLabel(group.title)
                ChipFlow {
                    ControlChips.filter { it.third == group }.forEach { (control, label, _) ->
                        ToggleChip(label, control in controls) { onToggleControl(control) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(18.dp))
    Text(text, style = MaterialTheme.typography.labelSmall, color = EzColors.SectionLabel)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun ToggleChip(label: String, active: Boolean, onClick: () -> Unit) {
    val base = Modifier
        .clip(RoundedCornerShape(9.dp))
        .background(if (active) EzColors.Ink else EzColors.Surface)
    val shaped = if (active) base else base.border(1.dp, EzColors.Divider, RoundedCornerShape(9.dp))
    Box(
        shaped
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (active) EzColors.Surface else EzColors.Muted,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
            ),
        )
    }
}
