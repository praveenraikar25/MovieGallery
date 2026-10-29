# Design

## Context

`LoadingState` in `ui/components/StateViews.kt` is a centred `CircularProgressIndicator` in the primary colour. Home, Movie Detail and Top TV Shows all call it, so replacing its content covers all three. Other spinners (login, load-more, chat bubble) are separate inline uses and stay as they are. The tab bar animation already keeps its timing maths in a pure object (`TabMotion`) so it can be unit tested on the JVM; this follows the same approach. See proposal.md for motivation and scope.

## Goals / Non-Goals

**Goals:**
- One reusable reel composable, drawn with Compose `Canvas`, that `LoadingState` uses.
- Geometry and motion rules testable without Android or Compose.

**Non-Goals:**
- A general loading-indicator framework or a user setting to pick a variant.
- Replacing the inline spinners.

## Decisions

- **Draw with `Canvas`, not an icon or Lottie.** The shape is a ring, six circles and a hub, so there is no asset to keep in sync and no new dependency. Alternative: Lottie. Rejected: adds a library for one shape.
- **Rotate with an infinite transition using a linear easing**, 1600 ms per turn, applied as a rotation of the whole drawing. Linear gives the constant speed the spec requires; the default easing would visibly slow at turn ends.
- **Pure helper for geometry.** A small object computes the six hole centre angles and radii as fractions of the reel size (no `android.*` or Compose imports), so tests check evenly spaced holes and holes inside the ring.
- **Colour from `MaterialTheme.colorScheme.primary`**, matching what the current spinner uses, so both themes work without new tokens.
- **Reduced motion:** read the system animator duration scale. When it is 0, skip the transition and draw the reel at rest. Compose has no built-in flag for this, so the scale is read from system settings in one place and exposed to the composable. Alternative: ignore it. Rejected: the spec requires it and it is an accessibility basic.
- **Accessibility:** `semantics { contentDescription = "Loading" }` on the reel itself so it is announced once.
- **Size:** about 64 dp, a new named size in the theme's `Sizes` rather than a literal.

## Risks / Trade-offs

- [Animator scale read from settings does not update while the screen is open] → Read it when the composable enters composition; a change takes effect the next time a loading state appears. Acceptable for a setting changed outside the app.
- [Continuous animation costs a little battery on a long load] → Loads are short and the animation stops as soon as the composable leaves composition.
- [Rotating a six-hole shape can look like it stutters at 1.6 s per turn because of its symmetry] → The hub and one slightly larger hole can break symmetry if it looks wrong on a device; tune during implementation.

## Open Questions

- Final size (64 dp assumed) and whether one hole should be marked to make the rotation easier to see. Both are visual tuning and do not change the specs or tasks.
