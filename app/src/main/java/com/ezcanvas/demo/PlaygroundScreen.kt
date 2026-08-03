package com.ezcanvas.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ezcanvas.DefaultToolbarControls
import com.ezcanvas.EzCanvas
import com.ezcanvas.EzToolbar
import com.ezcanvas.ToolbarControl
import com.ezcanvas.rememberEzCanvasState
import com.ezcanvas.shareAsPng
import com.ezcanvas.demo.ui.BackgroundSwatches
import com.ezcanvas.demo.ui.BrushSwatches
import com.ezcanvas.demo.ui.EzColors
import com.ezcanvas.model.Tool

/**
 * Hi-fi Screen 1 — the canvas. The whole control surface is the library's [EzToolbar], hosted in
 * a slide-up bottom sheet; the app only provides branding (top bar) and decides which tools and
 * controls are enabled. That config is what the gear's Configure sheet toggles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaygroundScreen(onOpenExamples: () -> Unit) {
    val state = rememberEzCanvasState()
    val context = LocalContext.current
    val scaffoldState = rememberBottomSheetScaffoldState()
    var tools by remember { mutableStateOf(AllTools) }
    var controls by remember { mutableStateOf(DefaultToolbarControls) }
    var showConfig by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    // An export option, so it belongs to the app rather than to the toolbar.
    var transparentExport by rememberSaveable { mutableStateOf(false) }
    val share = { state.shareAsPng(context, transparentBackground = transparentExport) }

    var initialized by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!initialized) {
            state.strokeColor = EzColors.Primary
            state.strokeWidthPx = 6f
            state.drawingName = "Untitled"
            initialized = true
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 150.dp,
        sheetContainerColor = EzColors.Surface,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = EzColors.Surface,
        sheetContent = {
            Column(
                Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
            ) {
                EzToolbar(
                    state = state,
                    controls = controls,
                    enabledTools = tools,
                    palette = BrushSwatches,
                    backgroundPalette = BackgroundSwatches,
                    onExport = share,
                )
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            EzCanvas(state, Modifier.fillMaxSize())

            TopBar(
                title = state.drawingName,
                showShare = ToolbarControl.Export in controls,
                onRename = { renaming = true },
                onExamples = onOpenExamples,
                onShare = share,
                onConfigure = { showConfig = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .fillMaxWidth(),
            )
        }
    }

    if (showConfig) {
        ConfigureToolsSheet(
            tools = tools,
            controls = controls,
            transparentExport = transparentExport,
            onToggleTool = { tool -> tools = if (tool in tools) tools - tool else tools + tool },
            onToggleControl = { control -> controls = if (control in controls) controls - control else controls + control },
            onToggleTransparentExport = { transparentExport = !transparentExport },
            onDismiss = { showConfig = false },
        )
    }

    if (renaming) {
        RenameDialog(
            current = state.drawingName,
            onConfirm = { state.drawingName = it },
            onDismiss = { renaming = false },
        )
    }
}

/**
 * The demo renames from its own top bar, so this is app UI. An app that does not want to build
 * one can enable `ToolbarControl.Rename` and get the library's own version instead.
 */
@Composable
private fun RenameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val confirm = {
        onConfirm(text.trim().ifBlank { "Untitled" })
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = EzColors.Surface,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(
                    "Drawing name",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = EzColors.Ink,
                )
                Spacer(Modifier.height(16.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = EzColors.Ink),
                    cursorBrush = SolidColor(EzColors.Primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    decorationBox = { innerField ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(EzColors.ChipBg)
                                .border(1.dp, EzColors.Divider, RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 15.dp),
                        ) {
                            if (text.isEmpty()) {
                                Text("Name", style = MaterialTheme.typography.bodyLarge, color = EzColors.SectionLabel)
                            }
                            innerField()
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Exports as ${text.trim().ifBlank { "drawing" }}.png",
                    style = MaterialTheme.typography.labelSmall,
                    color = EzColors.Subtle,
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, EzColors.Divider, RoundedCornerShape(12.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) { Text("Cancel", style = MaterialTheme.typography.labelLarge, color = EzColors.Ink) }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(EzColors.Ink)
                            .clickable { confirm() }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) { Text("Save", style = MaterialTheme.typography.labelLarge, color = EzColors.Surface) }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    showShare: Boolean,
    onRename: () -> Unit,
    onExamples: () -> Unit,
    onShare: () -> Unit,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .shadow(6.dp, RoundedCornerShape(14.dp), clip = false)
                .clip(RoundedCornerShape(14.dp))
                .background(EzColors.Surface)
                .clickable { onRename() }
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(EzColors.Ink),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "EZ",
                    color = EzColors.Surface,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                )
            }
            Text(
                title,
                color = EzColors.Ink,
                style = MaterialTheme.typography.titleSmall,
            )
            Icon(
                Icons.Filled.Edit,
                "Rename drawing",
                tint = EzColors.Muted,
                modifier = Modifier.size(14.dp).padding(end = 2.dp),
            )
            Spacer(Modifier.width(2.dp))
        }

        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircleButton(Icons.Filled.GridView, "Examples", EzColors.Surface, EzColors.Ink, onExamples)
            if (showShare) {
                CircleButton(Icons.Filled.IosShare, "Share", EzColors.Surface, EzColors.Ink, onShare)
            }
            CircleButton(Icons.Filled.Tune, "Configure tools", EzColors.Ink, EzColors.Surface, onConfigure)
        }
    }
}

@Composable
private fun CircleButton(icon: ImageVector, desc: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .shadow(4.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = fg, modifier = Modifier.size(18.dp))
    }
}
