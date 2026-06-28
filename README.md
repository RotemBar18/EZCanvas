# EZCanvas

![Platform](https://img.shields.io/badge/Android-minSdk%2028-3DDC84?logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.06-4285F4?logo=jetpackcompose&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)

A configurable, exportable drawing canvas for Android, built entirely with Jetpack Compose. Drop in a canvas and a complete toolbar, switch on the features you want, and you have a working drawing surface. The same library can be a two button signature pad or a full painting app.

```kotlin
val state = rememberEzCanvasState()
Column {
    EzCanvas(state, Modifier.weight(1f).fillMaxWidth())
    EzToolbar(state)   // a complete toolbar: tools, colors, sliders, shapes, export
}
```

## Why it exists

Apps keep rebuilding the same drawing surface for signature capture, screenshot markup, sketching, whiteboards, and kids' doodling. The hard parts get rewritten every time: smooth touch handling, undo and redo, brushes, flood fill, bitmap export, and surviving rotation. EZCanvas does them once behind a small API, and lets you switch features on or off so the same component fits a minimal pad and a full art tool.

## Features

- **Headless Logic Engine**: The state is completely decoupled from the UI. Use our ready-made toolbar or build a 100% custom interface using the `EzCanvasState` API.
- **Brushes**: pen, marker, neon glow, and calligraphy
- **Eraser** that clears strokes without touching the background
- **Shapes**: line, square, and circle, where squares and circles lock to 1:1
- **Line styles**: solid, dotted, dashed, and dash-dot, on the pen and every shape
- **Paint bucket** that flood fills the enclosed area under your finger
- **Backgrounds**: a solid color, a grid, dots or lined pattern, or your own photo to draw over
- **History**: undo, redo, and clear, with a capped stroke history
- **Export and share**: one call writes a PNG and opens the share sheet, plus raw `Bitmap` export
- **Rotation safe**: the drawing and the settings survive configuration changes
- **A complete `EzToolbar`** that follows your app's `MaterialTheme`

## Install (JitPack)

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

// module build.gradle.kts
implementation("com.github.RotemBar18.EZCanvas:ezcanvas:<tag>")
```

## Configure what your users get

`EzToolbar` renders only the tools and controls you enable, so the toolbar is the product. You never design or wire one by hand.

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
| `Tool` | `PEN`, `MARKER`, `NEON`, `CALLIGRAPHY`, `ERASER`, `LINE`, `SQUARE`, `CIRCLE`, `BUCKET` |
| `ToolbarControl` | `ToolSelector`, `ColorPicker`, `StrokeWidth`, `Opacity`, `EraserSize`, `Style`, `Background`, `Pattern`, `Image`, `Undo`, `Redo`, `Clear`, `Export` |

## Or drive it yourself

The state is hoisted and observable. Skip `EzToolbar` and build your own UI against it.

```kotlin
state.tool = Tool.MARKER
state.strokeColor = Color.Red
state.strokeWidthPx = 12f
state.lineStyle = LineStyle.Dashed
state.undo()
state.redo()
state.clear()
val canExport = !state.isEmpty
```

## Export, share, and image backgrounds

One call renders the drawing and opens the system share sheet. You set up no `FileProvider`, no bitmap I/O, and no image decoding.

```kotlin
state.shareAsPng(context)                 // render, then open the share sheet
val bitmap = state.exportBitmap()         // raw Bitmap, or null before layout
val uri = state.exportPngToCache(context) // shareable Uri via the bundled FileProvider

// A ready made photo picker that sets the chosen image as the canvas background:
val pickBackground = rememberBackgroundImagePicker(state)
Button(onClick = pickBackground) { Text("Background image") }
```

## The demo app

The `:app` module is a showcase. Each example is a real product built in about ten lines.

| Example | What the developer enabled |
|---|---|
| **Signature Pad** | Pen only, clear, export |
| **Kids Doodle** | Fat brushes, bucket fill, big color swatches |
| **Painting Studio** | Everything, with `EzToolbar(state)` |
| **Photo Markup** | Shapes and marker over an uploaded photo, dashed callouts |
| **Classroom Whiteboard** | Markers and shapes on a grid, undo and redo |
| **Drawing Game** | One pen, a few colors, fast |
| **Neon Art** | Glow brush on a dark canvas |

## Architecture

**One serializable element model.** Every drawn item is a `CanvasElement`, either a `StrokeElement`, a `ShapeElement`, or a `FillElement`, held in one ordered list. That single model powers undo and redo, export, and rotation restore. Adding a new element type never touches that machinery.

**Clean module split.** `:ezcanvas` holds the engine and the toolbar. `:app` only consumes the public API, so the drawing internals never leak into app code.

**Theme aware.** `EzToolbar` is built from `MaterialTheme`, so it adopts your app's colors.

```
:ezcanvas   EzCanvas, EzToolbar, rememberEzCanvasState(), the element model, export and share
:app        a configurable canvas screen and a gallery of example products
```

## Requirements

Jetpack Compose, Kotlin 2.2, AGP 9.1, `compileSdk 36`, `minSdk 28`.

## Roadmap

- Stroke width that reacts to drawing speed and stylus pressure
- Save and restore drawings as JSON
- Export with a transparent background
- Select and move shapes after drawing
- Palm rejection

## License

Released under the MIT License. See [LICENSE](LICENSE).
