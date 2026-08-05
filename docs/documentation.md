# EZCanvas Documentation

A configurable, exportable drawing canvas for Android, built entirely with Jetpack Compose. This document covers the concepts, a step by step integration, every public type and function, the behaviour you can rely on, and the limits.

For the reasoning behind the design, see [decisions.md](decisions.md).

## Contents

1. [What EZCanvas is](#1-what-ezcanvas-is)
2. [Core concepts](#2-core-concepts)
3. [Install](#3-install)
4. [Quick start](#4-quick-start)
5. [Integrate step by step](#5-integrate-step-by-step)
6. [Tools](#6-tools)
7. [Text](#7-text)
8. [Toolbar controls](#8-toolbar-controls)
9. [Colours and palettes](#9-colours-and-palettes)
10. [Backgrounds](#10-backgrounds)
11. [Export and sharing](#11-export-and-sharing)
12. [Rotation and persistence](#12-rotation-and-persistence)
13. [Building your own UI](#13-building-your-own-ui)
14. [Theming](#14-theming)
15. [Public API reference](#15-public-api-reference)
16. [Behaviour you can rely on](#16-behaviour-you-can-rely-on)
17. [Limits and known ceilings](#17-limits-and-known-ceilings)
18. [Troubleshooting and FAQ](#18-troubleshooting-and-faq)

## 1. What EZCanvas is

Apps keep rebuilding the same drawing surface for signature capture, screenshot markup, sketching, whiteboards, and children's doodling. The hard parts get rewritten every time: smooth touch handling, undo and redo, brushes, flood fill, bitmap export, and surviving rotation.

EZCanvas does them once, behind a small API, and lets you switch features on or off so the same component fits a minimal signature pad and a full art tool.

Two composables and one state object are the entire surface area:

```kotlin
val state = rememberEzCanvasState()

Column {
    EzCanvas(state, Modifier.weight(1f).fillMaxWidth())
    EzToolbar(state)
}
```

The toolbar is part of the library rather than something you build. It renders exactly the tools and controls you enable, and every control brings its own UI, including the dialogs for entering text and renaming a drawing.

## 2. Core concepts

Three ideas explain almost everything else.

**One state object.** `EzCanvasState` holds the settings and the drawing. The canvas and the toolbar are separate composables that share it, and nothing else connects them. That is why a signature pad can use the canvas with no toolbar at all, and why an app that wants a different look can build its own interface against the same state.

**One ordered list of elements.** Every drawn item is a `CanvasElement`: a `StrokeElement`, a `ShapeElement`, a `TextElement`, or a `FillElement`. They live in one list, drawn bottom to top. That single list is why undo and redo, export, and rotation restore all work without separate machinery. Erasing is an element too, which is why undo un-erases with no extra code.

**Configuration, not styling.** `EzToolbar` takes sets: which tools, which controls, which colours. It exposes no spacing or shape parameters and takes every colour from `MaterialTheme`. When you need a genuinely different look, skip the bar and build your own against the state.

## 3. Install

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

// module build.gradle.kts
implementation("com.github.RotemBar18:EZCanvas:1.1.0")
```

Requires Jetpack Compose, Kotlin 2.2, AGP 9.1, `compileSdk 36`, `minSdk 28`. The library depends only on Compose and Material 3. No dependency injection, no networking, no keys, no accounts.

## 4. Quick start

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
    enabledTools = setOf(Tool.Pen, Tool.Eraser),
    controls = setOf(ToolbarControl.ColorPicker, ToolbarControl.Undo, ToolbarControl.Clear),
)
```

## 5. Integrate step by step

### Step 1: Create the state

```kotlin
val state = rememberEzCanvasState()
```

It survives configuration changes, so the drawing is still there after a rotation.

Set the starting tool, colour, size, background and name here rather than assigning properties afterwards:

```kotlin
val state = rememberEzCanvasState(
    tool = Tool.Neon,
    strokeColor = Color(0xFF06B6D4),
    strokeWidthPx = 12f,
    backgroundColor = Color(0xFF0F172A),
    drawingName = "neon-art",
)
```

These are starting values, applied only when the state is first created. After a rotation the saved state wins, so a name the user typed or a colour they picked is kept. That is the reason to use them: assigning the same properties from a `LaunchedEffect` looks equivalent, but that block runs again when the activity is recreated and overwrites the user's choices.

### Step 2: Place the canvas

`EzCanvas` takes the size its modifier gives it.

```kotlin
EzCanvas(state, Modifier.weight(1f).fillMaxWidth())      // fill the remaining space
EzCanvas(state, Modifier.fillMaxWidth().height(200.dp))  // a fixed signature strip
```

It composes like any other composable, so decorate it however you like:

```kotlin
Surface(
    modifier = Modifier.fillMaxWidth().height(360.dp).padding(16.dp),
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, Color.LightGray),
) {
    EzCanvas(state, Modifier.fillMaxSize())
}
```

### Step 3: Add the toolbar

```kotlin
EzToolbar(state)
```

The bar fits the space it is given, so it needs no scrolling wrapper around it.

With every control enabled its sections stack to roughly 700dp. A phone screen is around 870dp tall in portrait and 390dp in landscape, so an unbounded bar would leave little or nothing to draw on. Two things prevent that. It caps itself at `maxHeight` and scrolls inside that cap. And on a screen shorter than 500dp, which in practice means a landscape phone or a small split screen window, it switches to a single horizontally scrolling row about 90dp tall.

That means a plain `Column` behaves on a phone in both orientations, with nothing to tune.

### Step 4: Choose what ships

```kotlin
// A signature pad: one pen, clear, export. That is the whole interface.
EzToolbar(
    state,
    enabledTools = setOf(Tool.Pen),
    controls = setOf(ToolbarControl.Clear, ToolbarControl.Export),
)

// A shape editor with no freehand at all.
EzToolbar(
    state,
    enabledTools = setOf(Tool.Line, Tool.Square, Tool.Circle, Tool.Bucket),
    controls = setOf(
        ToolbarControl.ToolSelector, ToolbarControl.ColorPicker,
        ToolbarControl.Style, ToolbarControl.Undo, ToolbarControl.Clear,
    ),
)

// Everything.
EzToolbar(state)
```

### Step 5: Get the drawing out

```kotlin
state.shareAsPng(context)                                  // named PNG, share sheet
state.shareAsPng(context, transparentBackground = true)    // no background behind it
val bitmap = state.exportBitmap()                          // raw Bitmap, or null before layout
```

### Full minimal example

```kotlin
@Composable
fun SignatureField(orderId: String, onSigned: (Uri) -> Unit) {
    val context = LocalContext.current
    val state = rememberEzCanvasState(
        strokeWidthPx = 5f,
        drawingName = "signature-$orderId",
    )

    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Sign below")

        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .border(1.dp, Color.LightGray, RoundedCornerShape(14.dp)),
        ) {
            EzCanvas(state, Modifier.fillMaxSize())
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { state.clear() }, enabled = !state.isEmpty) {
                Text("Clear")
            }
            Button(
                onClick = {
                    state.exportPngToCache(context, transparentBackground = true)?.let(onSigned)
                },
                enabled = !state.isEmpty,
            ) {
                Text("Confirm")
            }
        }
    }
}
```

No toolbar, no configuration, and the app drives clear and confirm through the state.

## 6. Tools

| Tool | Behaviour |
|---|---|
| `Pen` | Opaque round stroke. Honours `lineStyle` |
| `Marker` | Translucent flat stroke, so overlapping strokes build up |
| `Neon` | Glowing stroke, a soft halo behind a bright core |
| `Calligraphy` | Thicker flat stroke |
| `Eraser` | Clears strokes and leaves the background intact |
| `Line` | Straight line between press and release |
| `Square` | Perfect square, locked to 1:1 |
| `Circle` | Perfect circle, locked to 1:1 |
| `Bucket` | Flood fills the enclosed region under the tap |
| `Text` | Places, selects and moves text |

Drag to draw. Tap to place a dot with any brush, or to erase a spot with the eraser. Shapes rubber band between press and release, and a drag too small to see is discarded rather than committed as an invisible mark. Shapes are outlines, so use `Bucket` to fill one.

Helpers: `Tool.isShape` is true for the shape tools, and `Tool.shapeKind` returns the matching `ShapeKind` or null.

## 7. Text

With `Text` selected:

- Tapping empty canvas opens a dialog and places what you type.
- Tapping existing text selects it. The toolbar then shows that text's own colour, opacity and size.
- Changing colour, opacity or size while text is selected edits the text instead of the next stroke.
- Dragging selected text moves it.

The font size comes from `strokeWidthPx`, so the text tool needs no extra controls.

```kotlin
EzToolbar(
    state,
    enabledTools = setOf(Tool.Pen, Tool.Circle, Tool.Text),
    controls = setOf(
        ToolbarControl.ToolSelector, ToolbarControl.ColorPicker,
        ToolbarControl.StrokeWidth, ToolbarControl.Undo, ToolbarControl.Clear,
    ),
)
```

Use `state.hasSelection` to react to a selection, and `state.clearSelection()` to drop it. Selecting is limited to text: strokes and shapes cannot be moved after they are drawn.

## 8. Toolbar controls

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

## 9. Colours and palettes

Pass your own swatches for strokes and backgrounds. `DefaultPalette` and `DefaultBackgroundPalette` are the built in lists.

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

## 10. Backgrounds

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

`rememberBackgroundImagePicker` decodes off the main thread, so your app never touches a content `Uri` or a `Bitmap`. Setting `backgroundImage = null` returns to the background colour.

## 11. Export and sharing

One call renders the drawing and opens the system share sheet. You set up no `FileProvider`, no bitmap input or output and no image decoding, because the library ships its own provider.

```kotlin
state.drawingName = "signature-order-4821"
state.shareAsPng(context)                                  // uses drawingName
state.shareAsPng(context, fileName = "export-$timestamp")  // override for one call

val bitmap = state.exportBitmap()          // raw Bitmap, or null before layout
val uri = state.exportPngToCache(context)  // shareable Uri
```

File names are sanitised and get a `.png` extension when missing, so passing text a user typed is safe.

**The developer decides whether users can rename.** Set the name in code and leave `Rename` out of `controls` for a fixed name, or include it to let users edit it.

Exports contain the background, the strokes, the shapes, the fills and the text, flattened into one image. The selection outline is a screen affordance and never appears in an export.

Pass `transparentBackground = true` to leave out the background colour, image and pattern, so only what was drawn is exported. A signature then drops onto a document with nothing behind it.

```kotlin
state.shareAsPng(context, transparentBackground = true)
val cutout = state.exportBitmap(transparentBackground = true)
```

Export is PNG only. That is deliberate: PNG is lossless and supports transparency, while JPEG is lossy and leaves visible ringing artefacts around the sharp high contrast edges that a pen stroke is made of.

## 12. Rotation and persistence

The drawing and the settings survive configuration changes and process recreation through `rememberSaveable`. No work is needed on your side.

When the canvas changes shape, the drawing keeps its real size and stroke widths and is shifted so its centre matches the new canvas centre. Nothing is scaled or distorted. The shift is then clamped so the drawing stays in view: one that fits the new canvas is kept fully inside it, and one too large to fit is kept covering it. Rotating back restores the original framing.

A layout pass that leaves the canvas with no width or height is ignored rather than treated as a resize. A `Column` gives a weighted child no space at all once its siblings stop fitting, which a cramped landscape layout can do, and acting on that would move the drawing and then leave it there.

Bucket fills survive too, by a different route. A filled region has no compact geometric form, so its pixels would be megabytes, far past the size limit on saved state. Only the recipe is stored, the point that was tapped and the colour, and the fill is replayed once the canvas has a size again. Each one is replayed against the elements below it, so a stroke drawn after the fill does not change the region it covered.

The background image is the one thing that is not restored, because it is a photo rather than something the library can recompute. Choose it again, or keep its `Uri` in your own state and reload it.

## 13. Building your own UI

`EzToolbar` chooses **which** controls appear. It does not expose its own sizing, spacing or button shapes, and it always follows the host `MaterialTheme`. When you need a different look, for example oversized buttons for a children's app, do not try to restyle the bar. Skip it and build the UI yourself.

```kotlin
Row {
    IconButton(onClick = { state.tool = Tool.Pen }) { Icon(Icons.Filled.Edit, "Pen") }
    IconButton(onClick = { state.tool = Tool.Eraser }) { Icon(Icons.Filled.Clear, "Eraser") }
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
| Pick a tool | `state.tool = Tool.Pen` |
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

The text dialog still appears when a user taps with `Text` selected, because it belongs to the canvas rather than the toolbar.

## 14. Theming

`EzToolbar` and its dialogs take every colour from `MaterialTheme`, so they match your app without configuration. Wrapping the bar in a different scheme is all it takes to restyle it.

```kotlin
MaterialTheme(
    colorScheme = darkColorScheme(
        primary = Color(0xFF06B6D4),
        surface = Color(0xFF151C2E),
        onSurface = Color(0xFFE7EAF3),
    ),
) {
    EzToolbar(state, enabledTools = setOf(Tool.Neon))
}
```

The toolbar uses `primary` for the active tool and selected values, `surfaceVariant` for inactive controls, and `secondary` for the confirm button in dialogs.

## 15. Public API reference

### Composables

| Composable | Description |
|---|---|
| `rememberEzCanvasState(...)` | Creates the state that drives the canvas and the toolbar |
| `EzCanvas(state, modifier)` | The drawing surface |
| `EzToolbar(...)` | The complete, configurable toolbar |
| `rememberBackgroundImagePicker(state)` | Returns a callback that opens the photo picker |

### `rememberEzCanvasState` parameters

| Parameter | Type | Default |
|---|---|---|
| `tool` | `Tool` | `Pen` |
| `strokeColor` | `Color` | Black |
| `strokeWidthPx` | `Float` | `10f` |
| `strokeAlpha` | `Float` | `1f` |
| `eraserWidthPx` | `Float` | `40f` |
| `lineStyle` | `LineStyle` | `Solid` |
| `backgroundColor` | `Color` | White |
| `backgroundPattern` | `BackgroundPattern` | `None` |
| `drawingName` | `String` | `"drawing"` |

### `EzToolbar` parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `state` | `EzCanvasState` | required | The same state you gave `EzCanvas` |
| `modifier` | `Modifier` | `Modifier` | Layout modifier |
| `controls` | `Set<ToolbarControl>` | `DefaultToolbarControls` | Which controls appear |
| `enabledTools` | `Set<Tool>` | all tools | Which tools appear in the selector |
| `palette` | `List<Color>` | `DefaultPalette` | Stroke colour swatches |
| `backgroundPalette` | `List<Color>` | `DefaultBackgroundPalette` | Background colour swatches |
| `allowCustomColor` | `Boolean` | `true` | Adds a palette button that opens a full colour chooser |
| `onExport` | `(() -> Unit)?` | `null` | Overrides the export button, which otherwise shares a PNG |
| `maxHeight` | `Dp` | `300.dp` | How tall the stacked layout may grow before it scrolls inside itself |

### `EzCanvasState` drawing settings

| Property | Type | Default | Description |
|---|---|---|---|
| `tool` | `Tool` | `Pen` | The active tool |
| `strokeColor` | `Color` | Black | Colour of the next stroke, or of the selected text |
| `strokeWidthPx` | `Float` | `10f` | Stroke thickness in pixels, and the text size source |
| `strokeAlpha` | `Float` | `1f` | Opacity, 0 to 1 |
| `eraserWidthPx` | `Float` | `40f` | Eraser size in pixels |
| `lineStyle` | `LineStyle` | `Solid` | Dash style for the pen and every shape |
| `smoothing` | `Boolean` | `true` | Curves through the touch points. A developer level setting with no toolbar control, because turning it off only makes freehand look worse |
| `drawingName` | `String` | `"drawing"` | Names the drawing and the exported file |

### `EzCanvasState` canvas and background

| Property | Type | Default | Description |
|---|---|---|---|
| `backgroundColor` | `Color` | White | Canvas fill |
| `backgroundPattern` | `BackgroundPattern` | `None` | Grid, dots or ruled lines over the fill |
| `backgroundImage` | `ImageBitmap?` | `null` | An image drawn under the strokes |

### `EzCanvasState` history and status

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

`maxUndoSteps` decides how far back the user can step. It never removes anything from the drawing: older elements simply become permanent.

```kotlin
state.maxUndoSteps = 1   // a single step back, for a form field
state.maxUndoSteps = 10  // a short history
                         // leave it alone for unlimited
```

### Export functions

| Function | Returns | Description |
|---|---|---|
| `exportBitmap(transparentBackground)` | `Bitmap?` | Renders the drawing, or null before the canvas is laid out |
| `exportPngToCache(context, fileName, transparentBackground)` | `Uri?` | Writes a PNG and returns a shareable Uri |
| `shareAsPng(context, chooserTitle, fileName, transparentBackground)` | `Unit` | Renders a PNG and opens the system share sheet |
| `loadBackgroundImageFromUri(context, uri)` | `Boolean` | Suspending. Decodes an image and sets it as the background |

### Enums

| Enum | Values |
|---|---|
| `Tool` | `Pen`, `Marker`, `Neon`, `Calligraphy`, `Eraser`, `Line`, `Square`, `Circle`, `Bucket`, `Text` |
| `LineStyle` | `Solid`, `Dotted`, `Dashed`, `DashDot` |
| `BackgroundPattern` | `None`, `Grid`, `Dots`, `Lined` |
| `ShapeKind` | `Line`, `Square`, `Circle` |
| `ToolbarControl` | `ToolSelector`, `ColorPicker`, `StrokeWidth`, `Opacity`, `EraserSize`, `Style`, `Background`, `Pattern`, `Image`, `Rename`, `Undo`, `Redo`, `Clear`, `Export` |

### Element model

Every drawn item is a `CanvasElement` held in one ordered list.

| Type | What it holds |
|---|---|
| `StrokeElement` | Freehand points, the brush that made them, colour, width, alpha, style |
| `ShapeElement` | A kind, a start and an end point |
| `TextElement` | The text, its top left, size, colour and alpha |
| `FillElement` | A bucket fill: the seed and colour that made it, plus the pixels once rendered |

Freehand points are `StrokePoint(x, y)` in canvas pixels.

### Packages

The library is one module, `:ezcanvas`, in four layers that depend in one direction only.

| Package | Holds | Depends on |
|---|---|---|
| `model` | The element types and the enums | nothing |
| `render` | Turning elements into pixels, on screen and into a `Bitmap` | `model` |
| `ui` | The dialog widgets | nothing |
| `com.ezcanvas` | The public API: canvas, toolbar, state, fill, export, sharing | all of the above |

Everything in `render` and `ui` is internal, so the compiler enforces the direction rather than a convention. `render` is also why the screen and an exported PNG match: Compose cannot draw into a `Bitmap` outside a composition, so there are two renderers by necessity, but every paint decision they share, dash intervals, stroke caps, marker translucency and the background pattern geometry, is defined once in `render/StrokeStyles.kt` and read by both.

## 16. Behaviour you can rely on

| Case | Behaviour |
|---|---|
| A tap rather than a drag | Places a dot, or erases a spot. `detectDragGestures` alone ignores a tap, because it only fires past the touch slop threshold |
| A drag too small to see, with a shape tool | Discarded, so a stray tap does not leave an invisible shape in the drawing |
| Dragging a square or circle backwards | Anchors at the press point and grows 1:1 in the drag direction, rather than collapsing |
| Bucket tapped on an area already that colour | Nothing happens, instead of committing an invisible element |
| Bucket inside a dashed or open outline | Leaks, in the same way as any paint program |
| Fill replayed onto a region that no longer exists | Stays pending and draws nothing, rather than flooding the wrong area |
| Text tapped with the text tool | Selects it. Tapping empty canvas starts a new one |
| Selecting text while the brush is a different colour | Reads the text's own colour into the toolbar, and does not repaint the text |
| Switching tool while text is selected | Drops the selection, because it only means something for the text tool |
| Undo after a clear | Restores the whole drawing in one step. Drawing again makes the clear permanent |
| Rotating with a drawing larger than the new canvas | Stays at full size and keeps covering the canvas, so it is never shifted out of sight |
| A layout pass that leaves the canvas no height | Ignored, and not recorded as the canvas size |
| Restoring saved state written by an older version | Starts fresh, rather than reading rows that have a different shape |
| A file name typed by a user | Sanitised, path separators stripped, `.png` appended |
| Exporting before the canvas has been laid out | Returns null rather than crashing |

## 17. Limits and known ceilings

Stated plainly, so nothing surprises you in production. [decisions.md](decisions.md) explains the reasoning behind each one.

- Selection and dragging apply to text only. Strokes and shapes cannot be moved after they are drawn.
- `backgroundImage` is stretched to fill the canvas. Size the canvas to the image's aspect ratio when that matters.
- The background image is not restored after rotation. Bucket fills are, by replaying them.
- Flood fill and export run on the calling thread, which is a brief pause on a very large canvas.
- A dashed or open outline does not contain a bucket fill.
- There is no cap on canvas resolution.
- Memory grows with the drawing, and unevenly. A stroke of 200 points costs a few KB, so a thousand strokes is a handful of MB. A bucket fill stores pixels, so a large one can reach several MB on its own. Many large fills on a big screen are the realistic way to run out of memory. There is deliberately no cap on how much a user may draw, because any such cap has to delete their work to take effect.

## 18. Troubleshooting and FAQ

**My drawing disappears when I rotate to landscape.**
Almost always the canvas has no room rather than the drawing being lost. Check what else is in the `Column`. A weighted canvas gets whatever its siblings leave, and a header, a toolbar and a fixed height button can add up to more than a landscape phone has. Give the canvas a minimum height, or move content out of the way in landscape.

**`exportBitmap()` returns null.**
The canvas has not been laid out yet, so it has no size to render at. Export after the first composition, typically from a button rather than during composition.

**The bucket fill leaked across the whole canvas.**
The region was not closed. A dashed line, or an outline with a gap, does not contain a fill, exactly as in any paint program. Draw with `LineStyle.Solid`, or close the shape.

**Can I move a stroke after drawing it?**
No. Selection is text only. Hit testing a stroke means distance to a path rather than a bounding box, which is a different problem. Use undo, or draw on.

**Can I export JPEG or WebP?**
No, PNG only. JPEG is lossy and produces visible ringing around the sharp, high contrast edges that pen strokes are made of, and it cannot carry the transparency that `transparentBackground` exists for.

**My background photo is gone after rotation.**
That one is not restored, because it is a user's photo rather than something the library can recompute. Keep the `Uri` in your own state and call `loadBackgroundImageFromUri` again.

**How do I stop users choosing their own colours?**
Pass your `palette` and set `allowCustomColor = false`.

**How do I stop users renaming the drawing?**
Set `state.drawingName` in code and leave `ToolbarControl.Rename` out of `controls`.

**How do I limit undo to one step?**
`state.maxUndoSteps = 1`. It limits how far back a user can step and never deletes anything from the drawing.

**The toolbar does not match my app.**
It takes every colour from `MaterialTheme`. Wrap it in the scheme you want. If you need different sizing or shapes, that is deliberately not configurable: build your own UI against the state instead, as in [section 13](#13-building-your-own-ui).

**The toolbar is too tall, or scrolls when I did not expect it to.**
It caps itself at `maxHeight`, 300dp by default, and scrolls inside that. Raise the cap, or enable fewer controls.
