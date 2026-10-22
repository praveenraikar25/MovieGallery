# Design

## Context

The icon is currently Android Studio's default: `drawable/ic_launcher_background.xml` (green with grid lines), `drawable/ic_launcher_foreground.xml` (robot), and `mipmap-anydpi/ic_launcher.xml` / `ic_launcher_round.xml` (adaptive, reusing the foreground as monochrome). Legacy `mipmap-*dpi/*.webp` files also exist. The manifest already points at `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`. Palette comes from `ui/theme/Color.kt`: accent `#C96442` / `#E08A65`, cream `#FAF9F6`, ink `#1B1A17`.

## Goals / Non-Goals

**Goals:**
- Concept B rendered as vector drawables, crisp at every density and mask.
- Themed-icon support.

**Non-Goals:**
- Play Store assets, splash screen, in-app logo.

## Decisions

- **Vector adaptive icon, not raster.** Background and foreground are VectorDrawables on the 108x108 viewport; no PNG/WebP generation. Alternative: export bitmaps per density; rejected as heavier and blurrier. `minSdk` is 29, so adaptive icons are always available.
- **Background = flat `#C96442`.** The grid is the foreground. Keeping the background a plain fill keeps masks safe.
- **Foreground sized inside the 66dp safe zone.** The 2x2 grid (four rounded tiles, ~22dp each, ~4dp gap) is centred within the central 66x66 area so circle masks never clip it. Tile colours: `#FAF9F6`, `#F3D9CD`, `#F3D9CD`, and `#1B1A17` with a rounded `#E08A65` play triangle.
- **Separate monochrome drawable.** A dedicated `ic_launcher_monochrome.xml` draws the four tiles as solid shapes with the play mark cut out, because reusing the coloured foreground would tint all fills identically and lose the play mark.
- **Colours inline in the drawables**, not in `Color.kt`: launcher resources cannot depend on Compose tokens. Hex values are mirrored from the theme.
- **Delete legacy webp files**, since `minSdk` is 29 and adaptive icons cover every supported API level.

## Risks / Trade-offs

- [Grid clipped by a launcher mask] -> keep all artwork inside the 66dp safe zone; verify with Android Studio's Image Asset preview shapes.
- [Monochrome variant unreadable] -> use a cut-out play mark and verify with themed icons on an Android 13+ emulator.
