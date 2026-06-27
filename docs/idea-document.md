# Project Idea Document — EZCanvas

**Course:** Advanced Seminar in Mobile Development
**Type:** Reusable Android library for developers
**Name:** EZCanvas
**Built with:** Jetpack Compose, single Activity (no XML layouts)

## One-sentence description

A fully customizable, exportable drawing-canvas library for Android, with a configurable built-in toolbar the developer composes to fit their own use case. The library and demo are built entirely in Jetpack Compose.

## The problem

Apps constantly need a drawing surface — signature capture, photo/screenshot markup, freehand notes, sketching, whiteboards, image annotation. Today developers re-implement the same hard parts every time: smooth touch handling, undo/redo, stroke rendering, bitmap export, and state restoration across rotation. Existing libraries are often outdated, force their own fixed UI on you, or expose a closed feature set you can't trim down.

## The solution

A single `EzCanvas` composable that handles the hard drawing internals, plus a separate, **configurable `EzToolbar`** composable; both share one hoisted state holder via `rememberEzCanvasState()` (the standard Compose pattern). The developer decides exactly which controls the toolbar exposes (color, width, opacity, tool selector, undo/redo, clear, background, export), so the same library serves a minimal signature pad and a full sketch app equally well. Drawings export to bitmap/PNG/JPEG/WebP and serialize to JSON for save/restore.

## Target developers

Android developers who need an embeddable drawing/annotation surface: signature capture, document/photo markup, note-taking, education/whiteboard, and design tools.

## Key differentiator

Most drawing libraries impose their own toolbar UI. EZCanvas separates the **canvas engine** from a **composable toolbar**, letting developers enable/disable features and build the exact UI they need. Configurability is the product.

## Committed features (v1 = Phase 1 + Phase 2)

- Smooth freehand pen with bézier smoothing; configurable color, width, opacity, line cap/join, anti-alias
- Eraser (pixel and whole-stroke modes)
- Undo / redo / clear with capped history
- Brushes: pen, marker/highlighter, neon/glow, calligraphy
- Velocity- and pressure-based stroke width (stylus)
- Eyedropper (pick color from canvas)
- Background: solid color or image to draw on top of; transparent-background export
- Export to Bitmap / PNG / JPEG / WebP with quality and resolution scaling
- JSON serialize / restore; rotation & state restoration
- Configurable `InkToolbar` (composable control set, orientation, position, custom action slot)
- Dark mode, RTL, content descriptions, toggleable TalkBack action announcements
- Input modes (finger / stylus / both), gesture-conflict handling, off-main-thread export, OOM-safe canvas sizing

## Stretch / future work (documented, not promised)

Shapes (line, rectangle, oval, arrow) and text; zoom/pan; flood fill; palm rejection; layers; SVG export. These are narrated as future work in the final video if not completed.

## Why this is an advanced Android project

Jetpack Compose `Canvas` rendering, `pointerInput` touch handling with smoothing, a serializable element model, `rememberSaveable` state restoration, bitmap memory management and off-main-thread export, single-Activity navigation, dark mode, RTL, accessibility, and a clean multi-module library + demo separation published for reuse.

## Why a library (not app code)

The drawing engine, undo/redo, export, and toolbar are generic and reusable across many apps; packaging them as a versioned library avoids copy-paste and is exactly the "library for developers" deliverable the course targets.

## Architecture (sketch)

- **Module `library/`** — the `EzCanvas` composable, element model, brush renderers, history, export, and the `EzToolbar` composable. No app-specific code.
- **Module `demo/`** — a two-screen app: a Playground (full canvas + scrollable settings exposing every option) and a Gallery of one-tap preset configurations (signature pad, photo markup, neon art, whiteboard, kids doodle, calligraphy).
- **Element model** — each stroke/shape is a serializable object in an ordered list; one model powers undo/redo, serialization, export, and (later) layers/SVG.

## Demo app concept

The demo is a real two-screen app, not a minimal sample — its job is to showcase everything the library can produce for a developer and deliver to an end user. All controls only surface features already committed in Phase 1 + 2, so the demo adds UI plumbing, not new library scope.

**Screen 1 — Playground (fully customizable canvas).** The `EzCanvas` fills the screen with the full `EzToolbar` plus a scrollable settings panel exposing every option live: brush selector (pen, marker, neon, calligraphy) with velocity/pressure toggles; color swatches and a full color picker; stroke-width and opacity sliders; eraser (mode + size); eyedropper; background color/image/transparent; undo / redo / clear; export format (PNG/JPEG/WebP) with quality and scale plus save/share; and live toggles for dark mod