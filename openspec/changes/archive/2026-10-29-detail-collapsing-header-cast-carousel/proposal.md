# Proposal

## Why

On Movie Detail the backdrop, title and back button are the first item of the scrolling list, so they disappear as soon as the user scrolls and the user loses the title and the back control. The cast is a vertical list of up to ten rows with dividers, which makes the screen long and pushes everything else out of view. A pinned, collapsing header and a compact cast carousel fix both.

## What Changes

- The backdrop, title and back button collapse as the user scrolls and settle into a pinned strip at the top of the screen. The strip keeps a thin, darkened crop of the backdrop, the title and the back control. The rest of the screen scrolls behind it.
- The title shrinks and moves up beside the back button while the header collapses. Text and icons stay white throughout, so the status bar icons need no theme-dependent change.
- The cast section becomes a single horizontally scrolling row of tiles, each showing the member's initial avatar, name and character role. The first ten billed members are still shown, and the "Cast" heading is still omitted when there is no cast.
- The layout keeps working in landscape, with the same collapse behaviour.
- Backend(s) touched: none. This is a UI-only change; no new TMDB fields or endpoints are used.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `movie-detail`: "Detail shows the movie's core information" (the backdrop and title now collapse into a pinned header), "Back returns to the previous screen" (the back control stays reachable while scrolled), and "Detail lists up to ten cast members" (list becomes a horizontal carousel). A new requirement is added for the collapsing, pinned header.

## Impact

- `ui/screens/detail/MovieDetailScreen.kt`: header composition, scroll-driven collapse, cast section.
- Possibly a shared cast tile in `ui/components/`; `Sizes.AvatarLg` already exists in `ui/theme/Dimens.kt`.
- No change to `domain/`, `data/`, DI or navigation. `MovieDetailViewModel` and its tests are unaffected; the existing mapper cap of ten cast members is kept.
- No new dependencies.

## Out of scope

- Cast photos (TMDB `profile_path`); tiles keep initial avatars.
- Tagline display, director, trailer, certification and vote count (the "Batch 1" ideas), and anything else that needs new TMDB fields or `append_to_response` values.
- "More like this", "Where to watch" and other new sections.
- A solid-surface collapsed header; the strip keeps the backdrop.
- Changes to the Watchlist button, genre chips, synopsis or the loading and error states.

## Pre-production debt

none
