# EZCanvas — Feature Specification (living document)

> Project: **EZCanvas** — a fully customizable, exportable drawing-canvas library for Android, with a configurable built-in toolbar.
> Built with **Jetpack Compose** (single Activity, no XML layouts).

## Scope decision (locked)

- **v1 target = Phase 1 + Phase 2.** This is what we commit to building and demoing.
- **Phase 3–5 = documented stretch / future work.** Kept in the idea document for ambition, narrated in the final video if not completed.
- **Cut line:** at the end of Phase 2 the library is complete, polished, and demoable on its own.

## Platform & API style

- Built with **Jetpack Compose**; single Activity, no XML layouts.
- UI exposed as composables: `EzCanvas(state, modifier)` and `EzToolbar(state, controls)`.
- State hoisted via `rememberEzCanvasState()` (Compose state-holder pattern); restoration via `rememberSaveable`.
- Library depends only on Compose — no DI, no networking.

## Architectural commitment

Every drawn item (stroke, and later shape/text) is a **serializable element** held in an ordered list (per layer in the future). This single model unifies:

- Undo / redo (push/pop elements)
- Save / restore + JSON serialization
- Export (raster now; SVG later by serializing elements)
- Layers (later: ordered lists of elements)

Display caches committed elements (e.g. a `Picture` / `ImageBitmap`) for performance; the active stroke is drawn live each frame in a Compose `Canvas` via `pointerInput`, without recomposition.

---

## Phase 1 — Core (must ship)

### Core drawing & strokes
| Option | Description | Type / values | Default |
|---|---|---|---|
| `isDrawingEnabled` | Master input switch | Boolean | true |
| `strokeColor` | Stroke color | Color | black |
| `strokeWidth` | Thickness (dp) | Float | 4dp |
| `strokeOpacity` | Stroke alpha | Float 0–1 | 1.0 |
| `smoothing` | Bézier smoothing | Boolean | true |
| `smoothingFactor` | Smoothing strength | Float 0–1 | 0.5 |
| `antiAlias` | Edge anti-aliasing | Boolean | true |
| `lineCap` / `lineJoin` | End/corner style | Round/Butt/Square | Round |

### Brushes / tools
| Option | Description | Type / values | Default |
|---|---|---|---|
| `tool` | Active tool | Pen / Eraser | Pen |
| Pen | Opaque stroke | — | — |
| `enabledTools` | Whitelist of tools to expose | Set<Tool> | all |

### Eraser
| Option | Description | Type / values | Default |
|---|---|---|---|
| `eraserMode` | Pixel vs whole-stroke | Enum | Pixel |
| `eraserWidth` | Eraser size | Float | 20dp |

### Canvas & background
| Option | Description | Type / values | Default |
|---|---|---|---|
| `backgroundColor` | Canvas fill | Color | white |
| `backgroundImage` | Draw over a bitmap/drawable/uri | Bitmap/Uri | none |
| `transparentBackground` | Export without bg | Boolean | false |
| `canvasSize` | Match view vs fixed | Enum/Size | MatchView |

### History (undo / redo)
| Option | Description | Type / values | Default |
|---|---|---|---|
| `undo()` / `redo()` | Step history | method | — |
| `canUndo` / `canRedo` | Button state | Boolean | — |
| `clear()` | Wipe (undoable) | method | — |
| `maxHistorySize` | Memory cap | Int | 50 |

### Export
| Option | Description | Type / values | Default |
|---|---|---|---|
| `exportBitmap()` | Render to Bitmap | method | — |
| `exportToFile()` | PNG to file | method + format | PNG |

### State & persistence
| Option | Description | Type / values | Default |
|---|---|---|---|
| Rotation/state restore | Strokes survive recreation | auto | on |
| `isEmpty` | Anything drawn? | Boolean | — |

### Callbacks / listeners
| Option | Description | Type / values | Default |
|---|---|---|---|
| `onDrawingChanged` | Any change | listener | — |
| `onUndoStateChanged` | canUndo/canRedo changed | listener | — |

### Built-in toolbar (configurable) — key differentiator
The dev adds an `EzToolbar` composable that shares the canvas's hoisted state, and chooses which controls appear.

| Option | Description | Type / values | Default |
|---|---|---|---|
| `EzToolbar` | Ready-made bar sharing the canvas state | Composable | — |
| shared `EzCanvasState` | Toolbar & canvas use one hoisted state | state | — |
| `toolbarControls` | Which controls appear | Set of: ColorPicker, StrokeWidth, Opacity, ToolSelector, Undo, Redo, Clear, BackgroundPicker, Export | sensible default set |
| `toolbarOrientation` | Layout direction | Horizontal/Vertical | Horizontal |
| `toolbarPosition` | Placement hint | Top/Bottom | Bottom |
| `showToolbar` | Show/hide at runtime | Boolean | true |
| Custom action slot | Dev adds own button(s) | slot/API | — |

### Theming & appearance
| Option | Description | Default |
|---|---|---|
| Dark mode | Respect day/night | on |
| RTL (toolbar) | Mirror UI | on |

### Accessibility
| Option | Description | Type / values | Default |
|---|---|---|---|
| Content descriptions | Tools/buttons labeled | auto | on |
| `announceActions` | TalkBack speaks "Undo/Cleared" etc. | Boolean (toggle) | true |

### Input
| Option | Description | Type / values | Default |
|---|---|---|---|
| `inputMode` | Finger / Stylus-only / Both | Enum | Both |
| Disable parent scroll while drawing | Gesture-conflict fix | Boolean | true |

### Performance (automatic)
| Option | Description | Default |
|---|---|---|
| Committed-stroke bitmap caching | Don't redraw all paths each frame | on |
| Off-main-thread export | Avoid jank on big exports | on |
| `maxCanvasResolution` | Cap bitmap to avoid OOM | device-based |

---

## Phase 2 — Easy wins (committed for v1)

| Feature | Description |
|---|---|
| Marker / Highlighter brush | Translucent, multiply blend |
| Neon / Glow brush | Soft glow stroke (BlurMaskFilter) |
| Calligraphy brush | Angle-based width |
| `velocityWidth` | Stroke width reacts to draw speed |
| `pressureWidth` | Stroke width reacts to stylus pressure |
| Eyedropper | Pick color from canvas pixel |
| Export JPEG / WebP + `exportQuality` | More formats + compression |
| `exportScale` | Output resolution multiplier |
| JSON `serialize()` / `restore()` | Save/load drawing as data |

---

## Phase 3–5 — Stretch / future work (documented, not committed)

| Phase | Features | Risk |
|---|---|---|
| P3 — Shapes & text 