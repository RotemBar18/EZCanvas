# EZCanvas

![Platform](https://img.shields.io/badge/Android-minSdk%2028-3DDC84?logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.06-4285F4?logo=jetpackcompose&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)

A configurable, exportable **drawing-canvas library for Android**, built entirely with **Jetpack Compose**. Drop in a canvas and a complete toolbar, switch on the features you want, and you have a working drawing surface — a two-button signature pad or a full painting app, from the same library.

```kotlin
val state = rememberEzCanvasState()
Column {
    EzCanvas(state, Modifier.weight(1f).fillMaxWidth())
    EzToolbar(state)   // a complete toolbar: tools, colors, sliders, shapes, export…
}
```

---

## Why it exists

Apps keep rebuilding the same drawing surface — signature capture, screenshot markup, sketching, whiteboards, kids' doodling. The hard parts get rewritten every time: smooth touch handling, undo/redo, brushes, flood fill, bitmap export, surviving rotation. EZCanvas does them once, behind a small API, and lets you **enable or disable features** so the same component fits a minimal pad and a full art tool.

## Features

- **Brushes** — pen, marker / highlighter, neon glow, calligraphy
- **Eraser** — clears strokes without touching the background
- **Shapes** — line, square, circle (squares and circles are constrained 1:1)
- **Line styles** — solid · dotted · dashed · dash-dot, on the pen and every shape
- **Paint bucket** — flood-fills the enclosed region under your finger
- **Backgrounds** — solid color, grid / dots / lined patterns, or your own image to draw over
- **History** — undo · redo · clear, with a capped stroke history
- **Export & share** — one call renders a PNG and opens the share sheet; raw `Bitmap` export too
- **Rotation-safe** — the drawing and settings survive configuration changes
- **A complete, configurable `EzToolbar`** that follows your app's `MaterialTheme`

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

`EzToolbar` renders **only** the tools and controls you enable, so the toolbar is the product — you don't design or wire one by hand.

```kotlin
// A signature pad: one pen, clear, export. That's the whole UI.
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

The state is hoisted and observable — skip `EzToolbar` entirely and build your own UI against it.

```kotlin
state.tool = Tool.MARKER
state.strokeColor = Color.Red
state.strokeWidthPx = 12f
state.lineStyle = LineStyle.Dashed
state.undo(); state.redo(); state.clear()
val canExport = !state.isEmpty
```

## Export, share & image backgrounds

```kotlin
state.shareAsPng(context)                 // render + open the system share sheet
val bitmap = state.exportBitmap()         // raw Bitmap (or null before layout)
val uri = state.exportPngToCache(context) // shareable Uri via the library's bundled FileProvider

// Ready-made photo picker that sets the chosen image as the canvas background:
val pickBackground = rememberBackgroundImagePicker(state)
Button(onClick = pickBackground) { Text("Background image") }
```

No `FileProvider` setup, no bitmap I/O, no image decoding on your side.

## The demo app

The `:app` module is a showcase — each example is a real product built in ~10 lines:

| Example | What the developer enabled |
|---|---|
| **Signature Pad** | Pen only · clear · export |
| **Kids Doodle** | Fat brushes, bucket fill, big color swatches |
| **Painting Studio** | Everything — `EzToolbar(state)` |
| **Photo Markup** | Shapes + marker over an uploaded photo, dashed callouts |
| **Classroom Whiteboard** | Markers + shapes on a grid, undo/redo |
| **Drawing Game** | One pen, a few colors, fast |
| **Neon Art** | Glow brush on a dark canvas |

## Architecture

- **One serializable element model.** Every drawn item is a `CanvasElement` — `StrokeElement`, `ShapeElement`, or `FillElement` — held in a single ordered list. That one model powers undo/redo, export, and rotation restore; adding an element type never touches that machinery.
- **Clean module split.** `:ezcanvas` holds the engine *and* the toolbar; `:app` only consumes the public API. The drawing internals (pointer handling, rendering, flood fill, bitmap export) never leak into app code.
- **Theme-aware.** `EzToolbar` is built from `MaterialTheme`, so it adopts the host app's colors automatically.

```
:ezcanvas   EzCanvas · EzToolbar · rememberEzCanvasState() · model · export/share
:app        demo: a configurable canvas screen + a gallery of example products
```

## Requirements

Jetpack Compose · Kotlin 2.2 · AGP 9.1 · `compileSdk 36` · `minSdk 28`.

## Roadmap

Velocity / pressure-based stroke width · JSON `serialize()` / `restore()` · transparent-background export · shape select & move · palm rejection.

## License

[MIT](LICENSE).
