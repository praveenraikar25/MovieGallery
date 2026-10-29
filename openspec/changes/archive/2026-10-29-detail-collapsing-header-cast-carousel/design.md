# Design

## Context

`MovieDetailScreen.kt` renders a single `LazyColumn`. Its first item is the hero (backdrop, gradient, title, back button) at `Sizes.HeroHeight` (340dp) or `Sizes.HeroHeightLandscape` (220dp); its second item holds everything else, including the cast as a vertical list with dividers. Because the hero is a list item, it scrolls away with the content. The screen already forces white status bar icons via `LightStatusBarIcons(light = false)`. See proposal.md for motivation and scope.

UI only: no TMDB, BFF or Firebase calls change, and `domain/` and `data/` are untouched.

## Goals / Non-Goals

**Goals:**
- A header that collapses in step with scrolling and stays pinned, with content scrolling behind it.
- A compact, horizontally scrolling cast row.
- Smooth scrolling: the collapse must not trigger recomposition of the whole screen on every scroll frame.

**Non-Goals:**
- Cast photos, tagline, trailer, director, certification or any new data (see proposal "Out of scope").
- A solid-surface collapsed header.
- Reusing the collapse for other screens. It is built for this screen and extracted only if a second screen needs it.

## Decisions

**1. Keep one `LazyColumn`; draw the header above it in a `Box`.**
The list's first item becomes a spacer the height of the expanded header, so content starts below the image. The header is a sibling drawn after the list, so it sits on top and content scrolls behind it.
*Alternative: `LargeTopAppBar` with an exit-until-collapsed scroll behavior.* Rejected: it cannot host a backdrop image and gives little control over the title interpolation.
*Alternative: nested-scroll connection that shrinks the header and consumes scroll first.* Rejected: it moves the content with the header instead of letting it scroll behind, and needs more state.

**2. Derive the collapse fraction from the list's scroll position.**
The fraction is `scrollOffset / (expandedHeight - collapsedHeight)`, clamped to 0..1, where the offset is read from the list state: the first item's scroll offset while it is visible, and fully collapsed otherwise. It is read inside layout and draw phases (`layout`, `graphicsLayer`, or a lambda-based modifier) rather than in composition, so scrolling does not recompose the screen.
The mapping from scroll offset to fraction is a pure function, so it can be unit tested without Compose.

**3. Interpolate by one fraction.**
Header height goes from the expanded height to the collapsed height; the backdrop is center-cropped so it keeps its aspect as the header shrinks; the scrim alpha rises (roughly 0.3 to 0.6) so the white title stays legible on the thin strip; the title's text size and position interpolate from bottom-start to beside the back button. Collapsed height is the status bar inset plus a standard top app bar height.

**4. Status bar icons stay white.**
The collapsed strip still carries the darkened backdrop, so the existing `LightStatusBarIcons(light = false)` call stays valid and needs no scroll-driven change. This is the main reason for choosing the image strip over a solid surface.

**5. Cast as a `LazyRow` inside the `LazyColumn`.**
Nesting is fine because the two scroll in different directions. Each tile is a fixed-width column (about 88dp) with the initial avatar (`Sizes.AvatarLg`), the name (max 2 lines) and the role (max 2 lines, ellipsized), so tiles keep a uniform height. The row has horizontal content padding so the first and last tiles line up with the rest of the screen's gutter. The ten-member cap stays where it is (the mapper).
*Alternative: a `LazyVerticalGrid` as the root of the screen.* Rejected: it was only needed for a grid cast, and the carousel was chosen instead.

**6. Short content.**
The spacer plus content may be shorter than the viewport plus the collapse distance, so the header cannot fully collapse. The list gets bottom content padding sized to guarantee the full collapse distance is scrollable; otherwise the header would sit half-collapsed with nothing to scroll.

**7. Orientation.**
The expanded height keeps using the existing portrait and landscape sizes. The collapsed height is shared, so landscape has a shorter collapse distance.

## Risks / Trade-offs

- [Per-frame recomposition if the fraction is read in composition] → Read it in layout/draw (decision 2) and verify by scrolling on a device.
- [Bright backdrops reduce title contrast on the strip] → Scrim alpha rises with the fraction; check with a light backdrop.
- [Extra bottom padding adds empty space at the end of the page] → Only as much as needed to reach full collapse.
- [Back button at the top may collide with the status bar] → It keeps the existing status-bar inset padding in both states.
- [No UI tests in the project] → Cover the fraction mapping with JVM unit tests and check the animation manually on a device in both orientations.
