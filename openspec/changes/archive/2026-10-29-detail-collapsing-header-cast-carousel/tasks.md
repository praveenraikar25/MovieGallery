# Tasks

## 1. Collapse math

- [x] 1.1 Add a pure function in `ui/screens/detail/` that maps the list's first-item scroll offset, the expanded header height and the collapsed header height to a collapse fraction clamped to 0..1, treating "first item scrolled out of view" as fully collapsed. Verify with a JVM unit test in `app/src/test/.../ui/screens/detail/` covering: offset 0 gives 0 (scrolling back to the top), a mid offset gives the proportional fraction (scrolling down), an offset at or past the collapse distance gives 1 (fully collapsed), and the landscape height gives a shorter collapse distance than portrait. Run `./gradlew :app:testDebugUnitTest --tests "com.raikar.moviegallery.ui.screens.detail.*"` and see it pass.
- [x] 1.2 Add a pure function that returns the extra bottom content padding needed so the list can always scroll the full collapse distance, given the viewport height, content height and collapse distance. Verify with a JVM unit test: content already taller than viewport plus collapse distance gives 0, and short content gives exactly the shortfall (the "Short content" scenario).

## 2. Pinned collapsing header

- [x] 2.1 In `MovieDetailScreen.kt`, make the list's first item a spacer of the expanded header height and draw the header in a `Box` above the list, so content scrolls behind it. Verify on a device or emulator: scrolling the Detail screen moves content under the header, which stays at the top.
- [x] 2.2 Drive the header height, backdrop crop (center-cropped, with the generated gradient when there is no backdrop) and scrim alpha (about 0.3 to 0.6) from the fraction in 1.1, reading it in layout/draw rather than composition. Verify manually that the backdrop shrinks progressively, and that scrolling stays smooth with no whole-screen recomposition (check with Layout Inspector recomposition counts).
- [x] 2.3 Interpolate the title (smaller text, moving from bottom-start of the backdrop to beside the back control) and keep the back control and status-bar inset padding in both states. Verify manually that the title is legible on a light backdrop, and that tapping back while collapsed returns to the previous tab.
- [x] 2.4 Apply the bottom padding from 1.2 and confirm the white status bar icons (`LightStatusBarIcons(light = false)`) need no change. Verify manually with a movie with a very short synopsis and no cast that the header can still collapse fully, and that the status bar icons stay readable throughout.
- [x] 2.5 Verify portrait and landscape on a device: the header starts at its portrait or landscape height, collapses and pins in the same way, and expands again when scrolling back to the top.

## 3. Cast carousel

- [x] 3.1 Add a cast tile composable (in `ui/components/` if it is reusable, otherwise private to the detail screen) with the initial avatar at `Sizes.AvatarLg`, the name (max 2 lines, ellipsis) and the role (max 2 lines, ellipsis), at a fixed width so all tiles share one height. Verify with a Compose `@Preview` showing a short and a very long name and role, and visually confirm equal tile heights.
- [x] 3.2 Replace the vertical cast list and dividers with a `LazyRow` of tiles under the "Cast" heading, with horizontal content padding matching the screen gutter and spacing between tiles, still omitting the heading when there is no cast. Verify manually: a movie with a large cast shows ten tiles and scrolls sideways while the rest of the screen stays still, and a movie with no cast shows no "Cast" heading.
- [x] 3.3 Confirm the existing ten-member cap and billing order still hold. Verify by running the existing mapper test (`./gradlew :app:testDebugUnitTest`) and seeing it pass unchanged (the "Large cast" scenario is covered there).

## 4. Integration

- [x] 4.1 Run `./gradlew ktlintCheck` and `./gradlew test` and verify both pass.
- [x] 4.2 Walk through every scenario in `specs/movie-detail/spec.md` on a device in portrait and landscape, in light and dark theme, and tick each off. Note: scenarios that are purely visual (title interpolation, ellipsis, landscape, sideways cast scrolling) have no JVM test, because the project has no instrumented or Compose UI tests; the collapse and padding math is covered in 1.1 and 1.2.

## Workflow follow-up

- Update `openspec/specs/movie-detail/spec.md` via archive once the change is implemented and reviewed.
- Open a separate change for Batch 1 (tagline, director, trailer, certification, vote count, cast photos).
