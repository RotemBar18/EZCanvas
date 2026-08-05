# Design decisions

Why EZCanvas is built the way it is. [features.md](features.md) covers what the API does; this
covers why, what was considered instead, and where the edges are.

## Scope

| In scope | Deliberately out |
|---|---|
| Freehand brushes, eraser, shapes, line styles, flood fill, text | Layers |
| A configurable toolbar that renders what you enable | Zoom and pan |
| Undo, redo, an undoable clear | Selecting or moving strokes and shapes |
| PNG export and share, with optional transparency | JPEG, WebP, SVG |
| Backgrounds: colour, pattern, photo | Cloud sync or any networking |
| Surviving rotation | A document or file model |

The line is drawn at the drawing surface. Naming, storing and organising drawings belongs to the
app, because only the app knows what a drawing is for. The library holds one name, and only
because the export needs a file name.

## Decisions

### One element model

Every drawn item is a `CanvasElement` in a single ordered list: a stroke, a shape, text, or a fill.

*Considered instead:* separate lists per type, or a bitmap the canvas paints into.

A single list means undo, redo, export and rotation restore are written once and work for every
type. Adding text late in the project touched the model and the two renderers, and none of the
history or export code. A bitmap canvas would have made undo impossible without storing a snapshot
per action.

### The canvas and the toolbar are separate composables

They share one hoisted `EzCanvasState` rather than the toolbar living inside the canvas.

This is what lets a signature pad use the canvas with no toolbar at all, and lets a children's app
build its own controls. It also means the library never dictates layout: the developer decides
whether the bar sits in a bottom sheet, a side panel, or nowhere.

### The toolbar is configured, not styled

`EzToolbar` takes sets: which tools, which controls, which colours. It exposes no spacing or shape
parameters, and takes every colour from `MaterialTheme`. The one size it accepts is `maxHeight`,
which is not a matter of taste: it decides how much of the screen is left to draw on.

*Considered instead:* style parameters for button size, corner radius and so on.

That road has no end, and a half configurable component is worse than a fixed one. The escape hatch
is better: the state is public, so an app that needs a different look builds its own UI against it
and skips the bar. Theming still works, because wrapping the bar in a different `MaterialTheme` is
enough to restyle it.

### The toolbar fits itself to the screen

With every control enabled the bar's sections stack to roughly 700dp. A phone is about 870dp tall in
portrait and 390dp in landscape, so a bar that simply took its natural height would leave a sliver of
canvas in portrait and nothing at all in landscape. It therefore caps itself at `maxHeight` and
scrolls inside that, and on a screen shorter than 500dp it lays its sections out in one horizontal
scrolling row about 90dp tall instead of stacking them.

*Considered instead:* documenting the height and letting each app wrap the bar in a bounded scrolling
box.

That is what the library did first, and every example ended up with the same
`heightIn(max = ...).verticalScroll(...)` around the bar. A workaround that every caller has to write
identically is a missing feature. It also failed quietly: the wrapper made the bar scroll, but the
canvas above it was still squeezed to almost nothing in landscape, so a drawing that survived
rotation correctly still looked like it had vanished.

### The eraser is an element, not white paint

Committed elements are drawn into an isolated layer, and the eraser is stored as a normal element
that clears pixels in that layer with `BlendMode.Clear`.

*Considered instead:* drawing over strokes with the background colour.

Painting white would look correct until the background changed or a photo was loaded, then the
erased area would show as a white smear. Because the eraser is an element like any other, undo
un-erases with no extra code.

### Fills are pixels, but they persist as a recipe

A flood fill has no compact geometric description, so the filled region is baked into a bitmap. That
bitmap is far too large for saved state, where the limit is around one megabyte, so only the seed
point and the colour are saved and the fill is replayed after layout.

*Considered instead:* saving the bitmap, which would throw `TransactionTooLargeException` on
rotation, or dropping fills on rotation, which is what the library did before and which users read
as losing their work.

Each fill is replayed against only the elements below it, so a stroke drawn after the fill does not
change the region it originally covered.

### Text colour is applied at draw time

`TextMeasurer` caches layout results, and colour does not affect layout. Passing colour inside the
measured style meant a recoloured text kept drawing with the cached paint, so colour changes did
nothing while size changes worked.

Text is measured for layout only, and colour and alpha are passed as arguments to `drawText`.

### Rotation translates, it does not scale

Coordinates are stored in canvas pixels. When the canvas changes shape the drawing is shifted so its
centre matches the new centre, and it is not resized.

*Considered instead:* scaling to fit the new canvas.

Scaling changes stroke widths and shrinks the user's work to fit a shape they did not choose.
Silently shrinking someone's drawing is worse than showing part of it.

Centring alone is not enough. Rotating into landscape can make the canvas far shorter than the
drawing is tall, and a plain centring shift then shunts the whole thing out of sight. So the shift
is clamped: a drawing that fits the new canvas is kept fully inside it, and a drawing too large to
fit is kept covering it. Either way something is always on screen.

### Undo is capped by steps, never by deleting

`maxUndoSteps` limits how far back the user can go. Elements outside that window become permanent
and stay on the canvas.

An earlier version capped the number of elements and removed the oldest one when the cap was passed,
which silently deleted part of the drawing. Trading a rare out of memory error for guaranteed data
loss is the wrong trade. There is no cap on how much a user may draw, because any such cap has to
delete their work to take effect. The correct memory lever is bounding the size of each allocation,
which is listed under future work.

### PNG only

*Considered instead:* JPEG and WebP, which were in the original plan.

PNG is lossless and supports transparency. JPEG is lossy and produces visible ringing around sharp
high contrast edges, which is exactly what a pen stroke is, so adding it would let a developer make
their users' drawings look worse. Transparency matters more than format choice for the main use
cases, so that was built instead.

### No third party dependencies

The library depends only on Compose and Material. No image loader, no dependency injection, no
networking. A library that drags a dependency tree behind it is much harder to adopt, and every
dependency is a version conflict waiting for the consuming app.

### Two renderers, kept in step by hand

The screen draws with Compose `DrawScope`; the export draws with `android.graphics.Canvas`. They are
different APIs, so every brush, dash and shape exists twice.

*Considered instead:* rendering the screen from the same Android `Canvas` used for export.

That would have meant giving up Compose's draw phase and its invalidation. The cost is a real risk:
if the two drift, the exported PNG differs from what the user drew. Shared helpers, such as the dash
interval calculation, keep them aligned, and the risk is called out here so it stays visible.

## Edge cases

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
| A layout pass that leaves the canvas no height | Ignored, and not recorded as the canvas size, so the pass that restores a real size still has a size to remap from |
| Restoring saved state written by an older version | Starts fresh, rather than reading rows that have a different shape |
| A file name typed by a user | Sanitised, path separators stripped, `.png` appended |
| Exporting before the canvas has been laid out | Returns null rather than crashing |

## Known ceilings

Listed so they are choices rather than surprises.

| Ceiling | Why it is still there |
|---|---|
| Flood fill and export run on the calling thread | A background thread needs a progress and cancellation story to be worth it |
| No cap on canvas resolution | The right memory guard, and the one thing that would meaningfully reduce out of memory risk |
| Selection is text only | Hit testing strokes and shapes is a different problem: distance to a path rather than a bounding box |
| `backgroundImage` is stretched, not fitted | Sizing the canvas to the image sidesteps it, and a fit mode needs a scale policy |
| The background image is not restored after rotation | It is a user's photo, not something the library can recompute. Keep the `Uri` if you need it back |
