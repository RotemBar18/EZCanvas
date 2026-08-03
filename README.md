# EZCanvas

[![JitPack](https://jitpack.io/v/RotemBar18/EZCanvas.svg)](https://jitpack.io/#RotemBar18/EZCanvas)
![Platform](https://img.shields.io/badge/Android-minSdk%2028-3DDC84?logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.06-4285F4?logo=jetpackcompose&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)

A configurable, exportable drawing canvas for Android, built entirely with Jetpack Compose. Drop in a canvas and a complete toolbar, switch on the features you want, and you have a working drawing surface. The same library can be a two button signature pad or a full painting app.

```kotlin
val state = rememberEzCanvasState()
Column {
    EzCanvas(state, Modifier.weight(1f).fillMaxWidth())
    EzToolbar(state)   // a complete toolbar: tools, colors, sliders, shapes, text, export
}
```

## Screenshots

| Canvas | Configure tools | Examples |
|---|---|---|
| ![Canvas](docs/images/canvas.png) | ![Configure](docs/images/configure.png) | ![Examples](docs/images/examples.png) |

| Signature Pad | Photo Markup | Neon Art |
|---|---|---|
| ![Signature](docs/images/signature.png) | ![Markup](docs/images/markup.png) | ![Neon](docs/images/neon.png) |

## Run the demo

```bash
git clone https://github.com/RotemBar18/EZCanvas.git
cd EZCanvas
./gradlew :app:installDebug     # build and install on a connected device or emulator
```

Or open the folder in Android Studio and press Run. The demo needs no keys, no accounts and no network.

To build the APK without installing:

```bash
./gradlew :app:assembleDebug    # output: app/build/outputs/apk/debug/app-debug.apk
```

To run the library's unit tests:

```bash
./gradlew :ezcanvas:testDebugUnitTest
```

## Project structure

```
EZCanvas/
├── ezcanvas/                     the library, published to JitPack
│   └── src/main/java/com/ezcanvas/
│       ├── EzCanvas.kt           the drawing surface, gestures and rendering
│       ├── EzCanvasState.kt      hoisted state, history, save and restore
│       ├── EzToolbar.kt          the configurable toolbar
│       ├── Dialogs.kt            the text and rename dialogs
│       ├── Fill.kt               scanline flood fill
│       ├── Export.kt             Bitmap and PNG rendering
│       ├── Sharing.kt            share sheet and photo picker
│       └── model/Models.kt       the element model and enums
└── app/                          the demo
    └── src/main/java/com/ezcanvas/demo/
        ├── PlaygroundScreen.kt   the configurable canvas screen
        ├── GalleryScreen.kt      the example gallery
        ├── ConfigureToolsSheet.kt  toggles what the toolbar shows
        └── examples/             one file per example integration
```

## Why it exists

Apps keep rebuilding the same drawing surface for signature capture, screenshot markup, sketching, whiteboards, and kids' doodling. The hard parts get rewritten every time: smooth touch handling, undo and redo, brushes, flood fill, bitmap export, and surviving rotation. EZCanvas does them once behind a small API, and lets you switch features on or off so the same component fits a minimal pad and a full art tool.

## Features

- **Headless logic engine**: the drawing state is fully decoupled from the UI. Use the included `EzToolbar`, or build a completely custom interface on the `EzCanvasState` API.
- **Brushes**: pen, marker, neon glow, and calligraphy
- **Eraser** that clears strokes without touching the background
- **Shapes**: line, square, and circle, where squares and circles lock to 1:1
- **Line styles**: solid, dotted, dashed, and dash-dot, on the pen and every shape
- **Paint bucket** that flood fills the enclosed area under your finger
- **Text** you can place, select, drag, and restyle from the toolbar
- **Backgrounds**: a solid color, a grid, dots or lined pattern, or your own photo to draw over
- **Color chooser** with a full spectrum, or lock users to exactly the swatches you supply
- **History**: undo and redo with no limit by default, a developer settable cap, and an undoable clear
- **Export and share**: one call writes a named PNG and opens the share sheet, optionally with a transparent background, plus raw `Bitmap` export
- **Rotation safe**: strokes, shapes, text and fills all survive, keeping their size and staying centered
- **Every control brings its own UI**, including the text and rename dialogs

## Install (JitPack)

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

// module build.gradle.kts
implementation("com.github.RotemBar18.EZCanvas:ezcanvas:v1.0.0")
```

## Configure what your users get

`EzToolbar` renders only the tools and controls you enable, so the toolbar is the product. You never design or wire one by hand, and controls that need a dialog bring their own.

```kotlin
// A signature pad: one pen, clear, export. That is the whole UI.
EzToolbar(
    state,
    enabledTools = setOf(Tool.PEN),
    controls = setOf(ToolbarControl.Clear, ToolbarControl.Export),
)

// A painting studio: every tool, every control.
EzToolbar(state)
```

| Type | Values |
|---|---|
| `Tool` | `PEN`, `MARKER`, `NEON`, `CALLIGRAPHY`, `ERASER`, `LINE`, `SQUARE`, `CIRCLE`, `BUCKET`, `TEXT` |
| `ToolbarControl` | `ToolSelector`, `ColorPicker`, `StrokeWidth`, `Opacity`, `EraserSize`, `Style`, `Background`, `Pattern`, `Image`, `Rename`, `Undo`, `Redo`, `Clear`, `Export` |

## Drawing

Drag to draw. Tap to place a dot with any brush, or to erase a spot with the eraser. Shapes rubber band between the point you press and the point you release, and squares and circles stay 1:1.

With `TEXT` selected, tapping empty canvas asks for the text and places it. Tapping existing text selects it, and dragging moves it. While text is selected the toolbar shows that text's own color, opacity and size, and changing any of them edits the text instead of the next stroke.

## Make it your own

Size the canvas with a modifier, the same as any composable.

```kotlin
EzCanvas(state, Modifier.weight(1f).fillMaxWidth())        // fill the space
EzCanvas(state, Modifier.fillMaxWidth().height(200.dp))    // a fixed signature strip
```

Pass your own swatches, and the toolbar picks up your `MaterialTheme` colors on its own. Set `allowCustomColor` to false to hold users to exactly the colors you supply.

```kotlin
EzToolbar(
    state,
    palette = listOf(Color(0xFF4F46E5), Color(0xFFFB6F61), Color(0xFF14B8A6)),
    backgroundPalette = listOf(Color.White, Color(0xFFFFFDE7), Color(0xFF0F172A)),
    allowCustomColor = false,
)
```

Set up the canvas by assigning state properties.

```kotlin
state.backgroundColor = Color(0xFF0F172A)
state.backgroundPattern = BackgroundPattern.Grid
state.tool = Tool.NEON
state.strokeColor = Color(0xFF06B6D4)
state.strokeWidthPx = 12f
state.lineStyle = LineStyle.Dashed
```

Wrapping the toolbar in a different theme is enough to restyle it, because it takes every color from `MaterialTheme`.

```kotlin
MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF06B6D4))) {
    EzToolbar(state, enabledTools = setOf(Tool.NEON))
}
```

## Or drive it yourself

The state is hoisted and observable. Skip `EzToolbar` and build your own UI against it.

```kotlin
IconButton(onClick = { state.tool = Tool.ERASER }) { Icon(Icons.Filled.Clear, "Eraser") }
IconButton(onClick = { state.undo() }, enabled = state.canUndo) {
    Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
}
Slider(
    value = state.strokeWidthPx,
    onValueChange = { state.strokeWidthPx = it },
    valueRange = 2f..60f,
)
```

## Export, share, and image backgrounds

One call renders the drawing and opens the system share sheet. You set up no `FileProvider`, no bitmap I/O, and no image decoding. Exports are named after `state.drawingName`, which you can set in code or let users edit with the `Rename` control.

```kotlin
state.drawingName = "signature-order-4821"
state.shareAsPng(context)                 // render, then open the share sheet
val bitmap = state.exportBitmap()         // raw Bitmap, or null before layout
val uri = state.exportPngToCache(context) // shareable Uri via the bundled FileProvider

// A ready made photo picker that sets the chosen image as the canvas background:
val pickBackground = rememberBackgroundImagePicker(state)
Button(onClick = pickBackground) { Text("Background image") }
```

## The demo app

The `:app` module is a showcase. Each example is a real app screen rather than a bare canvas, and each one is about ten lines of library code.

| Example | The integration it shows |
|---|---|
| **Signature Pad** | A canvas as one field inside a delivery form, with no toolbar at all |
| **Photo Markup** | Pick a photo from the device, then annotate it with shapes and labels |
| **Drawing Game** | A timed round with one pen and four colors, so nothing slows the player down |
| **Classroom Whiteboard** | Markers and shapes on a grid, with undo and redo for live teaching |
| **Kids Doodle** | Three controls and a locked palette, so there is nothing to get wrong |
| **Painting Studio** | Everything switched on, with `EzToolbar(state)` |
| **Neon Art** | The same toolbar wrapped in a dark theme |

## Architecture

**One serializable element model.** Every drawn item is a `CanvasElement`: a `StrokeElement`, a `ShapeElement`, a `TextElement`, or a `FillElement`, held in one ordered list. That single model powers undo and redo, export, and rotation restore. Adding a new element type never touches that machinery.

**Clean module split.** `:ezcanvas` holds the engine and the toolbar. `:app` only consumes the public API, so the drawing internals never leak into app code.

**Theme aware.** `EzToolbar` and its dialogs are built from `MaterialTheme`, so they adopt your app's colors.

```
:ezcanvas   EzCanvas, EzToolbar, rememberEzCanvasState(), the element model, export and share
:app        a configurable canvas screen and a gallery of example products
```

## Requirements

Jetpack Compose, Kotlin 2.2, AGP 9.1, `compileSdk 36`, `minSdk 28`.

## Full documentation

- [docs/features.md](docs/features.md) documents every property, function and enum, with usage examples for sizing, theming, custom UI and export.
- [docs/decisions.md](docs/decisions.md) covers why the library is built this way, what was considered instead, the edge cases it handles, and the ceilings it still has.
