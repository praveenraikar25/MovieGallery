# Proposal

## Why

The app still ships Android's default launcher icon (green background, stock robot foreground), which gives it no identity on the home screen or in the app drawer. A branded icon that reuses the app's terracotta/cream palette makes MovieGallery recognisable and ready for production.

## What Changes

- Replace the default launcher icon with concept "B, Grid": a terracotta tile holding a 2x2 grid of rounded poster tiles, the bottom-right one dark with a play mark.
- Provide the icon as an adaptive icon (background layer + foreground layer) plus a single-colour monochrome layer for Android 13+ themed icons.
- Remove the legacy default `.webp` launcher assets so the old icon cannot resurface on any API level.
- Backends touched: none.

## Capabilities

### New Capabilities
- `app-branding`: how the app presents itself outside its own screens, starting with the launcher icon (home screen, app drawer, themed icons).

### Modified Capabilities
<!-- None: no existing capability's requirements change. -->

## Impact

- Resources only: launcher icon drawables and mipmaps under `app/src/main/res/` (`drawable/ic_launcher_background.xml`, `drawable/ic_launcher_foreground.xml`, a new monochrome drawable, `mipmap-anydpi/ic_launcher*.xml`, legacy `mipmap-*/ic_launcher*.webp`). `AndroidManifest.xml` already references `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`, so it is unchanged.
- No Kotlin code, dependencies, or backend changes.

## Out of scope

- Splash screen / in-app logo and wordmark.
- Play Store listing assets (512x512 icon, feature graphic).
- Changing the app name or in-app theme colours.
- Alternative or user-selectable icons.

## Pre-production debt

None.
