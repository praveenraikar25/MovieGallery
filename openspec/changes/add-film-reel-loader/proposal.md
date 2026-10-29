# Proposal

## Why

Every full-screen load in the app shows the stock Material circular spinner, which says nothing about MovieGallery. A film reel spinner gives the app a recognisable, on-brand loading moment while keeping the same behaviour (shown until data arrives, then replaced).

## What Changes

- Replace the plain circular spinner on the shared full-screen loading state with a film reel spinner: a ring with six evenly spaced holes around a central hub, rotating continuously.
- The reel takes its colour from the app theme, so it reads correctly in light and dark themes.
- The reel is announced to screen readers as "Loading".
- When the system has animations turned off, the reel is shown static instead of rotating.
- Screens affected (they already share one loading state): Home, Movie Detail and Top TV Shows.

## Capabilities

### New Capabilities
- `loading-indicator`: the app's shared full-screen loading indicator (appearance, motion, theming and accessibility), so individual screens only say *when* it is shown.

### Modified Capabilities
<!-- None. Screen specs (movie-browse, movie-detail, tv-shows) say "a loading indicator is shown" and stay true; they do not change. -->

## Impact

- Backends touched: none.
- Code: the shared loading state in `ui/components/` gains the reel; Home, Movie Detail and Top TV Shows pick it up without changes of their own.
- Dependencies: none new (plain Compose drawing and animation).
- Tests: JVM unit tests for the reel's geometry and motion rules; no instrumented tests.

## Out of scope

- Search: it shows no spinner today, only the "Searching…" header text. Adding a spinner there is a behaviour change and is not included.
- The login screen's request spinner, the load-more spinner at the end of the TV shows grid, and the small spinner in the AI chat reply bubble. They are inline or small, and the reel does not suit them.
- Skeleton placeholders, clapperboard, popcorn and projector-sweep variants.
- Changing when or how long any loading state is shown.

## Pre-production debt

None.
