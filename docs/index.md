# EZCanvas

A configurable, exportable drawing canvas for Android, built entirely with Jetpack Compose. Drop in a canvas and a complete toolbar, switch on the features you want, and you have a working drawing surface. The same library is a two button signature pad or a full painting app.

<video src="vids/EZCanvas-Explainer.mp4" controls width="600"></video>

## Documentation

- **[Full documentation](documentation.md)**: concepts, a step by step integration, every composable and state property, export, rotation, the public API reference, and a troubleshooting FAQ.
- **[Design decisions](decisions.md)**: why the eraser is an element rather than white paint, why fills persist as a recipe, how rotation behaves, and what can still break.
- **[Project README](https://github.com/RotemBar18/EZCanvas)**: screenshots, project structure, and the feature list.

## Install

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories { maven { url = uri("https://jitpack.io") } }
}

// app/build.gradle.kts
dependencies {
    implementation("com.github.RotemBar18:EZCanvas:1.1.0")
}
```

## The whole integration

A state object, a canvas, and a toolbar bound to the same state. The toolbar is supplied by the library, dialogs included, so there is nothing to design or wire by hand.

```kotlin
val state = rememberEzCanvasState()

Column {
    EzCanvas(state, Modifier.weight(1f).fillMaxWidth())
    EzToolbar(state)
}
```

Choose what your users get by passing sets. Anything you leave out is simply not there.

```kotlin
// A signature pad: one pen, clear, export. That is the whole interface.
EzToolbar(
    state,
    enabledTools = setOf(Tool.Pen),
    controls = setOf(ToolbarControl.Clear, ToolbarControl.Export),
)

// A painting studio: every tool, every control.
EzToolbar(state)
```

Export is a single call. It writes a named PNG and opens the share sheet, optionally with a transparent background so a signature composites onto a document with nothing behind it.

```kotlin
state.shareAsPng(context)
state.shareAsPng(context, transparentBackground = true)
```

See the [full documentation](documentation.md) for everything else.

## Requirements

Android 9 (minSdk 28) and above. No keys, no accounts, and no network.
