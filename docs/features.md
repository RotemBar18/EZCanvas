# EZCanvas API Reference

EZCanvas is a configurable drawing canvas for Android, built with Jetpack Compose. This document covers every public type, property and function, and shows how to use them.

**Contents**

[Install](#install) | [Quick start](#quick-start) | [Composables](#composables) | [EzCanvasState](#ezcanvasstate) | [Enums](#enums) | [Tools](#tools) | [Text](#text) | [Toolbar controls](#toolbar-controls) | [Colours](#colours) | [Backgrounds](#backgrounds) | [Export and sharing](#export-and-sharing) | [Your own UI](#your-own-ui) | [Theming](#theming) | [Rotation and persistence](#rotation-and-persistence) | [Architecture](#architecture) | [Limits](#limits)

## Install

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

// module build.gradle.kts
implementation("com.github.RotemBar18:EZCanvas:1.0.1")
```

Requires Jetpack Compose, Kotlin 2.2, AGP 9.1, `compileSdk 36`, `minSdk 28`. The library depends only on Compose and Material 3. No DI, no networking.

## Quick start

There are three levels of integration. Pick the one that fits.

**1. Canvas only.** The defaults are already a black pen on white, so this alone is a working signature pad.

```kotlin
val state = rememberEzCanvasState()
EzCanvas(state, Modifier.fillMaxSize())
```

**2. Canvas and a toolbar.** One extra line gives you the full control surface.

```kotlin
val state = rememberEzCanvasState()
Column {
    EzCanvas(state, Modifier.weight(1f).fillMaxWidth())
    EzToolbar(state)
}
```

**3. Canvas and a trimmed toolbar.** Choose exactly what your users get.

```kotlin
EzToolbar(
    state,
    enabledTools = setOf(Tool.PEN, Tool.ERASER),
    controls = setOf(ToolbarControl.ColorPicker, ToolbarControl.Undo, ToolbarControl.Clear),
)
```

## Composables

### `rememberEzCanvasState()`

Creates the state that drives the canvas and the toolbar. It survives configuration changes, so the drawing is still there after a rotation.

```kotlin
val state = rememberEzCanvasState()
```

Set the starting tool, colour, size, background and name here rather than assigning properties afterwards.

```kotlin
val state = rememberEzCanvasState(
    tool = Tool.NEON,
    strokeColor = Color(0xFF06B6D4),
    strokeWidthPx = 12f,
    backgroundColor = Color(0xFF0F172A),
    drawingName = "neon-art",
)
```

These are starting values, applied only when the state is first created. After a rotation the saved state wins, so a name the user typed or a colour they picked is kept. That is the reason to use them: assigning the same properties from a `LaunchedEffect` looks equivalent, but that block runs again when the activity is recreated and overwrites the user's choices.

### `EzCanvas(state, modifier)`

The drawing surface. It takes the size its modifier gives it.

```kotlin
EzCanvas(state, Modifier.weight(1f).fillMaxWidth())      // fill the remaining space
EzCanvas(state, Modifier.fillMaxWidth().height(200.dp))  // a fixed signature strip

Surface(
    modifier = Modifier.fillMaxWidth().height(360.dp).padding(16.dp),
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, Color.LightGray),
) {
    EzCanvas(state, Modifier.fillMaxSize())
}
```

### `EzToolbar(...)`

A complete toolbar. It renders only what you enable, and every control brings its own UI, including the dialogs for text and renaming.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `state` | `EzCanvasState` | required | The same state you gave `EzCanvas` |
| `modifier` | `Modifier` | `Modifier` | Layout modifier |
| `controls` | `Set<ToolbarControl>` | `DefaultToolbarControls` | Which controls appear |
| `enabledTools` | `Set<Tool>` | all tools | Which tools appear in the selector |
| `palette` | `List<Color>` | `DefaultSwatches` | Stroke colour swatches |
| `backgroundPalette` | `List<Color>` | `DefaultBackgrounds` | Background colour swatches |
| `allowCustomColor` | `Boolean` | `true` | Adds a palette button that opens a full colour chooser |
| `onExport` | `(() -> Unit)?` | `null` | Overrides the export button, which otherwise shares a PNG |

The toolbar lays its sections out in a `Column` with no internal scrolling, so put it in a scrollable container or a bottom sheet when you enable many controls.

```kotlin
Box(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
    EzToolbar(state)
}
```

### `rememberBackgroundImagePicker(state)`

Returns a callback that opens the system photo picker and sets the chosen image as the canvas background. It decodes off the main thread, so your app never touches a content Uri or a Bitmap.

```kotlin
val pickPhoto = rememberBackgroundImagePicker(state)
Button(onClick = pickPhoto) { Text("Choose a photo") }
```

## `EzCanvasState`

Everything the toolbar does goes through these members, so anything the toolbar can do, your own UI can do too.

### Drawing settings

| Property | Type | Default | Description |
|---|---|---|---|
| `tool` | `Tool` | `PEN` | The active tool |
| `strokeColor` | `Color` | Black | Colour of the next stroke, or of the selected text |
| `strokeWidthPx` | `Float` | `10f` | Stroke thickness in pixels, and the text size source |
| `strokeAlpha` | `Float` | `1f` | Opacity, 0 to 1 |
| `eraserWidthPx` | `Float` | `40f` | Eraser size in pixels |
| `lineStyle` | `LineStyle` | `Solid` | Dash style for the pen and every shape |
| `smoothing` | `Boolean` | `true` | Curves through the touch points. Set to false for exact point to point strokes. A developer level setting with no toolbar control, because turning it off only makes freehand look worse |
| `drawingName` | `String` | `"drawing"` | Names the drawing and the exported file |

### Canvas and background

| Property | Type | Default | Description |
|---|---|---|---|
| `backgroundColor` | `Color` | White | Canvas fill |
| `backgroundPattern` | `BackgroundPattern` | `None` | Grid, dots or ruled lines over the fill |
| `backgroundImage` | `ImageBitmap?` | `null` | An image drawn under the strokes |

### History and status

| Member | Type | Description |
|---|---|---|
| `undo()` | function | Step back one element |
| `redo()` | function | Step forward one element |
| `clear()` | function | Remove everything. A single `undo()` brings it all back |
| `clearSelection()` | function | Drop the current text selection |
| `canUndo` | `Boolean` | Whether there is anything to undo |
| `canRedo` | `Boolean` | Whether there is anything to redo |
| `isEmpty` | `Boolean` | Whether anything has been drawn |
| `hasSelection` | `Boolean` | Whether a piece of text is selected |
| `maxUndoSteps` | `Int` | How many steps back `undo()` can go, unlimited by default |

**Limiting undo.** `maxUndoSteps` decides how far back the user can step. It never removes anything from the drawing: older elements simply become permanent.

```kotlin
state.maxUndoSteps = 1   // a single step back, for a form field
state.maxUndoSteps = 10  // a short history
                         // leave it alone for unlimited
```

### Export

| Function | Returns | Description |
|---|---|---|
| `exportBitmap(transparentBackground)` | `Bitmap?` | Renders the drawing, or null before the canvas is laid out |
| `exportPngToCache(context, fileName, transparentBackground)` | `Uri?` | Writes a PNG and returns a shareable Uri |
| `shareAsPng(context, chooserTitle, fileName, transparentBackground)` | `Unit` | Renders a PNG and opens the system share sheet |
| `loadBackgroundImageFromUri(context, uri)` | `Boolean` | Suspending. Decodes an image and sets it as the background |

## Enums

| Enum | Values |
|---|---|
| `Tool` | `PEN`, `MARKER`, `NEON`, `CALLIGRAPHY`, `ERASER`, `LINE`, `SQUARE`, `CIRCLE`, `BUCKET`, `TEXT` |
| `LineStyle` | `Solid`, `Dotted`, `Dashed`, `DashDot` |
| `BackgroundPattern` | `None`, `Grid`, `Dots`, `Lined` |
| `ShapeKind` | `Line`, `Square`, `Circle` |
| `ToolbarControl` | see [Toolbar controls](#toolbar-controls) |

Helpers: `Tool.isShape` is true for the shape tools, and `Tool.shapeKind()` returns the matching `ShapeKind` or null.

## Tools

| Tool | Behaviour |
|---|---|
| `PEN` | Opaque round stroke. Honours `lineStyle` |
| `MARKER` | Translucent flat stroke |
| `NEON` | Glowing stroke, a soft halo behind a bright core |
| `CALLIGRAPHY` | Thicker flat stroke |
| `ERASER` | Clears strokes and leaves the background intact |
| `LINE` | Straight line between press and release |
| `SQUARE` | Perfect square, locked to 1:1 |
| `CIRCLE` | Perfect circle, locked to 1:1 |
| `BUCKET` | Flood fills the enclosed region under the tap |
| `TEXT` | Places, selects and moves text |

Drag to draw. Tap to place a dot with any brush, or to erase a spot with the eraser. Shapes rubber band between press and release, and a drag too small to see is discarded rather than committed as an invisible mark. Shapes are outlines, so use `BUCKET` to fill one.

## Text

With `TEXT` selected:

- Tapping empty canvas opens a dialog and places what you type.
- Tapping existing text selects it. The toolbar then shows that text's own colour, opacity and size.
- Changing colour, opacity or size while text is selected edits the text instead of the next stroke.
- Dragging selected text moves it.

The font size comes from `strokeWidthPx`, so the text tool needs no extra controls.

```kotlin
EzToolbar(
    state,
    enabledTools = setOf(Tool.PEN, Tool.CIRCLE, Tool.TEXT),
    controls = setOf(
        ToolbarControl.ToolSelector, ToolbarControl.ColorPicker,
        ToolbarControl.StrokeWidth, ToolbarControl.Undo, ToolbarControl.Clear,
    ),
)
```

Use `state.hasSelection` to react to a selection, and `state.clearSelection()` to drop it. Selecting is limited to text: strokes and shapes cannot be moved after they are drawn.

## Toolbar controls

| Control | What it shows |
|---|---|
| `ToolSelector` | A row of the enabled tools |
| `ColorPicker` | Stroke colour swatches, plus the colour chooser |
| `StrokeWidth` | Size slider |
| `Opacity` | Opacity slider |
| `EraserSize` | Eraser size slider |
| `Style` | Line style samples, shown for the pen and the shape tools |
| `Background` | Background colour swatches |
| `Pattern` | Grid, dots and ruled line samples |
| `Image` | Upload and remove a background image |
| `Rename` | A tappable name field that edits `drawingName` |
| `Undo`, `Redo`, `Clear` | History buttons |
| `Export` | Shares a PNG, or runs your `onExport` |

`DefaultToolbarControls` is every control.

```kotlin
// A signature pad
EzToolbar(state, enabledTools = setOf(Tool.PEN), controls = setOf(ToolbarControl.Clear, ToolbarControl.Export))

// A shape editor with no freehand at all
EzToolbar(
    state,
    enabledTools = setOf(Tool.LINE, Tool.SQUARE, Tool.CIRCLE, Tool.BUCKET),
    controls = setOf(
        ToolbarControl.ToolSelector, ToolbarControl.ColorPicker,
        ToolbarControl.Style, ToolbarControl.Undo, ToolbarControl.Clear,
    ),
)
```

## Colours

Pass your own swatches for strokes and backgrounds. `DefaultSwatches` and `DefaultBackgrounds` are the built in lists.

```kotlin
EzToolbar(
    state,
    palette = listOf(Color(0xFF4F46E5), Color(0xFFFB6F61), Color(0xFF14B8A6)),
    backgroundPalette = listOf(Color.White, Color(0xFFFFFDE7), Color(0xFF0F172A)),
)
```

By default a palette button after the swatches opens a chooser with a full spectrum of hues, shades and greys. Turn it off to hold users to exactly the colours you supply, which suits brand locked or child friendly apps.

```kotlin
EzToolbar(
    state,
    palette = listOf(Color.Black, Color(0xFF2563EB), Color(0xFFDC2626)),
    allowCustomColor = false,
)
```

## Backgrounds

```kotlin
state.backgroundColor = Color(0xFF0B2A4A)
state.backgroundPattern = BackgroundPattern.Grid
```

For a photo, use the picker and size the canvas to the image so nothing is distorted.

```kotlin
val pickPhoto = rememberBackgroundImagePicker(state)
val photo = state.backgroundImage

if (photo == null) {
    Button(onClick = pickPhoto) { Text("Choose a photo") }
} else {
    Box(Modifier.fillMaxWidth().aspectRatio(photo.width.toFloat() / photo.height.toFloat())) {
        EzCanvas(state, Modifier.fillMaxSize())
    }
}
```

Setting `backgroundImage = null` returns to the background colour.

## Export and sharing

One call renders the drawing and opens the system share sheet. You set up no `FileProvider`, no bitmap I/O and no image decoding, because the library ships its own provider.

```kotlin
state.drawingName = "signature-order-4821"
state.shareAsPng(context)                  // uses drawingName
state.shareAsPng(context, fileName = "export-$timestamp")  // override for one call

val bitmap = state.exportBitmap()          // raw Bitmap, or null before layout
val uri = state.exportPngToCache(context)  // shareable Uri
```

File names are sanitised and get a `.png` extension when missing, so passing text a user typed is safe.

**The developer decides whether users can rename.** Set the name in code and leave `Rename` out of `controls` for a fixed name, or include it to let users edit it.

```kotlin
// Fixed name, no rename UI. Suits a signature pad.
state.drawingName = "signature-order-$orderId"
EzToolbar(state, controls = setOf(ToolbarControl.Clear, ToolbarControl.Export))
```

Exports contain the background, the strokes, the shapes, the fills and the text, flattened into one image. The selection outline is a screen affordance and never appears in an export.

Pass `transparentBackground = true` to leave out the background colour, image and pattern, so only what was drawn is exported. A signature then drops onto a document with nothing behind it.

```kotlin
state.shareAsPng(context, transparentBackground = true)
val cutout = state.exportBitmap(transparentBackground = true)
```

Export is PNG only. That is deliberate: PNG is lossless and supports transparency, while JPEG is lossy and leaves visible ringing artefacts around the sharp high contrast edges that a pen stroke is made of.

## Your own UI

`EzToolbar` chooses **which** controls appear, through `controls`, `enabledTools`, `palette` and `allowCustomColor`. It does not expose its own sizing, spacing or button shapes, and it always follows the host `MaterialTheme`. When you need a different look, for example oversized buttons for a children's app, do not try to restyle the bar. Skip it and build the UI yourself.

```kotlin
Row {
    IconButton(onClick = { state.tool = Tool.PEN }) { Icon(Icons.Filled.Edit, "Pen") }
    IconButton(onClick = { state.tool = Tool.ERASER }) { Icon(Icons.Filled.Clear, "Eraser") }
    IconButton(onClick = { state.undo() }, enabled = state.canUndo) {
        Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
    }
    Slider(
        value = state.strokeWidthPx,
        onValueChange = { state.strokeWidthPx = it },
        valueRange = 2f..60f,
    )
}
```

Everything the toolbar does is available this way, and nothing about it is private to the library.

| What you want | What you set or call |
|---|---|
| Pick a tool | `state.tool = Tool.PEN` |
| Pick a colour | `state.strokeColor = Color.Red` |
| Brush and eraser size | `state.strokeWidthPx`, `state.eraserWidthPx` |
| Opacity | `state.strokeAlpha` |
| Dash style | `state.lineStyle = LineStyle.Dashed` |
| Background | `state.backgroundColor`, `state.backgroundPattern`, `state.backgroundImage` |
| Photo picker | `rememberBackgroundImagePicker(state)` |
| History | `state.undo()`, `state.redo()`, `state.clear()` |
| Button enabled states | `state.canUndo`, `state.canRedo`, `state.isEmpty` |
| Text selection | `state.hasSelection`, `state.clearSelection()` |
| Name and export | `state.drawingName`, `state.shareAsPng(context)` |

The text dialog still appears when a user taps with `TEXT` selected, because it belongs to the canvas rather than the toolbar.

## Theming

`EzToolbar` and its dialogs take every colour from `MaterialTheme`, so they match your app without configuration. Wrapping the bar in a different scheme is all it takes to restyle it.

```kotlin
MaterialTheme(
    colorScheme = darkColorScheme(
        primary = Color(0xFF06B6D4),
        surface = Color(0xFF151C2E),
        onSurface = Color(0xFFE7EAF3),
    ),
) {
    EzToolbar(state, enabledTools = setOf(Tool.NEON))
}
```

The toolbar uses `primary` for the active tool and selected values, `surfaceVariant` for inactive controls, and `secondary` for the confirm button in dialogs.

## Rotation and persistence

The drawing and the settings survive configuration changes and process recreation through `rememberSaveable`. No work is needed on your side.

When the canvas changes shape, for example on rotation, the drawing keeps its real size and stroke widths and is shifted so its centre matches the new canvas centre. Nothing is scaled or distorted. A drawing larger than the new canvas therefore runs past the edge, and rotating back brings it into view.

Bucket fills survive too, by a different route. A filled region has no compact geometric form, so its pixels would be megabytes, far past the size limit on saved state. Only the recipe is stored, the point that was tapped and the colour, and the fill is replayed once the canvas has a size again. Each one is replayed against the elements below it, so a stroke drawn after the fill does not change the region it covered.

The background image is the one thing that is not restored, because it is a photo rather than something the library can recompute. Choose it again, or keep its `Uri` in your own state and reload it.

## Architecture

Every drawn item is a `CanvasElement` held in one ordered list:

| Type | What it holds |
|---|---|
| `StrokeElement` | Freehand points, the brush that made them, colour, width, alpha, style |
| `ShapeElement` | A kind, a start and an end point |
| `TextElement` | The text, its top left, size, colour and alpha |
| `FillElement` | A bucket fill: the seed and colour that made it, plus the pixels once rendered |

That single model powers undo and redo, export and rotation restore, so adding a new element type never touches that machinery. Freehand points are `StrokePoint(x, y)` in canvas pixels.

The library is one module, `:ezcanvas`, holding the canvas, the toolbar, the element model, flood fill, export and sharing.

## Limits

Stated plainly, so nothing surprises you in production. [decisions.md](decisions.md) explains the reasoning behind each one.

- Selection and dragging apply to text only. Strokes and shapes cannot be moved after they are drawn.
- `backgroundImage` is stretched to fill the canvas. Size the canvas to the image's aspect ratio when that matters.
- The background image is not restored after rotation. Bucket fills are, by replaying them.
- Flood fill runs on the calling thread, which is a brief pause on a very large canvas.
- A dashed or open outline does not contain a bucket fill, in the same way as any paint program.
- Memory grows with the drawing, and unevenly. A stroke of 200 points costs a few KB, so a thousand strokes is a handful of MB. A bucket fill stores pixels, so a large one can reach several MB on its own. Many large fills on a big screen are the realistic way to run out of memory. There is deliberately no cap on how much a user may draw, because any such cap has to delete their work to take effect.
